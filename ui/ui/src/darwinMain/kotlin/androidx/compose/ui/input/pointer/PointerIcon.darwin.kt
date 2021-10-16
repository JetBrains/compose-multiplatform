/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

// uikit doesn't seem to have NSCursor.
// TODO: consider having it for macos though.
object DummyPointerIcon : PointerIcon

internal actual val pointerIconDefault: PointerIcon = DummyPointerIcon
internal actual val pointerIconCrosshair: PointerIcon = DummyPointerIcon
internal actual val pointerIconText: PointerIcon = DummyPointerIcon
internal actual val pointerIconHand: PointerIcon = DummyPointerIcon