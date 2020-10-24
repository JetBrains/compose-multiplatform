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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ContentDrawScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.down
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performGesture
import androidx.ui.test.up
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class IndicationTest {

    @get:Rule
    val rule = createComposeRule()

    val testTag = "indication"

    @Test
    fun indication_receivesInitialState() {
        val state = InteractionState()
        val countDownLatch = CountDownLatch(1)
        val indication = makeIndication {
            // just wait for initial draw with empty interaction
            if (it.value.isEmpty()) {
                countDownLatch.countDown()
            }
        }
        rule.setContent {
            Box(Modifier.testTag(testTag).preferredSize(100.dp).indication(state, indication))
        }
        assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun indication_click_receivesStateUpdates() {
        // indicaiton should be called 3 times: 0 indication, press, and after click 0 again
        val countDownLatch = CountDownLatch(3)
        val indication = makeIndication {
            it.value // value read
            countDownLatch.countDown()
        }
        rule.setContent {
            Box(
                Modifier
                    .testTag(testTag)
                    .preferredSize(100.dp)
                    .clickable(indication = indication) {}
            )
        }
        assertThat(countDownLatch.count).isEqualTo(2)
        rule.onNodeWithTag(testTag)
            .assertExists()
            .performGesture {
                down(center)
            }
        rule.runOnIdle {
            assertThat(countDownLatch.count).isEqualTo(1)
        }
        rule.onNodeWithTag(testTag)
            .assertExists()
            .performGesture {
                up()
            }
        assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun indication_disposed_whenIndicationRemoved() {
        val state = InteractionState()
        val switchState = mutableStateOf(true)
        val countDownLatch = CountDownLatch(2)
        val indication = makeIndication(
            onDispose = { countDownLatch.countDown() },
            onDraw = { countDownLatch.countDown() }
        )
        rule.setContent {
            val switchableIndication =
                if (switchState.value) Modifier.indication(state, indication) else Modifier
            Box(Modifier.testTag(testTag).preferredSize(100.dp).then(switchableIndication))
        }
        assertThat(countDownLatch.count).isEqualTo(1)
        rule.runOnIdle {
            switchState.value = !switchState.value
        }
        rule.runOnIdle {
            assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    @Test
    @Ignore("b/155466122: multitouch is not supported yet")
    fun indication_multiplyPress_firstWins() {
        var lastPosition: Offset? = null
        val indication = makeIndication {
            it.value // value read
            lastPosition = it.interactionPositionFor(Interaction.Pressed)
        }
        rule.setContent {
            Box(
                Modifier
                    .testTag(testTag)
                    .preferredSize(100.dp)
                    .clickable(indication = indication) { }
            )
        }
        assertThat(lastPosition).isNull()
        var position1: Offset? = null
        rule.onNodeWithTag(testTag)
            .assertExists()
            .performGesture {
                position1 = Offset(center.x, center.y + 20f)
                // pointer 1, when we have multitouch
                down(position1!!)
            }
        rule.runOnIdle {
            assertThat(lastPosition).isEqualTo(position1!!)
        }
        rule.onNodeWithTag(testTag)
            .assertExists()
            .performGesture {
                val position2 = Offset(center.x + 20f, center.y)
                // pointer 2, when we have multitouch
                down(position2)
            }
        // should be still position1
        rule.runOnIdle {
            assertThat(lastPosition).isEqualTo(position1!!)
        }
        rule.onNodeWithTag(testTag)
            .assertExists()
            .performGesture {
                // pointer 1, when we have multitouch
                up()
            }
        rule.runOnIdle {
            assertThat(lastPosition).isNull()
        }
        rule.onNodeWithTag(testTag)
            .assertExists()
            .performGesture {
                // pointer 2, when we have multitouch
                up()
            }
    }

    private fun makeIndication(
        onDispose: () -> Unit = {},
        onDraw: (InteractionState) -> Unit
    ): Indication {
        return object : Indication {
            override fun createInstance(): IndicationInstance {
                return object : IndicationInstance {
                    override fun ContentDrawScope.drawIndication(
                        interactionState: InteractionState
                    ) {
                        onDraw(interactionState)
                    }

                    override fun onDispose() {
                        super.onDispose()
                        onDispose()
                    }
                }
            }
        }
    }
}