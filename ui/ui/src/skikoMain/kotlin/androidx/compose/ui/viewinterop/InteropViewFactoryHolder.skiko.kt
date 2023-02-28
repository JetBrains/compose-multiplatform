/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.ComposeNodeLifecycleCallback

// Skiko doesn't have an intrinsic interop view type, so satisfy the expect declaration with a
// no-op and unusable type.
internal actual class InteropViewFactoryHolder private constructor(
    // No instances allowed.
) : ComposeNodeLifecycleCallback {
    init {
        throwUnsupportedError()
    }

    actual fun getInteropView(): InteropView? = throwUnsupportedError()

    override fun onReuse() {
        throwUnsupportedError()
    }

    override fun onDeactivate() {
        throwUnsupportedError()
    }

    override fun onRelease() {
        throwUnsupportedError()
    }

    private fun throwUnsupportedError(): Nothing = throw UnsupportedOperationException(
        "InteropViewFactoryHolder cannot be used because " +
            "interoperability views are not supported on this platform."
    )
}