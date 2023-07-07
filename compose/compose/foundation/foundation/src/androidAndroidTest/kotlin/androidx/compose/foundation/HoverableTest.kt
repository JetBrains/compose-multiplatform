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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class HoverableTest {

    @get:Rule
    val rule = createComposeRule()

    val hoverTag = "myHoverable"

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun hoverableText_testInspectorValue() {
        rule.setContent {
            val interactionSource = remember { MutableInteractionSource() }
            val modifier = Modifier.hoverable(interactionSource) as InspectableValue
            Truth.assertThat(modifier.nameFallback).isEqualTo("hoverable")
            Truth.assertThat(modifier.valueOverride).isNull()
            Truth.assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "interactionSource",
                    "enabled",
                )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @ExperimentalComposeUiApi
    @Test
    fun hoverableTest_hovered() {
        var isHovered = false
        val interactionSource = MutableInteractionSource()

        rule.setContent {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .testTag(hoverTag)
                    .hoverable(interactionSource)
            )

            isHovered = interactionSource.collectIsHoveredAsState().value
        }

        rule.onNodeWithTag(hoverTag).performMouseInput {
            enter(Offset(64.dp.toPx(), 64.dp.toPx()))
        }

        rule.waitForIdle()
        Truth.assertThat(isHovered).isTrue()

        rule.onNodeWithTag(hoverTag).performMouseInput {
            moveTo(Offset(96.dp.toPx(), 96.dp.toPx()))
        }

        rule.waitForIdle()
        Truth.assertThat(isHovered).isTrue()

        rule.onNodeWithTag(hoverTag).performMouseInput {
            moveTo(Offset(129.dp.toPx(), 129.dp.toPx()))
        }

        rule.waitForIdle()
        Truth.assertThat(isHovered).isFalse()

        rule.onNodeWithTag(hoverTag).performMouseInput {
            moveTo(Offset(96.dp.toPx(), 96.dp.toPx()))
        }

        rule.waitForIdle()
        Truth.assertThat(isHovered).isTrue()
    }

    @OptIn(ExperimentalTestApi::class)
    @ExperimentalComposeUiApi
    @Test
    fun hoverableTest_interactionSource() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .testTag(hoverTag)
                    .hoverable(interactionSource = interactionSource)
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(hoverTag).performMouseInput {
            enter(Offset(64.dp.toPx(), 64.dp.toPx()))
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(1)
            Truth.assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithTag(hoverTag).performMouseInput {
            moveTo(Offset(129.dp.toPx(), 129.dp.toPx()))
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(2)
            Truth.assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
            Truth.assertThat(interactions[1])
                .isInstanceOf(HoverInteraction.Exit::class.java)
            Truth.assertThat((interactions[1] as HoverInteraction.Exit).enter)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hoverableTest_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitHoverable by mutableStateOf(true)

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitHoverable) {
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .testTag(hoverTag)
                            .hoverable(interactionSource = interactionSource)
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(hoverTag).performMouseInput {
            enter(Offset(64.dp.toPx(), 64.dp.toPx()))
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(1)
            Truth.assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        // Dispose hoverable, Interaction should be gone
        rule.runOnIdle {
            emitHoverable = false
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(2)
            Truth.assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
            Truth.assertThat(interactions[1])
                .isInstanceOf(HoverInteraction.Exit::class.java)
            Truth.assertThat((interactions[1] as HoverInteraction.Exit).enter)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hoverableTest_interactionSource_dontHoverWhenDisabled() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .testTag(hoverTag)
                        .hoverable(interactionSource = interactionSource, enabled = false)
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(hoverTag).performMouseInput {
            enter(Offset(64.dp.toPx(), 64.dp.toPx()))
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).isEmpty()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hoverableTest_interactionSource_resetWhenDisabled() {
        val interactionSource = MutableInteractionSource()
        var enableHoverable by mutableStateOf(true)

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .testTag(hoverTag)
                        .hoverable(interactionSource = interactionSource, enabled = enableHoverable)
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(hoverTag).performMouseInput {
            enter(Offset(64.dp.toPx(), 64.dp.toPx()))
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(1)
            Truth.assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        // Disable hoverable, Interaction should be gone
        rule.runOnIdle {
            enableHoverable = false
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(2)
            Truth.assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
            Truth.assertThat(interactions[1])
                .isInstanceOf(HoverInteraction.Exit::class.java)
            Truth.assertThat((interactions[1] as HoverInteraction.Exit).enter)
                .isEqualTo(interactions[0])
        }
    }
}