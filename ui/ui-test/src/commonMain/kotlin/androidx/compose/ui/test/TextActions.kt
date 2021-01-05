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

package androidx.compose.ui.test

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteAllCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction

/**
 * Clears the text in this node in similar way to IME.
 *
 * Note performing this operation requires to get a focus.
 *
 * @param alreadyHasFocus Whether the node already has a focus and thus does not need to be
 * clicked on.
 */
fun SemanticsNodeInteraction.performTextClearance(alreadyHasFocus: Boolean = false) {
    if (!alreadyHasFocus) {
        performClick()
    }
    // TODO: There should be some assertion on focus in the future.

    sendTextInputCommand(listOf(DeleteAllCommand()))
}

/**
 * Sends the given text to this node in similar way to IME.
 *
 * @param text Text to send.
 * @param alreadyHasFocus Whether the node already has a focus and thus does not need to be
 * clicked on.
 */
fun SemanticsNodeInteraction.performTextInput(text: String, alreadyHasFocus: Boolean = false) {
    if (!alreadyHasFocus) {
        performClick()
    }
    // TODO: There should be some assertion on focus in the future.

    sendTextInputCommand(listOf(CommitTextCommand(text, 1)))
}

/**
 * Replaces existing text with the given text in this node in similar way to IME.
 *
 * This does not reflect text selection. All the text gets cleared out and new inserted.
 *
 * @param text Text to send.
 * @param alreadyHasFocus Whether the node already has a focus and thus does not need to be
 * clicked on.
 */
fun SemanticsNodeInteraction.performTextReplacement(
    text: String,
    alreadyHasFocus: Boolean = false
) {
    if (!alreadyHasFocus) {
        performClick()
    }

    // TODO: There should be some assertion on focus in the future.

    sendTextInputCommand(listOf(DeleteAllCommand(), CommitTextCommand(text, 1)))
}

/**
 * Sends to this node the IME action associated with it in similar way to IME.
 *
 * The node needs to define its IME action in semantics.
 *
 * @param alreadyHasFocus Whether the node already has a focus and thus does not need to be
 * clicked on.
 *
 * @throws AssertionError if the node does not support input or does not define IME action.
 * @throws IllegalStateException if tne node did not establish input connection (e.g. is not
 * focused)
 */
fun SemanticsNodeInteraction.performImeAction(alreadyHasFocus: Boolean = false) {
    if (!alreadyHasFocus) {
        performClick()
    }

    val errorOnFail = "Failed to perform IME action."
    val node = fetchSemanticsNode(errorOnFail)

    assert(hasSetTextAction()) { errorOnFail }

    val actionSpecified = node.config.getOrElse(SemanticsProperties.ImeAction) {
        ImeAction.Unspecified
    }
    if (actionSpecified == ImeAction.Unspecified) {
        throw AssertionError(
            buildGeneralErrorMessage(
                "Failed to perform IME action as current node does not specify any.", selector, node
            )
        )
    }

    @OptIn(InternalTestApi::class)
    testContext.testOwner.sendImeAction(node, actionSpecified)
}

internal fun SemanticsNodeInteraction.sendTextInputCommand(command: List<EditCommand>) {
    val errorOnFail = "Failed to perform text input."
    val node = fetchSemanticsNode(errorOnFail)
    assert(hasSetTextAction()) { errorOnFail }

    @OptIn(InternalTestApi::class)
    testContext.testOwner.sendTextInputCommand(node, command)
}
