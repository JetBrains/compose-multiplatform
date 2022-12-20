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
import androidx.compose.ui.input.key.key
import org.jetbrains.skiko.SkikoKey

private val DIRECTION_UP_KEY_CODE = SkikoKey.KEY_UP.platformKeyCode.toLong()
private val DIRECTION_DOWN_KEY_CODE = SkikoKey.KEY_DOWN.platformKeyCode.toLong()
private val DIRECTION_LEFT_KEY_CODE = SkikoKey.KEY_LEFT.platformKeyCode.toLong()
private val DIRECTION_RIGHT_KEY_CODE = SkikoKey.KEY_RIGHT.platformKeyCode.toLong()
private val HOME_KEY_CODE = SkikoKey.KEY_HOME.platformKeyCode.toLong()
private val END_KEY_CODE = SkikoKey.KEY_END.platformKeyCode.toLong()
private val PG_UP_KEY_CODE = SkikoKey.KEY_PGUP.platformKeyCode.toLong()
private val PG_DN_KEY_CODE = SkikoKey.KEY_PGDOWN.platformKeyCode.toLong()
private val ESC_KEY_CODE = SkikoKey.KEY_ESCAPE.platformKeyCode.toLong()

internal actual val KeyEvent.isDirectionUp: Boolean
    get() = key.keyCode == DIRECTION_UP_KEY_CODE

internal actual val KeyEvent.isDirectionDown: Boolean
    get() = key.keyCode == DIRECTION_DOWN_KEY_CODE

internal actual val KeyEvent.isDirectionRight: Boolean
    get() = key.keyCode == DIRECTION_RIGHT_KEY_CODE

internal actual val KeyEvent.isDirectionLeft: Boolean
    get() = key.keyCode == DIRECTION_LEFT_KEY_CODE

internal actual val KeyEvent.isHome: Boolean
    get() = key.keyCode == HOME_KEY_CODE

internal actual val KeyEvent.isMoveEnd: Boolean
    get() = key.keyCode == END_KEY_CODE

internal actual val KeyEvent.isPgUp: Boolean
    get() = key.keyCode == PG_UP_KEY_CODE

internal actual val KeyEvent.isPgDn: Boolean
    get() = key.keyCode == PG_DN_KEY_CODE

internal actual val KeyEvent.isEsc: Boolean
    get() = key.keyCode == ESC_KEY_CODE
