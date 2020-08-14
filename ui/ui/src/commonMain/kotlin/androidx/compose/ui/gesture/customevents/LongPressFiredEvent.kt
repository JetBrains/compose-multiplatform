/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.gesture.customevents

import androidx.compose.ui.input.pointer.CustomEvent
import androidx.compose.ui.input.pointer.PointerInputFilter

/**
 * Dispatched to indicate that a [PointerInputFilter] that responds to a pointer touching a
 * region of the UI for a period of time has fired it's associated callback such that any other
 * [PointerInputFilter] that may be waiting to fire, should no longer do so.
 */
object LongPressFiredEvent : CustomEvent