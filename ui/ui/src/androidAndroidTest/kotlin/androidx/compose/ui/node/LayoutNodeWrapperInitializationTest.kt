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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusModifier
import androidx.compose.ui.focus.FocusStateImpl
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.KeyInputModifier
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.PointerInteropFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LayoutNodeWrapperInitializationTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun initializeIsCalledWhenFocusNodeIsCreated() {
        // Arrange.
        val focusModifier = FocusModifier(FocusStateImpl.Inactive)

        // Act.
        rule.setContent {
            Box(Modifier.focusTarget(focusModifier))
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusModifier).isNotNull()
        }
    }

    @Test
    fun initializeIsCalledWhenKeyInputNodeIsCreated() {
        // Arrange.
        val keyInputModifier = KeyInputModifier(null, null)

        // Act.
        rule.setContent {
            Box(modifier = keyInputModifier)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(keyInputModifier.layoutNode).isNotNull()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun initializeIsCalledWhenPointerInputNodeWrapperIsCreated() {
        // Arrange.
        val pointerInputModifier = PointerInteropFilter()

        // Act.
        rule.setContent {
            Box(modifier = pointerInputModifier)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(pointerInputModifier.pointerInputFilter.layoutCoordinates).isNotNull()
        }
    }

    @Test
    fun initializeIsCalledWhenOnGloballyPositionedNodeIsCreated() {
        // Arrange.
        lateinit var layoutCoordinates: LayoutCoordinates

        // Act.
        rule.setContent {
            Box(modifier = Modifier.onGloballyPositioned { layoutCoordinates = it })
        }

        // Assert.
        rule.runOnIdle {
            val layoutNodeWrapper = layoutCoordinates as LayoutNodeWrapper
            val callbacks = layoutNodeWrapper.layoutNode.getOrCreateOnPositionedCallbacks()
            assertThat(callbacks.asMutableList()).isNotEmpty()
        }
    }

    @Test
    fun initializeIsCalledWhenFocusNodeIsReused() {
        // Arrange.
        lateinit var focusModifier: FocusModifier
        lateinit var scope: RecomposeScope
        rule.setContent {
            scope = currentRecomposeScope
            focusModifier = FocusModifier(FocusStateImpl.Inactive)
            Box(Modifier.focusTarget(focusModifier))
        }

        // Act.
        rule.runOnIdle { scope.invalidate() }

        // Assert.
        rule.runOnIdle { assertThat(focusModifier).isNotNull() }
    }

    @Test
    fun initializeIsCalledWhenKeyInputNodeIsReused() {
        // Arrange.
        lateinit var keyInputModifier: KeyInputModifier
        lateinit var scope: RecomposeScope
        rule.setContent {
            scope = currentRecomposeScope
            keyInputModifier = KeyInputModifier(null, null)
            Box(modifier = keyInputModifier)
        }

        // Act.
        rule.runOnIdle { scope.invalidate() }

        // Assert.
        rule.runOnIdle {
            assertThat(keyInputModifier.layoutNode).isNotNull()
        }
    }

    @ExperimentalComposeUiApi
    @Test
    fun initializeIsCalledWhenPointerInputNodeIsReused() {
        // Arrange.
        lateinit var pointerInputModifier: PointerInputModifier
        lateinit var scope: RecomposeScope
        rule.setContent {
            scope = currentRecomposeScope
            pointerInputModifier = PointerInteropFilter()
            Box(modifier = pointerInputModifier)
        }

        // Act.
        rule.runOnIdle { scope.invalidate() }

        // Assert.
        rule.runOnIdle {
            assertThat(pointerInputModifier.pointerInputFilter.layoutCoordinates).isNotNull()
        }
    }

    @Test
    fun initializeIsCalledWhenOnGloballyPositionedNodeIsReused() {
        // Arrange.
        lateinit var layoutCoordinates: LayoutCoordinates
        lateinit var scope: RecomposeScope
        rule.setContent {
            scope = currentRecomposeScope
            Box(modifier = Modifier.onGloballyPositioned { layoutCoordinates = it })
        }

        // Act.
        rule.runOnIdle { scope.invalidate() }

        // Assert.
        rule.runOnIdle {
            val layoutNodeWrapper = layoutCoordinates as LayoutNodeWrapper
            val callbacks = layoutNodeWrapper.layoutNode.getOrCreateOnPositionedCallbacks()
            assertThat(callbacks.asMutableList()).isNotEmpty()
        }
    }
}
