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

package androidx.compose.ui.node

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Density

/**
 * The marker interface to be implemented by the root backing the composition.
 * To be used in tests.
 */
interface RootForTest {
    /**
     * Current device density.
     */
    val density: Density

    /**
     * Semantics owner for this root. Manages all the semantics nodes.
     */
    val semanticsOwner: SemanticsOwner

    /**
     * The service handling text input.
     */
    val textInputService: TextInputService

    /**
     * Send this [KeyEvent] to the focused component in this [Owner].
     *
     * @return true if the event was consumed. False otherwise.
     */
    fun sendKeyEvent(keyEvent: KeyEvent): Boolean
}