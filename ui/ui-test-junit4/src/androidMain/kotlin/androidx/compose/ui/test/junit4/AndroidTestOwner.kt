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

package androidx.compose.ui.test.junit4

import android.annotation.SuppressLint
import androidx.compose.ui.node.Owner
import androidx.compose.ui.platform.AndroidOwner
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.InternalTestingApi
import androidx.compose.ui.test.TestOwner
import androidx.compose.ui.test.junit4.android.ComposeIdlingResource
import androidx.compose.ui.text.input.EditOperation
import androidx.compose.ui.text.input.ImeAction

@OptIn(InternalTestingApi::class)
internal class AndroidTestOwner(
    private val composeIdlingResource: ComposeIdlingResource
) : TestOwner {

    @SuppressLint("DocumentExceptions")
    override fun sendTextInputCommand(node: SemanticsNode, command: List<EditOperation>) {
        val owner = node.layoutNode.owner as AndroidOwner

        @Suppress("DEPRECATION")
        runOnUiThread {
            val textInputService = owner.getTextInputServiceOrDie()
            val onEditCommand = textInputService.onEditCommand
                ?: throw IllegalStateException("No input session started. Missing a focus?")
            onEditCommand(command)
        }
    }

    @SuppressLint("DocumentExceptions")
    override fun sendImeAction(node: SemanticsNode, actionSpecified: ImeAction) {
        val owner = node.layoutNode.owner as AndroidOwner

        @Suppress("DEPRECATION")
        runOnUiThread {
            val textInputService = owner.getTextInputServiceOrDie()
            val onImeActionPerformed = textInputService.onImeActionPerformed
                ?: throw IllegalStateException("No input session started. Missing a focus?")
            onImeActionPerformed.invoke(actionSpecified)
        }
    }

    @SuppressLint("DocumentExceptions")
    override fun <T> runOnUiThread(action: () -> T): T {
        return androidx.compose.ui.test.junit4.runOnUiThread(action)
    }

    override fun getOwners(): Set<Owner> {
        return composeIdlingResource.getOwners()
    }

    private fun AndroidOwner.getTextInputServiceOrDie(): TextInputServiceForTests {
        return textInputService as? TextInputServiceForTests
            ?: throw IllegalStateException(
                "Text input service wrapper not set up! Did you use ComposeTestRule?"
            )
    }
}
