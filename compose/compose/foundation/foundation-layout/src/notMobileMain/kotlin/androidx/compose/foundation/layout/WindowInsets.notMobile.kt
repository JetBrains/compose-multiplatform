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

package androidx.compose.foundation.layout

private val ZeroInsets = WindowInsets(0, 0, 0, 0)

actual val WindowInsets.Companion.captionBar: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.displayCutout: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.ime: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.navigationBars: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.statusBars: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.systemBars: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.systemGestures: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.tappableElement: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.waterfall: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.safeDrawing: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.safeGestures: WindowInsets
    get() = ZeroInsets

actual val WindowInsets.Companion.safeContent: WindowInsets
    get() = ZeroInsets
