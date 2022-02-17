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
    fun noBeyondBoundsLayoutParent_callsOnCompletedLambda_untilConditionIsFalse() {
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
        rule.runOnIdle {
            beyondBoundsRequester.requestBeyondBoundsLayout(
                direction = After,
                until = { false },
                onBeyondBoundsLayoutCompleted = { blockInvoked = true }
            )
        }

        // Assert.
        assertThat(blockInvoked).isTrue()
    }

    @Test
    fun noBeyondBoundsLayoutParent_callsOnCompletedLambda_untilConditionIsTrue() {
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
        rule.runOnIdle {
            beyondBoundsRequester.requestBeyondBoundsLayout(
                direction = After,
                until = { true },
                onBeyondBoundsLayoutCompleted = { blockInvoked = true }
            )
        }

        // Assert.
       assertThat(blockInvoked).isTrue()
    }

    @Test
    fun beyondBoundsLayoutParent_isInvoked() {
        // Arrange.
        lateinit var beyondBoundsLayoutRequest: BeyondBoundsLayoutRequest
        lateinit var beyondBoundsRequester: BeyondBoundsLayout
        val until: () -> Boolean = { false }
        val direction = After
        val onCompleted = { }
        rule.setContent {
            Box(
                Modifier
                    .modifierLocalProvider(ModifierLocalBeyondBoundsLayout) {
                        object : BeyondBoundsLayout {
                            override fun requestBeyondBoundsLayout(
                                direction: BeyondBoundsLayoutDirection,
                                until: () -> Boolean,
                                onBeyondBoundsLayoutCompleted: () -> Unit
                            ) {
                                beyondBoundsLayoutRequest =
                                    BeyondBoundsLayoutRequest(direction, until, onCompleted)
                            }
                        }
                    }
                    .modifierLocalConsumer {
                        beyondBoundsRequester = ModifierLocalBeyondBoundsLayout.current
                    }
            )
        }

        // Act.
        rule.runOnIdle {
            beyondBoundsRequester.requestBeyondBoundsLayout(
                direction = direction,
                until = until,
                onBeyondBoundsLayoutCompleted = onCompleted
            )
        }

        // Assert.
        assertThat(beyondBoundsLayoutRequest)
            .isEqualTo(BeyondBoundsLayoutRequest(direction, until, onCompleted))
    }

    private data class BeyondBoundsLayoutRequest(
        val direction: BeyondBoundsLayoutDirection? = null,
        val until: (() -> Boolean)? = null,
        val onCompleted: (() -> Unit)? = null
    )
}
