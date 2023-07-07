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

package androidx.compose.material3

import androidx.compose.ui.input.key.KeyEvent

internal expect val KeyEvent.isDirectionUp: Boolean
internal expect val KeyEvent.isDirectionDown: Boolean
internal expect val KeyEvent.isDirectionRight: Boolean
internal expect val KeyEvent.isDirectionLeft: Boolean
internal expect val KeyEvent.isHome: Boolean
internal expect val KeyEvent.isMoveEnd: Boolean
internal expect val KeyEvent.isPgUp: Boolean
internal expect val KeyEvent.isPgDn: Boolean
internal expect val KeyEvent.isEsc: Boolean
