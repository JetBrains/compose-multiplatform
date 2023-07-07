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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.hasSize
import androidx.compose.foundation.isNull
import androidx.compose.foundation.containsExactly
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isEmpty
import androidx.compose.foundation.isFalse
import androidx.compose.foundation.isTrue
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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class HoverableTest {


    val hoverTag = "myHoverable"

    @BeforeTest
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @AfterTest
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun hoverableText_testInspectorValue() = runSkikoComposeUiTest {
        setContent {
            val interactionSource = remember { MutableInteractionSource() }
            val modifier = Modifier.hoverable(interactionSource) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("hoverable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "interactionSource",
                    "enabled",
                )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @ExperimentalComposeUiApi
    @Test
    fun hoverableTest_hovered() = runSkikoComposeUiTest {
        var isHovered = false
        val interactionSource = MutableInteractionSource()

        setContent {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .testTag(hoverTag)
                    .hoverable(interactionSource)
            )

            isHovered = interactionSource.collectIsHoveredAsState().value
        }

        onNodeWithTag(hoverTag).performMouseInput {
            enter(Offset(64.dp.toPx(), 64.dp.toPx()))
        }

        waitForIdle()
        assertThat(isHovered).isTrue()

        onNodeWithTag(hoverTag).performMouseInput {
            moveTo(Offset(96.dp.toPx(), 96.dp.toPx()))
        }

        waitForIdle()
        assertThat(isHovered).isTrue()

        onNodeWithTag(hoverTag).performMouseInput {
            moveTo(Offset(129.dp.toPx(), 129.dp.toPx()))
        }

        waitForIdle()
        assertThat(isHovered).isFalse()

        onNodeWithTag(hoverTag).performMouseInput {
            moveTo(Offset(96.dp.toPx(), 96.dp.toPx()))
        }

        waitForIdle()
        assertThat(isHovered).isTrue()
    }

    @OptIn(ExperimentalTestApi::class)
    @ExperimentalComposeUiApi
    @Test
    fun hoverableTest_interactionSource() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        setContent {
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

        runOnIdle {
            assertThat(interactions).isEmpty()
        }

        onNodeWithTag(hoverTag).performMouseInput {
            enter(Offset(64.dp.toPx(), 64.dp.toPx()))
        }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertTrue { interactions.first() is HoverInteraction.Enter }
        }

        onNodeWithTag(hoverTag).performMouseInput {
            moveTo(Offset(129.dp.toPx(), 129.dp.toPx()))
        }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertTrue { interactions.first() is HoverInteraction.Enter }
            assertTrue { interactions[1] is HoverInteraction.Exit }
            assertThat((interactions[1] as HoverInteraction.Exit).enter)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hoverableTest_interactionSource_resetWhenDisposed() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()
        var emitHoverable by mutableStateOf(true)

        var scope: CoroutineScope? = null

        setContent {
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

        runOnIdle {
            assertThat(interactions).isEmpty()
        }

        onNodeWithTag(hoverTag).performMouseInput {
            enter(Offset(64.dp.toPx(), 64.dp.toPx()))
        }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertTrue { interactions.first() is HoverInteraction.Enter }
        }

        // Dispose hoverable, Interaction should be gone
        runOnIdle {
            emitHoverable = false
        }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertTrue { interactions.first() is HoverInteraction.Enter }
            assertTrue { interactions[1] is HoverInteraction.Exit }
            assertThat((interactions[1] as HoverInteraction.Exit).enter)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hoverableTest_interactionSource_dontHoverWhenDisabled() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        setContent {
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

        runOnIdle {
            assertThat(interactions).isEmpty()
        }

        onNodeWithTag(hoverTag).performMouseInput {
            enter(Offset(64.dp.toPx(), 64.dp.toPx()))
        }

        runOnIdle {
            assertThat(interactions).isEmpty()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hoverableTest_interactionSource_resetWhenDisabled() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()
        var enableHoverable by mutableStateOf(true)

        var scope: CoroutineScope? = null

        setContent {
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

        runOnIdle {
            assertThat(interactions).isEmpty()
        }

        onNodeWithTag(hoverTag).performMouseInput {
            enter(Offset(64.dp.toPx(), 64.dp.toPx()))
        }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertTrue { interactions.first() is HoverInteraction.Enter }
        }

        // Disable hoverable, Interaction should be gone
        runOnIdle {
            enableHoverable = false
        }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertTrue { interactions.first() is HoverInteraction.Enter }
            assertTrue { interactions[1] is HoverInteraction.Exit }
            assertThat((interactions[1] as HoverInteraction.Exit).enter)
                .isEqualTo(interactions[0])
        }
    }
}
