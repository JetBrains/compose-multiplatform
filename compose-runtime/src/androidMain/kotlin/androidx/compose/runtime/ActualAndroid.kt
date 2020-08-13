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

package androidx.compose.runtime

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.dispatch.AndroidUiDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class AndroidEmbeddingContext : EmbeddingContext {
    private val handler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    override fun mainThreadCompositionContext(): CoroutineContext {
        return MainAndroidUiContext
    }

    override fun postOnMainThread(block: () -> Unit) {
        handler.post(block)
    }

    override fun postFrameCallback(callback: ChoreographerFrameCallback) {
        android.view.Choreographer.getInstance().postFrameCallback(callback)
    }

    override fun cancelFrameCallback(callback: ChoreographerFrameCallback) {
        android.view.Choreographer.getInstance().removeFrameCallback(callback)
    }
}

actual fun EmbeddingContext(): EmbeddingContext = AndroidEmbeddingContext()

// TODO(b/137794558): Create portable abstraction for scheduling
internal actual typealias ChoreographerFrameCallback = android.view.Choreographer.FrameCallback

// TODO: Our host-side tests still grab the Android actuals based on SDK stubs that return null.
// Satisfy their dependencies.
private val MainAndroidUiContext: CoroutineContext by lazy {
    if (Looper.getMainLooper() != null) AndroidUiDispatcher.Main
    else Dispatchers.Main
}

internal actual object Trace {
    actual fun beginSection(name: String) = android.os.Trace.beginSection(name)
    actual fun endSection() = android.os.Trace.endSection()
}

internal actual typealias MainThread = androidx.annotation.MainThread

internal actual typealias CheckResult = androidx.annotation.CheckResult
