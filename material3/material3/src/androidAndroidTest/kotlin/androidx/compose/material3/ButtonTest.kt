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
package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ButtonTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                Button(modifier = Modifier.testTag("myButton"), onClick = {}) {
                    Text("myButton")
                }
            }
        }

        rule.onNodeWithTag("myButton")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
    }

    @Test
    fun disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                Button(modifier = Modifier.testTag("myButton"), onClick = {}, enabled = false) {
                    Text("myButton")
                }
            }
        }

        rule.onNodeWithTag("myButton")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun findByTextAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "myButton"

        rule.setMaterialContent(lightColorScheme()) {
            Box {
                Button(onClick = onClick, modifier = Modifier.testTag("myButton")) {
                    Text(text)
                }
            }
        }

        // TODO(b/129400818): this actually finds the text, not the button as
        // merge semantics aren't implemented yet
        rule.onNodeWithTag("myButton")
            // remove this and the todo
            //    rule.onNodeWithText(text)
            .performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun canBeDisabled() {
        val tag = "myButton"

        rule.setMaterialContent(lightColorScheme()) {
            var enabled by remember { mutableStateOf(true) }
            val onClick = { enabled = false }
            Box {
                Button(modifier = Modifier.testTag(tag), onClick = onClick, enabled = enabled) {
                    Text("Hello")
                }
            }
        }
        rule.onNodeWithTag(tag)
            // Confirm the button starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick()
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun clickIsIndependentBetweenButtons() {
        var button1Counter = 0
        val button1OnClick: () -> Unit = { ++button1Counter }
        val button1Tag = "button1"

        var button2Counter = 0
        val button2OnClick: () -> Unit = { ++button2Counter }
        val button2Tag = "button2"

        val text = "myButton"

        rule.setMaterialContent(lightColorScheme()) {
            Column {
                Button(modifier = Modifier.testTag(button1Tag), onClick = button1OnClick) {
                    Text(text)
                }
                Button(modifier = Modifier.testTag(button2Tag), onClick = button2OnClick) {
                    Text(text)
                }
            }
        }

        rule.onNodeWithTag(button1Tag)
            .performClick()

        rule.runOnIdle {
            assertThat(button1Counter).isEqualTo(1)
            assertThat(button2Counter).isEqualTo(0)
        }

        rule.onNodeWithTag(button2Tag)
            .performClick()

        rule.runOnIdle {
            assertThat(button1Counter).isEqualTo(1)
            assertThat(button2Counter).isEqualTo(1)
        }
    }
}
