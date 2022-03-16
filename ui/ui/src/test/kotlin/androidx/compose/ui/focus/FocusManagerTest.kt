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
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.focus.FocusStateImpl.DeactivatedParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.InnerPlaceable
import androidx.compose.ui.node.LayoutNode
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

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
        focusModifier.layoutNodeWrapper = innerPlaceable
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
                Active, ActiveParent, Captured, Deactivated, DeactivatedParent -> initialFocusState
            }
        )
    }

    @Test
    fun releaseFocus_changesStateToInactive() {
        // Arrange.
        focusModifier.focusState = initialFocusState as FocusStateImpl
        if (initialFocusState == ActiveParent || initialFocusState == DeactivatedParent) {
            val childLayoutNode = LayoutNode()
            val child = FocusModifier(Active).apply {
                layoutNodeWrapper = InnerPlaceable(childLayoutNode)
            }
            focusModifier.layoutNodeWrapper!!.layoutNode._children.add(childLayoutNode)
            focusModifier.focusedChild = child
        }

        // Act.
        focusManager.releaseFocus()

        // Assert.
        assertThat(focusModifier.focusState).isEqualTo(
            when (initialFocusState) {
                Active, ActiveParent, Captured, Inactive -> Inactive
                Deactivated, DeactivatedParent -> Deactivated
            }
        )
    }

    @Test
    fun clearFocus_forced() {
        // Arrange.
        focusModifier.focusState = initialFocusState as FocusStateImpl
        if (initialFocusState == ActiveParent || initialFocusState == DeactivatedParent) {
            val childLayoutNode = LayoutNode()
            val child = FocusModifier(Active).apply {
                layoutNodeWrapper = InnerPlaceable(childLayoutNode)
            }
            focusModifier.layoutNodeWrapper!!.layoutNode._children.add(childLayoutNode)
            focusModifier.focusedChild = child
        }

        // Act.
        focusManager.clearFocus(force = true)

        // Assert.
        assertThat(focusModifier.focusState).isEqualTo(
            when (initialFocusState) {
                // If the initial state was focused, assert that after clearing the hierarchy,
                // the root is set to Active.
                Active, ActiveParent, Captured -> Active
                Deactivated, DeactivatedParent -> Deactivated
                Inactive -> Inactive
            }
        )
    }

    @Test
    fun clearFocus_notForced() {
        // Arrange.
        focusModifier.focusState = initialFocusState as FocusStateImpl
        if (initialFocusState == ActiveParent || initialFocusState == DeactivatedParent) {
            val childLayoutNode = LayoutNode()
            val child = FocusModifier(Active).apply {
                layoutNodeWrapper = InnerPlaceable(childLayoutNode)
            }
            focusModifier.layoutNodeWrapper!!.layoutNode._children.add(childLayoutNode)
            focusModifier.focusedChild = child
        }

        // Act.
        focusManager.clearFocus(force = false)

        // Assert.
        assertThat(focusModifier.focusState).isEqualTo(
            when (initialFocusState) {
                // If the initial state was focused, assert that after clearing the hierarchy,
                // the root is set to Active.
                Active, ActiveParent -> Active
                Deactivated, DeactivatedParent -> Deactivated
                Captured -> Captured
                Inactive -> Inactive
            }
        )
    }

    @Test
    fun clearFocus_childIsCaptured() {
        if (initialFocusState == ActiveParent || initialFocusState == DeactivatedParent) {
            // Arrange.
            focusModifier.focusState = initialFocusState as FocusStateImpl
            val childLayoutNode = LayoutNode()
            val child = FocusModifier(Captured).apply {
                layoutNodeWrapper = InnerPlaceable(childLayoutNode)
            }
            focusModifier.layoutNodeWrapper!!.layoutNode._children.add(childLayoutNode)
            focusModifier.focusedChild = child

            // Act.
            focusManager.clearFocus()

            // Assert.
            assertThat(focusModifier.focusState).isEqualTo(initialFocusState)
        }
    }
}
