/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.BeyondBoundsLayoutDirection.Companion.After
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class BeyondBoundsLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun noBeyondBoundsLayoutParent_conditionBlockReturnsFalse() {
        // Arrange.
        lateinit var beyondBoundsRequester: BeyondBoundsLayout
        var blockInvoked = false
        rule.setContent {
            Box(
                Modifier.modifierLocalConsumer {
                    beyondBoundsRequester = ModifierLocalBeyondBoundsLayout.current
                }
            )
        }

        // Act.
        val returnValue = rule.runOnIdle {
            beyondBoundsRequester.requestBeyondBoundsLayout(direction = After) {
                blockInvoked = true
                false
            }
        }

        // Assert.
        assertThat(blockInvoked).isTrue()
        assertThat(returnValue).isFalse()
    }

    @Test
    fun noBeyondBoundsLayoutParent_conditionBlockReturnsTrue() {
        // Arrange.
        lateinit var beyondBoundsRequester: BeyondBoundsLayout
        var blockInvoked = false
        rule.setContent {
            Box(
                Modifier.modifierLocalConsumer {
                    beyondBoundsRequester = ModifierLocalBeyondBoundsLayout.current
                }
            )
        }

        // Act.
        val returnValue = rule.runOnIdle {
            beyondBoundsRequester.requestBeyondBoundsLayout(direction = After) {
                blockInvoked = true
                true
            }
        }

        // Assert.
        assertThat(blockInvoked).isTrue()
        assertThat(returnValue).isTrue()
    }

    @Test
    fun beyondBoundsLayoutParent_conditionBlockReturnsFalse() {
        // Arrange.
        lateinit var beyondBoundsLayoutRequest: BeyondBoundsLayoutRequest
        lateinit var beyondBoundsRequester: BeyondBoundsLayout
        val block: () -> Boolean = { false }
        val direction = After
        rule.setContent {
            Box(
                Modifier
                    .modifierLocalProvider(ModifierLocalBeyondBoundsLayout) {
                        object : BeyondBoundsLayout {
                            override fun requestBeyondBoundsLayout(
                                direction: BeyondBoundsLayoutDirection,
                                block: () -> Boolean
                            ): Boolean {
                                beyondBoundsLayoutRequest =
                                    BeyondBoundsLayoutRequest(direction, block)
                                return block.invoke()
                            }
                        }
                    }
                    .modifierLocalConsumer {
                        beyondBoundsRequester = ModifierLocalBeyondBoundsLayout.current
                    }
            )
        }

        // Act.
        val returnValue = rule.runOnIdle {
            beyondBoundsRequester.requestBeyondBoundsLayout(direction, block)
        }

        // Assert.
        assertThat(beyondBoundsLayoutRequest).isEqualTo(BeyondBoundsLayoutRequest(direction, block))
        assertThat(returnValue).isFalse()
    }

    @Test
    fun beyondBoundsLayoutParent_conditionBlockReturnsTrue() {
        // Arrange.
        lateinit var beyondBoundsLayoutRequest: BeyondBoundsLayoutRequest
        lateinit var beyondBoundsRequester: BeyondBoundsLayout
        val block: () -> Boolean = { true }
        val direction = After
        rule.setContent {
            Box(
                Modifier
                    .modifierLocalProvider(ModifierLocalBeyondBoundsLayout) {
                        object : BeyondBoundsLayout {
                            override fun requestBeyondBoundsLayout(
                                direction: BeyondBoundsLayoutDirection,
                                block: () -> Boolean
                            ): Boolean {
                                beyondBoundsLayoutRequest =
                                    BeyondBoundsLayoutRequest(direction, block)
                                return block.invoke()
                            }
                        }
                    }
                    .modifierLocalConsumer {
                        beyondBoundsRequester = ModifierLocalBeyondBoundsLayout.current
                    }
            )
        }

        // Act.
        val returnValue = rule.runOnIdle {
            beyondBoundsRequester.requestBeyondBoundsLayout(direction, block)
        }

        // Assert.
        assertThat(beyondBoundsLayoutRequest).isEqualTo(BeyondBoundsLayoutRequest(direction, block))
        assertThat(returnValue).isTrue()
    }

    private data class BeyondBoundsLayoutRequest(
        val direction: BeyondBoundsLayoutDirection? = null,
        val block: (() -> Boolean)? = null,
    )
}
