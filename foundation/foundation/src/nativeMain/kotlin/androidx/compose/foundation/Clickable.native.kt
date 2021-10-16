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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.KeyEvent

@Composable
internal actual fun isComposeRootInScrollableContainer(): () -> Boolean = { false }

// TODO: b/168524931 - should this depend on the input device?
internal actual val TapIndicationDelay: Long = 0L

/**
 * Whether the specified [KeyEvent] represents a user intent to perform a click.
 * (eg. When you press Enter on a focused button, it should perform a click).
 */
internal actual val KeyEvent.isClick: Boolean
    get() = TODO("implement native isClick")
