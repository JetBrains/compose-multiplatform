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

package androidx.compose.material

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RadioButtonTest {

    @get:Rule
    val rule = createComposeRule()

    private val itemOne = "Bar"
    private val itemTwo = "Foo"
    private val itemThree = "Sap"

    private fun SemanticsNodeInteraction.assertHasSelectedSemantics(): SemanticsNodeInteraction =
        assertIsSelected()

    private fun SemanticsNodeInteraction.assertHasUnSelectedSemantics(): SemanticsNodeInteraction =
        assertIsNotSelected()

    private val options = listOf(itemOne, itemTwo, itemThree)

    @Test
    fun radioGroupTest_defaultSemantics() {
        val selected = mutableStateOf(itemOne)

        rule.setMaterialContent {
            Column {
                options.forEach { item ->
                    RadioButton(
                        modifier = Modifier.testTag(item),
                        selected = (selected.value == item),
                        onClick = { selected.value = item }
                    )
                }
            }
        }

        rule.onNodeWithTag(itemOne)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
            )
            .assertHasSelectedSemantics()
        rule.onNodeWithTag(itemTwo)
            .assertHasUnSelectedSemantics()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
            )
        rule.onNodeWithTag(itemThree)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
            )
            .assertHasUnSelectedSemantics()
    }

    @Test
    fun radioGroupTest_ensureUnselectable() {
        val selected = mutableStateOf(itemOne)

        rule.setMaterialContent {
            Column {
                options.forEach { item ->
                    RadioButton(
                        modifier = Modifier.testTag(item),
                        selected = (selected.value == item),
                        onClick = { selected.value = item }
                    )
                }
            }
        }

        rule.onNodeWithTag(itemOne)
            .assertHasSelectedSemantics()
            .performClick()
            .assertHasSelectedSemantics()

        rule.onNodeWithTag(itemTwo)
            .assertHasUnSelectedSemantics()

        rule.onNodeWithTag(itemThree)
            .assertHasUnSelectedSemantics()
    }

    @Test
    fun radioGroupTest_clickSelect() {
        val selected = mutableStateOf(itemOne)
        rule.setMaterialContent {
            Column {
                options.forEach { item ->
                    RadioButton(
                        modifier = Modifier.testTag(item),
                        selected = (selected.value == item),
                        onClick = { selected.value = item }
                    )
                }
            }
        }
        rule.onNodeWithTag(itemTwo)
            .assertHasUnSelectedSemantics()
            .performClick()
            .assertHasSelectedSemantics()

        rule.onNodeWithTag(itemOne)
            .assertHasUnSelectedSemantics()

        rule.onNodeWithTag(itemThree)
            .assertHasUnSelectedSemantics()
    }

    @Test
    fun radioGroup_untoggleableAndMergeable_whenNullLambda() {
        val parentTag = "parent"
        rule.setMaterialContent {
            Column(Modifier.semantics(mergeDescendants = true) {}.testTag(parentTag)) {
                RadioButton(
                    selected = true,
                    onClick = null,
                    modifier = Modifier.semantics { focused = true }
                )
            }
        }

        rule.onNodeWithTag(parentTag)
            .assertHasNoClickAction()
            .assert(isFocusable()) // Check merged into parent
    }

    @Test
    @LargeTest
    fun radioGroupTest_clickSelectTwoDifferentItems() {
        val selected = mutableStateOf(itemOne)

        rule.setMaterialContent {
            Column {
                options.forEach { item ->
                    RadioButton(
                        modifier = Modifier.testTag(item),
                        selected = (selected.value == item),
                        onClick = { selected.value = item }
                    )
                }
            }
        }

        rule.onNodeWithTag(itemTwo)
            .assertHasUnSelectedSemantics()
            .performClick()
            .assertHasSelectedSemantics()

        rule.onNodeWithTag(itemOne)
            .assertHasUnSelectedSemantics()

        rule.onNodeWithTag(itemThree)
            .assertHasUnSelectedSemantics()
            .performClick()
            .assertHasSelectedSemantics()

        rule.onNodeWithTag(itemOne)
            .assertHasUnSelectedSemantics()

        rule.onNodeWithTag(itemTwo)
            .assertHasUnSelectedSemantics()
    }

    @Test
    fun radioButton_materialSizes_whenSelected() {
        materialSizesTestForValue(selected = true)
    }

    @Test
    fun radioButton_materialSizes_whenNotSelected() {
        materialSizesTestForValue(selected = false)
    }

    private fun materialSizesTestForValue(selected: Boolean) {
        rule
            .setMaterialContentForSizeAssertions {
                RadioButton(selected = selected, onClick = {})
            }
            .assertIsSquareWithSize(2.dp * 2 + 20.dp)
    }
}