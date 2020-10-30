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

package androidx.ui.test

import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.GestureScope
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("performKeyPress(keyEvent)", "androidx.compose.ui.test")
)
@OptIn(ExperimentalKeyInput::class)
fun SemanticsNodeInteraction.performKeyPress(keyEvent: KeyEvent): Boolean =
    performKeyPress(keyEvent)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("performClick()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.performClick() = performClick()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("performScrollTo()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.performScrollTo() = performScrollTo()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("performSemanticsAction(key, invocation)", "androidx.compose.ui.test")
)
fun <T : Function<Boolean>> SemanticsNodeInteraction.performSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<T>>,
    invocation: (T) -> Unit
) = performSemanticsAction(key, invocation)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("performSemanticsAction(key)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.performSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>
) = performSemanticsAction(key)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("performGesture()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.performGesture(
    block: GestureScope.() -> Unit
) = performGesture(block)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("performTextClearance(alreadyHasFocus)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.performTextClearance(alreadyHasFocus: Boolean = false) =
    performTextClearance(alreadyHasFocus)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("performTextInput(text, alreadyHasFocus)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.performTextInput(text: String, alreadyHasFocus: Boolean = false) =
    performTextInput(text, alreadyHasFocus)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "performTextReplacement(text, alreadyHasFocus)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteraction.performTextReplacement(
    text: String,
    alreadyHasFocus: Boolean = false
) = performTextReplacement(text, alreadyHasFocus)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("performImeAction(alreadyHasFocus)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.performImeAction(alreadyHasFocus: Boolean = false) =
    performImeAction(alreadyHasFocus)