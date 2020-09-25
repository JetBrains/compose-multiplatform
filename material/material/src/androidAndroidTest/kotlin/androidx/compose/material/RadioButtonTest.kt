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

import androidx.compose.foundation.Strings
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.assertIsInMutuallyExclusiveGroup
import androidx.ui.test.assertIsNotSelected
import androidx.ui.test.assertIsSelected
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performClick
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
        assertIsInMutuallyExclusiveGroup()
            .assertIsSelected()
            .assertValueEquals(Strings.Selected)

    private fun SemanticsNodeInteraction.assertHasUnSelectedSemantics(): SemanticsNodeInteraction =
        assertIsInMutuallyExclusiveGroup()
            .assertIsNotSelected()
            .assertValueEquals(Strings.NotSelected)

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

        rule.onNodeWithTag(itemOne).assertHasSelectedSemantics()
        rule.onNodeWithTag(itemTwo).assertHasUnSelectedSemantics()
        rule.onNodeWithTag(itemThree).assertHasUnSelectedSemantics()
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