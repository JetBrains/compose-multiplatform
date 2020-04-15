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

package androidx.compose

import androidx.core.os.HandlerCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual typealias EmbeddingUIContext = android.content.Context

// TODO(b/137794558): Create portable abstraction for scheduling
internal actual typealias Looper = android.os.Looper

// TODO(b/137794558): Create portable abstraction for scheduling
internal actual object LooperWrapper {
    actual fun getMainLooper(): Looper = android.os.Looper.getMainLooper()
}

internal actual fun isMainThread(): Boolean {
    return android.os.Looper.myLooper() == android.os.Looper.getMainLooper()
}

// TODO(b/137794558): Create portable abstraction for scheduling
internal actual class Handler {
    val handler: android.os.Handler

    actual constructor(looper: Looper) {
        handler = HandlerCompat.createAsync(looper)
    }
    actual fun post(block: () -> Unit): Boolean {
        return handler.post(block)
    }
}

// TODO(b/137794558): Create portable abstraction for scheduling
internal actual typealias ChoreographerFrameCallback = android.view.Choreographer.FrameCallback

internal actual object Choreographer {
    actual fun postFrameCallback(callback: ChoreographerFrameCallback) {
        android.view.Choreographer.getInstance().postFrameCallback(callback)
    }
    actual fun postFrameCallbackDelayed(delayMillis: Long, callback: ChoreographerFrameCallback) {
        android.view.Choreographer.getInstance().postFrameCallbackDelayed(callback, delayMillis)
    }
    actual fun removeFrameCallback(callback: ChoreographerFrameCallback) {
        android.view.Choreographer.getInstance().removeFrameCallback(callback)
    }
}

// TODO: Our host-side tests still grab the Android actuals based on SDK stubs that return null.
// Satisfy their dependencies.
private val MainAndroidUiDispatcher by lazy {
    if (Looper.getMainLooper() != null) AndroidUiDispatcher.Main
    else Dispatchers.Main
}

private object MainDispatcherCompositionFrameClock : CompositionFrameClock {
    override suspend fun <R> awaitFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
        withContext(Dispatchers.Main) {
            onFrame(System.nanoTime())
        }
}

private val MainAndroidCompositionFrameClock by lazy {
    if (Looper.getMainLooper() != null) AndroidUiDispatcher.Main.compositionFrameClock
    else MainDispatcherCompositionFrameClock
}

internal actual fun mainThreadCompositionDispatcher(): CoroutineDispatcher =
    MainAndroidUiDispatcher

internal actual fun mainThreadCompositionFrameClock(): CompositionFrameClock =
    MainAndroidCompositionFrameClock

internal actual object Trace {
    actual fun beginSection(name: String) = android.os.Trace.beginSection(name)
    actual fun endSection() = android.os.Trace.endSection()
}

internal actual typealias MainThread = androidx.annotation.MainThread

internal actual typealias CheckResult = androidx.annotation.CheckResult
