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

package androidx.compose.ui.focus

import androidx.compose.ui.FocusModifier
import androidx.compose.ui.focus.FocusState.Active
import androidx.compose.ui.focus.FocusState.ActiveParent
import androidx.compose.ui.focus.FocusState.Captured
import androidx.compose.ui.focus.FocusState.Disabled
import androidx.compose.ui.focus.FocusState.Inactive
import androidx.compose.ui.node.InnerPlaceable
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifiedFocusNode
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.jvm.JvmStatic

@OptIn(
    ExperimentalFocus::class,
)
@RunWith(Parameterized::class)
class FocusManagerTest(private val initialFocusState: FocusState) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "rootInitialFocus = {0}")
        fun initParameters() = FocusState.values()
    }

    private val focusModifier = FocusModifier(Inactive)
    private val focusManager = FocusManagerImpl(focusModifier)

    @Before
    fun setup() {
        val innerPlaceable = InnerPlaceable(LayoutNode())
        focusModifier.focusNode = ModifiedFocusNode(innerPlaceable, focusModifier)
    }

    @Test
    fun defaultFocusState() {
        assertThat(focusModifier.focusState).isEqualTo(Inactive)
    }

    @Test
    fun takeFocus_onlyInactiveChangesState() {
        // Arrange.
        focusModifier.focusState = initialFocusState

        // Act.
        focusManager.takeFocus()

        // Assert.
        assertThat(focusModifier.focusState).isEqualTo(
            when (initialFocusState) {
                Inactive -> Active
                Active, ActiveParent, Captured, Disabled -> initialFocusState
            }
        )
    }

    @Test
    fun clearFocus_changesStateToInactive() {
        // Arrange.
        focusModifier.focusState = initialFocusState
        if (initialFocusState == ActiveParent) {
            val childLayoutNode = LayoutNode()
            val child = ModifiedFocusNode(InnerPlaceable(childLayoutNode), FocusModifier(Active))
            focusModifier.focusNode.layoutNode._children.add(childLayoutNode)
            focusModifier.focusedChild = child
        }

        // Act.
        focusManager.releaseFocus()

        // Assert.
        assertThat(focusModifier.focusState).isEqualTo(
            when (initialFocusState) {
                Active, ActiveParent, Captured, Inactive -> Inactive
                Disabled -> initialFocusState
            }
        )
    }

    @Test
    fun clearFocus_childIsCaptured() {
        // Arrange.
        focusModifier.focusState = ActiveParent
        val childLayoutNode = LayoutNode()
        val child = ModifiedFocusNode(InnerPlaceable(childLayoutNode), FocusModifier(Captured))
        focusModifier.focusNode.layoutNode._children.add(childLayoutNode)
        focusModifier.focusedChild = child

        // Act.
        focusManager.clearFocus()

        // Assert.
        assertThat(focusModifier.focusState).isEqualTo(ActiveParent)
    }
}
