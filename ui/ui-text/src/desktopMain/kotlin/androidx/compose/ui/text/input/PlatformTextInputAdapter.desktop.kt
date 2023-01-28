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

package androidx.compose.ui.text.input

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi

// TODO(b/267235947) Flesh this out, document it, and wire it up when ready to integrate new text
//  field with desktop.
@ExperimentalTextApi
@Immutable
actual interface PlatformTextInputPlugin<T : PlatformTextInputAdapter>

// TODO(b/267235947) Flesh this out, document it, and wire it up when ready to integrate new text
//  field with desktop.
@ExperimentalTextApi
actual interface PlatformTextInputAdapter

@OptIn(ExperimentalTextApi::class)
internal actual fun PlatformTextInputAdapter.dispose() {
    // TODO(b/267235947)
}