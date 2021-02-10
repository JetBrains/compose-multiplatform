/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.yield
import kotlin.coroutines.CoroutineContext

/**
 * Dispatch events on the next EDT tick if they are blocking EDT too long,
 * so EDT can handle more important actions (like render a new frame).
 *
 * (EDT - AWT event dispatch thread)
 *
 * [maxNanosToBlockThread] defines how long events can block EDT.
 *
 * It is needed in a case when we have a lot of heavy events (like mouse scroll)
 * in a short period of time.
 *
 * Without dispatching events we may have a situation
 * when 30 events of scroll block AWT Thread for 1 second, without rerendering content.
 */
internal class AWTDebounceEventQueue constructor(
    // 4 ms is enough for the user not to see the lags
    private val maxNanosToBlockThread: Long = 4_000_000, // 4 milliseconds
    private val nanoTime: () -> Long = System::nanoTime,
    context: CoroutineContext = Dispatchers.Swing
) {
    private val queue = Channel<() -> Unit>(Channel.UNLIMITED)

    private var job = GlobalScope.launch(context) {
        var lastTime = nanoTime()
        for (event in queue) {
            val time = nanoTime()
            if (time - lastTime >= maxNanosToBlockThread) {
                lastTime = time
                yield()
            }
            event()
        }
    }

    fun cancel() {
        job.cancel()
    }

    fun post(event: () -> Unit) {
        queue.offer(event)
    }
}