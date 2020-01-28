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

// TODO(b/137794549): Remove View System-related expect/actuals
actual typealias ViewParent = android.view.ViewParent

// TODO(b/137794549): Remove View System-related expect/actuals
actual typealias View = android.view.View

// TODO(b/137794549): Remove View System-related expect/actuals
actual val View.parent: ViewParent
    get() = parent

// TODO(b/137794549): Remove View System-related expect/actuals
actual val View.context: Context
    get() = context

// TODO(b/137794549): Remove View System-related expect/actuals
actual typealias ViewGroup = android.view.ViewGroup

// TODO(b/137794549): Remove View System-related expect/actuals
actual typealias Context = android.content.Context

// TODO(b/137794549): Remove View System-related expect/actuals
actual typealias FrameLayout = android.widget.FrameLayout

// TODO(b/137794558): Create portable abstraction for scheduling
actual typealias Looper = android.os.Looper

// TODO(b/137794558): Create portable abstraction for scheduling
actual object LooperWrapper {
    actual fun getMainLooper(): Looper = android.os.Looper.getMainLooper()
}

actual fun isMainThread(): Boolean {
    return android.os.Looper.myLooper() == android.os.Looper.getMainLooper()
}

// TODO(b/137794558): Create portable abstraction for scheduling
actual class Handler {
    val handler: android.os.Handler

    actual constructor(looper: Looper) {
        handler = android.os.Handler(looper)
    }
    actual fun postAtFrontOfQueue(block: () -> Unit): Boolean {
        return handler.postAtFrontOfQueue(block)
    }
}

// TODO(b/137794558): Create portable abstraction for scheduling
actual typealias ChoreographerFrameCallback = android.view.Choreographer.FrameCallback

actual object Choreographer {
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

actual object Trace {
    actual fun beginSection(name: String) = android.os.Trace.beginSection(name)
    actual fun endSection() = android.os.Trace.endSection()
}
