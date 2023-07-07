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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.containsExactly
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isNull
import androidx.compose.foundation.isTrue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class IndicationTest {


    val testTag = "indication"

    @BeforeTest
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @AfterTest
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun indication_receivesInitialState() = runSkikoComposeUiTest {
        val dispatcher = MutableInteractionSource()
        var counter = 0
        val indication = makeIndication {
            counter++
        }
        setContent {
            Box(Modifier.testTag(testTag).size(100.dp).indication(dispatcher, indication))
        }
        waitUntil(1000L) {
            counter == 1
        }
        assertEquals(1, counter)
    }

    @Test
    fun indication_click_receivesStateUpdates() = runSkikoComposeUiTest {
        // indication should be called 3 times: 0 indication, press, and after click 0 again
        var countDownLatch = 3
        val interactions = mutableStateListOf<Interaction>()

        val interactionSource = MutableInteractionSource()

        val indication = makeIndication {
            interactions.lastOrNull() // value read
            countDownLatch--
        }

        var scope: CoroutineScope? = null

        setContent {
            scope = rememberCoroutineScope()
            Box(
                Modifier
                    .testTag(testTag)
                    .size(100.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = indication,
                    ) {}
            )
        }

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        assertThat(countDownLatch).isEqualTo(2)

        onNodeWithTag(testTag)
            .assertExists()
            .performTouchInput {
                down(center)
            }

        runOnIdle {
            assertThat(countDownLatch).isEqualTo(1)
        }

        onNodeWithTag(testTag)
            .assertExists()
            .performTouchInput {
                up()
            }

        waitUntil { countDownLatch == 0 }
        assertThat(countDownLatch == 0).isTrue()
    }

    private fun makeIndication(onDraw: () -> Unit): Indication {
        return object : Indication {
            @Composable
            override fun rememberUpdatedInstance(
                interactionSource: InteractionSource
            ): IndicationInstance {
                return remember(interactionSource) {
                    object : IndicationInstance {
                        override fun ContentDrawScope.drawIndication() {
                            onDraw()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testInspectorValue() = runSkikoComposeUiTest {
        val state = MutableInteractionSource()
        val indication = makeIndication {}
        setContent {
            val modifier = Modifier.indication(state, indication) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("indication")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "indication",
                "interactionSource"
            )
        }
    }
}
