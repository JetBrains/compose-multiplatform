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

package androidx.compose.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.test.filters.MediumTest
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertIsInMutuallyExclusiveGroup
import androidx.ui.test.assertIsNotSelected
import androidx.ui.test.assertIsSelected
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.down
import androidx.ui.test.isInMutuallyExclusiveGroup
import androidx.ui.test.onFirst
import androidx.ui.test.onNodeWithText
import androidx.ui.test.performClick
import androidx.ui.test.performGesture
import androidx.ui.test.up
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class SelectableTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun selectable_defaultSemantics() {
        rule.setContent {
            Text(
                "Text in item",
                modifier = Modifier.selectable(selected = true, onClick = {})
            )
        }

        rule.onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(1)
            .onFirst()
            .assertIsInMutuallyExclusiveGroup()
            .assertIsSelected()
    }

    @Test
    fun selectable_defaultClicks() {
        rule.setContent {
            val (selected, onSelected) = remember { mutableStateOf(false) }
            Text(
                "Text in item",
                modifier = Modifier.selectable(
                    selected = selected,
                    onClick = { onSelected(!selected) }
                )
            )
        }

        rule.onNode(isInMutuallyExclusiveGroup())
            .assertIsNotSelected()
            .performClick()
            .assertIsSelected()
            .performClick()
            .assertIsNotSelected()
    }

    @Test
    fun selectable_noClicksNoChanges() {
        rule.setContent {
            val (selected, _) = remember { mutableStateOf(false) }
            Text(
                "Text in item",
                modifier = Modifier.selectable(
                    selected = selected,
                    onClick = {}
                )
            )
        }

        rule.onNode(isInMutuallyExclusiveGroup())
            .assertIsNotSelected()
            .performClick()
            .assertIsNotSelected()
    }

    @Test
    fun selectableTest_interactionState() {
        val interactionState = InteractionState()

        rule.setContent {
            Box {
                Box(
                    Modifier.selectable(
                        selected = true,
                        interactionState = interactionState,
                        onClick = {}
                    )
                ) {
                    Text("SelectableText")
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        rule.onNodeWithText("SelectableText")
            .performGesture { down(center) }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        rule.onNodeWithText("SelectableText")
            .performGesture { up() }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }

    @Test
    fun selectableTest_interactionState_resetWhenDisposed() {
        val interactionState = InteractionState()
        var emitSelectableText by mutableStateOf(true)

        rule.setContent {
            Box {
                if (emitSelectableText) {
                    Box(
                        Modifier.selectable(
                            selected = true,
                            interactionState = interactionState,
                            onClick = {}
                        )
                    ) {
                        Text("SelectableText")
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        rule.onNodeWithText("SelectableText")
            .performGesture { down(center) }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        // Dispose selectable
        rule.runOnIdle {
            emitSelectableText = false
        }

        rule.runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }
}