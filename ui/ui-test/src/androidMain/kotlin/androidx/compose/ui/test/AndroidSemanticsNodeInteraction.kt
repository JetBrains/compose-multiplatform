/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.platform.AndroidOwner
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.text.input.EditOperation
import androidx.compose.ui.text.input.ImeAction

internal actual fun getAllSemanticsNodes(mergingEnabled: Boolean): List<SemanticsNode> =
    androidx.compose.ui.test.android.SynchronizedTreeCollector.getAllSemanticsNodes(mergingEnabled)

internal actual fun SemanticsNodeInteraction.actualPerformImeAction(
    node: SemanticsNode,
    actionSpecified: ImeAction
) {
    @OptIn(ExperimentalLayoutNodeApi::class)
    val owner = node.componentNode.owner as AndroidOwner

    @Suppress("DEPRECATION")
    runOnUiThread {
        val textInputService = owner.getTextInputServiceOrDie()

        val onImeActionPerformed = textInputService.onImeActionPerformed
            ?: throw IllegalStateException("No input session started. Missing a focus?")

        onImeActionPerformed.invoke(actionSpecified)
    }
}

internal actual fun SemanticsNodeInteraction.actualSendTextInputCommand(
    node: SemanticsNode,
    command: List<EditOperation>
) {
    @OptIn(ExperimentalLayoutNodeApi::class)
    val owner = node.componentNode.owner as AndroidOwner

    @Suppress("DEPRECATION")
    runOnUiThread {
        val textInputService = owner.getTextInputServiceOrDie()

        val onEditCommand = textInputService.onEditCommand
            ?: throw IllegalStateException("No input session started. Missing a focus?")

        onEditCommand(command)
    }
}

internal fun AndroidOwner.getTextInputServiceOrDie(): TextInputServiceForTests {
    return this.textInputService as TextInputServiceForTests?
        ?: throw IllegalStateException (
            "Text input service wrapper not set up! Did you use " +
                "ComposeTestRule?"
        )
}