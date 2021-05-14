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

import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Disabled
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.InnerPlaceable
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifiedFocusNode
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.jvm.JvmStatic

@RunWith(Parameterized::class)
class FocusManagerTest(private val initialFocusState: FocusState) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "rootInitialFocus = {0}")
        fun initParameters(): List<FocusState> = FocusStateImpl.values().asList()
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
        focusModifier.focusState = initialFocusState as FocusStateImpl

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
    fun releaseFocus_changesStateToInactive() {
        // Arrange.
        focusModifier.focusState = initialFocusState as FocusStateImpl
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
    fun clearFocus_forced() {
        // Arrange.
        focusModifier.focusState = initialFocusState as FocusStateImpl
        if (initialFocusState == ActiveParent) {
            val childLayoutNode = LayoutNode()
            val child = ModifiedFocusNode(InnerPlaceable(childLayoutNode), FocusModifier(Active))
            focusModifier.focusNode.layoutNode._children.add(childLayoutNode)
            focusModifier.focusedChild = child
        }

        // Act.
        focusManager.clearFocus(forcedClear = true)

        // Assert.
        assertThat(focusModifier.focusState).isEqualTo(
            when (initialFocusState) {
                // If the initial state was focused, assert that after clearing the hierarchy,
                // the root is set to Active.
                Active, ActiveParent, Captured -> Active
                Disabled, Inactive -> initialFocusState
            }
        )
    }

    @Test
    fun clearFocus_notForced() {
        // Arrange.
        focusModifier.focusState = initialFocusState as FocusStateImpl
        if (initialFocusState == ActiveParent) {
            val childLayoutNode = LayoutNode()
            val child = ModifiedFocusNode(InnerPlaceable(childLayoutNode), FocusModifier(Active))
            focusModifier.focusNode.layoutNode._children.add(childLayoutNode)
            focusModifier.focusedChild = child
        }

        // Act.
        focusManager.clearFocus(forcedClear = false)

        // Assert.
        assertThat(focusModifier.focusState).isEqualTo(
            when (initialFocusState) {
                // If the initial state was focused, assert that after clearing the hierarchy,
                // the root is set to Active.
                Active, ActiveParent -> Active
                Captured, Disabled, Inactive -> initialFocusState
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
