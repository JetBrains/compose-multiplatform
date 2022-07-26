/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key

internal actual val platformDefaultKeyMapping: KeyMapping = defaultKeyMapping

@OptIn(ExperimentalComposeUiApi::class)
internal actual object MappedKeys {
    actual val A: Key = Key(Key.A.keyCode)
    actual val C: Key = Key(Key.C.keyCode)
    actual val H: Key = Key(Key.H.keyCode)
    actual val V: Key = Key(Key.V.keyCode)
    actual val X: Key = Key(Key.X.keyCode)
    actual val Z: Key = Key(Key.Z.keyCode)
    actual val Backslash: Key = Key(Key.Backslash.keyCode)
    actual val DirectionLeft: Key = Key(Key.DirectionLeft.keyCode)
    actual val DirectionRight: Key = Key(Key.DirectionRight.keyCode)
    actual val DirectionUp: Key = Key(Key.DirectionUp.keyCode)
    actual val DirectionDown: Key = Key(Key.DirectionDown.keyCode)
    actual val PageUp: Key = Key(Key.PageUp.keyCode)
    actual val PageDown: Key = Key(Key.PageDown.keyCode)
    actual val MoveHome: Key = Key(Key.MoveHome.keyCode)
    actual val MoveEnd: Key = Key(Key.MoveEnd.keyCode)
    actual val Insert: Key = Key(Key.Insert.keyCode)
    actual val Enter: Key = Key(Key.Enter.keyCode)
    actual val Backspace: Key = Key(Key.Backspace.keyCode)
    actual val Delete: Key = Key(Key.Delete.keyCode)
    actual val Paste: Key = Key(Key.Paste.keyCode)
    actual val Cut: Key = Key(Key.Cut.keyCode)
    actual val Tab: Key = Key(Key.Tab.keyCode)
    val Copy: Key = Key(Key.Copy.keyCode)
}
