// ktlint-disable filename
/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.animation.core

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import androidx.compose.ui.util.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.CountDownLatch

/** @suppress */
@InternalAnimationApi
var rootAnimationClockFactory: (CoroutineScope) -> AnimationClockObservable = {
    DefaultAnimationClock()
}
    @VisibleForTesting
    set

/**
 * Default Choreographer based clock that pushes a new frame to all subscribers on each
 * Choreographer tick, until all subscribers have unsubscribed. An instance of this clock will be
 * provided through [AnimationClockAmbient][androidx.compose.ui.platform.AnimationClockAmbient] at
 * the root of the composition tree.
 *
 * If initialized from any other thread but the main thread, part of the initialization is done
 * synchronously on the main thread. If this poses a problem, consider initializing this clock on
 * the main thread itself.
 */
class DefaultAnimationClock : BaseAnimationClock() {

    private val mainChoreographer: Choreographer

    init {
        /**
         * If not initializing on the main thread, a message will be posted on the main thread to
         * fetch the Choreographer, and initialization blocks until that fetch is completed.
         */
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mainChoreographer = Choreographer.getInstance()
        } else {
            val latch = CountDownLatch(1)
            var choreographer: Choreographer? = null
            Handler(Looper.getMainLooper()).postAtFrontOfQueue {
                try {
                    choreographer = Choreographer.getInstance()
                } finally {
                    latch.countDown()
                }
            }
            latch.await()
            mainChoreographer = choreographer!!
        }
    }

    @Volatile
    private var subscribedToChoreographer = false

    private val frameCallback = Choreographer.FrameCallback {
        dispatchTime(it / 1000000)
    }

    override fun subscribe(observer: AnimationClockObserver) {
        if (!subscribedToChoreographer) {
            mainChoreographer.postFrameCallback(frameCallback)
            subscribedToChoreographer = true
        }
        super.subscribe(observer)
    }

    override fun dispatchTime(frameTimeMillis: Long) {
        super.dispatchTime(frameTimeMillis)
        subscribedToChoreographer = if (hasObservers()) {
            mainChoreographer.postFrameCallback(frameCallback)
            true
        } else {
            false
        }
    }
}