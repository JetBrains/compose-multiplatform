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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class AssertsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun assertIsOn_forCheckedElement_isOk() {
        rule.setContent {
            BoundaryNode { testTag = "test"; toggleableState = ToggleableState.On }
        }

        rule.onNodeWithTag("test")
            .assertIsOn()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOn_forUncheckedElement_throwsError() {
        rule.setContent {
            BoundaryNode { testTag = "test"; toggleableState = ToggleableState.Off }
        }

        rule.onNodeWithTag("test")
            .assertIsOn()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOn_forNotToggleableElement_throwsError() {
        rule.setContent {
            BoundaryNode { testTag = "test" }
        }

        rule.onNodeWithTag("test")
            .assertIsOn()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOff_forCheckedElement_throwsError() {
        rule.setContent {
            BoundaryNode { testTag = "test"; toggleableState = ToggleableState.On }
        }

        rule.onNodeWithTag("test")
            .assertIsOff()
    }

    @Test
    fun assertIsOff_forUncheckedElement_isOk() {
        rule.setContent {
            BoundaryNode { testTag = "test"; toggleableState = ToggleableState.Off }
        }

        rule.onNodeWithTag("test")
            .assertIsOff()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOff_forNotToggleableElement_throwsError() {
        rule.setContent {
            BoundaryNode { testTag = "test"; }
        }

        rule.onNodeWithTag("test")
            .assertIsOff()
    }

    @Test(expected = AssertionError::class)
    fun assertIsSelected_forNotSelectedElement_throwsError() {
        rule.setContent {
            BoundaryNode { testTag = "test"; selected = false }
        }

        rule.onNodeWithTag("test")
            .assertIsSelected()
    }

    @Test
    fun assertIsSelected_forSelectedElement_isOk() {
        rule.setContent {
            BoundaryNode { testTag = "test"; selected = true }
        }

        rule.onNodeWithTag("test")
            .assertIsSelected()
    }

    @Test(expected = AssertionError::class)
    fun assertIsSelected_forNotSelectableElement_throwsError() {
        rule.setContent {
            BoundaryNode { testTag = "test"; }
        }

        rule.onNodeWithTag("test")
            .assertIsSelected()
    }

    @Test(expected = AssertionError::class)
    fun assertIsNotSelected_forSelectedElement_throwsError() {
        rule.setContent {
            BoundaryNode { testTag = "test"; selected = true }
        }

        rule.onNodeWithTag("test")
            .assertIsNotSelected()
    }

    @Test
    fun assertIsNotSelected_forNotSelectedElement_isOk() {
        rule.setContent {
            BoundaryNode { testTag = "test"; selected = false }
        }

        rule.onNodeWithTag("test")
            .assertIsNotSelected()
    }

    @Test(expected = AssertionError::class)
    fun assertIsNotSelected_forNotSelectableElement_throwsError() {
        rule.setContent {
            BoundaryNode { testTag = "test"; }
        }

        rule.onNodeWithTag("test")
            .assertIsNotSelected()
    }

    @Composable
    fun BoundaryNode(props: (SemanticsPropertyReceiver.() -> Unit)) {
        Column(Modifier.semantics(properties = props)) {}
    }
}