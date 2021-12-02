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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class IndicationTest {

    @get:Rule
    val rule = createComposeRule()

    val testTag = "indication"

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun indication_receivesInitialState() {
        val dispatcher = MutableInteractionSource()
        val countDownLatch = CountDownLatch(1)
        val indication = makeIndication {
            countDownLatch.countDown()
        }
        rule.setContent {
            Box(Modifier.testTag(testTag).size(100.dp).indication(dispatcher, indication))
        }
        assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun indication_click_receivesStateUpdates() {
        // indication should be called 3 times: 0 indication, press, and after click 0 again
        val countDownLatch = CountDownLatch(3)
        val interactions = mutableStateListOf<Interaction>()

        val interactionSource = MutableInteractionSource()

        val indication = makeIndication {
            interactions.lastOrNull() // value read
            countDownLatch.countDown()
        }

        var scope: CoroutineScope? = null

        rule.setContent {
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

        assertThat(countDownLatch.count).isEqualTo(2)
        rule.onNodeWithTag(testTag)
            .assertExists()
            .performTouchInput {
                down(center)
            }

        rule.runOnIdle {
            assertThat(countDownLatch.count).isEqualTo(1)
        }

        rule.onNodeWithTag(testTag)
            .assertExists()
            .performTouchInput {
                up()
            }
        assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
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
    fun testInspectorValue() {
        val state = MutableInteractionSource()
        val indication = makeIndication {}
        rule.setContent {
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
