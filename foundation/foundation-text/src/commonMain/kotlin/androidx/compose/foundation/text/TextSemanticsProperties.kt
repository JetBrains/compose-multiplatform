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

package androidx.compose.foundation.text

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Semantics properties that apply to the text / text input elements.
 */
object TextSemanticsProperties {
    /**
     * Contains the IME action provided by the node.
     *
     *  @see SemanticsPropertyReceiver.imeAction
     */
    val ImeAction = SemanticsPropertyKey<ImeAction>("ImeAction")

    /**
     * Whether the node supports input methods.
     *
     * Supporting input methods means that the node provides a connection to IME (keyboard) and
     * is able to accept input from it. This is typically a text field for instance.
     *
     *  @see SemanticsPropertyReceiver.supportsInputMethods
     */
    val SupportsInputMethods = SemanticsPropertyKey<Unit>("SupportsInputMethods")
}

/**
 * Contains the IME action provided by the node.
 *
 *  @see TextSemanticsProperties.ImeAction
 */
var SemanticsPropertyReceiver.imeAction by TextSemanticsProperties.ImeAction

/**
 * Whether the component supports input methods.
 *
 * Supporting input methods means that the component provides a connection to IME (keyboard) and
 * is able to accept input from it. This is typically a text field for instance.
 *
 *  @see TextSemanticsProperties.SupportsInputMethods
 */
fun SemanticsPropertyReceiver.supportsInputMethods() {
    this[TextSemanticsProperties.SupportsInputMethods] = Unit
}