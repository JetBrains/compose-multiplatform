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
import androidx.compose.ui.layout.BeyondBoundsLayout.BeyondBoundsScope
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.After
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

    // The result of an imaginary operation that is run after we add the beyondBounds items we need.
    private val OperationResult = 10

    @Test
    fun noBeyondBoundsItems() {
        // Arrange.
        var parent: BeyondBoundsLayout? = null
        var blockInvoked = false
        rule.setContent {
            Box(
                Modifier
                    .parentWithoutNonVisibleItems()
                    .modifierLocalConsumer {
                        parent = ModifierLocalBeyondBoundsLayout.current
                    }
            )
        }

        // Act.
        val returnValue = rule.runOnIdle {
            parent!!.layout(After) {
                blockInvoked = true
                OperationResult
            }
        }

        // Assert.
        assertThat(blockInvoked).isFalse()
        assertThat(returnValue).isNull()
    }

    @Test
    fun noItemFound() {
        // Arrange.
        var parent: BeyondBoundsLayout? = null
        var blockInvokeCount = 0
        rule.setContent {
            Box(
                Modifier
                    .parentWithFiveNonVisibleItems()
                    .modifierLocalConsumer {
                        parent = ModifierLocalBeyondBoundsLayout.current
                    }
            )
        }

        // Act.
        val returnValue = rule.runOnIdle {
            assertThat(parent).isNotNull()
            parent?.layout<Int>(After) {
                blockInvokeCount++
                // Always return null, to continue searching and indicate that
                // we didn't find the item we were looking for.
                null
            }
        }

        // Assert.
        assertThat(blockInvokeCount).isEqualTo(5)
        assertThat(returnValue).isNull()
    }

    @Test
    fun hasMoreItemsReturnsFalseWhenItemsRunOut() {
        // Arrange.
        var parent: BeyondBoundsLayout? = null
        val callMap = mutableMapOf<Int, Int?>()
        var iterationCount = 0
        rule.setContent {
            Box(
                Modifier
                    .parentWithFiveNonVisibleItems()
                    .modifierLocalConsumer {
                        parent = ModifierLocalBeyondBoundsLayout.current
                    }
            )
        }

        // Act.
        val returnValue = rule.runOnIdle {
            assertThat(parent).isNotNull()
            parent?.layout(After) {
                val returnValue = if (hasMoreContent) null else OperationResult
                callMap[++iterationCount] = returnValue
                returnValue
            }
        }

        // Assert.
        assertThat(callMap).containsExactlyEntriesIn(
            mapOf(
                1 to null,
                2 to null,
                3 to null,
                4 to null,
                5 to OperationResult,
            )
        )
        assertThat(returnValue).isEqualTo(OperationResult)
    }

    @Test
    fun itemFoundOnFirstIteration() {
        // Arrange.
        var parent: BeyondBoundsLayout? = null
        rule.setContent {
            Box(
                Modifier
                    .parentWithFiveNonVisibleItems()
                    .modifierLocalConsumer {
                        parent = ModifierLocalBeyondBoundsLayout.current
                    }
            )
        }

        // Act.
        val returnValue = rule.runOnIdle {
            assertThat(parent).isNotNull()
            parent?.layout(After) {
                // After the first item was added, we were able to perform our operation.
                OperationResult
            }
        }

        // Assert.
        assertThat(returnValue).isEqualTo(OperationResult)
    }

    @Test
    fun itemFoundOnThirdIteration() {
        // Arrange.
        var parent: BeyondBoundsLayout? = null
        rule.setContent {
            Box(
                Modifier
                    .parentWithFiveNonVisibleItems()
                    .modifierLocalConsumer {
                        parent = ModifierLocalBeyondBoundsLayout.current
                    }
            )
        }

        // Act.
        val returnValue = rule.runOnIdle {
            assertThat(parent).isNotNull()
            var iterationCount = 0
            parent?.layout(After) {
                if (iterationCount++ < 3) null else OperationResult
            }
        }

        // Assert.
        assertThat(returnValue).isEqualTo(OperationResult)
    }

    @Test
    fun iteratorCountWhenCalledMultipleTimes() {
        // Arrange.
        var parent: BeyondBoundsLayout? = null
        var block1InvokeCount = 0
        var block2InvokeCount = 0
        var returnValue1: Int? = null
        var returnValue2: Int? = null
        rule.setContent {
            Box(
                Modifier
                    .parentWithFiveNonVisibleItems()
                    .modifierLocalConsumer {
                        parent = ModifierLocalBeyondBoundsLayout.current
                    }
            )
        }

        // Act.
        rule.runOnIdle {
            assertThat(parent).isNotNull()
            returnValue1 = parent?.layout<Int>(After) {
                block1InvokeCount++
                // Always return null, to indicate that we didn't find the item we were looking for.
                null
            }
            returnValue2 = parent?.layout<Int>(After) {
                block2InvokeCount++
                // Always return null, to indicate that we didn't find the item we were looking for.
                null
            }
        }

        // Assert.
        assertThat(block1InvokeCount++).isEqualTo(5)
        assertThat(block2InvokeCount++).isEqualTo(5)
        assertThat(returnValue1).isNull()
        assertThat(returnValue2).isNull()
    }

    @Test
    fun reentrantIteratorCount() {
        // Arrange.
        var parent: BeyondBoundsLayout? = null
        val direction = After
        var block1InvokeCount = 0
        var block2InvokeCount = 0
        var returnValue1: Int? = null
        var returnValue2: Int? = null
        rule.setContent {
            Box(
                Modifier
                    .parentWithFiveNonVisibleItems()
                    .modifierLocalConsumer {
                        parent = ModifierLocalBeyondBoundsLayout.current
                    }
            )
        }

        // Act.
        rule.runOnIdle {
            assertThat(parent).isNotNull()
            returnValue1 = parent?.layout<Int>(direction) {
                block1InvokeCount++

                if (!hasMoreContent) {
                    // Re-entrant call.
                    returnValue2 =
                        parent?.layout<Int>(direction) {
                            block2InvokeCount++
                            // Always return null, to indicate that we didn't find the item we were looking for.
                            null
                        }
                }

                // Always return null, to indicate that we didn't find the item we were looking for.
                null
            }
        }

        // Assert.
        assertThat(block1InvokeCount++).isEqualTo(5)
        assertThat(block2InvokeCount++).isEqualTo(5)
        assertThat(returnValue1).isNull()
        assertThat(returnValue2).isNull()
    }

    private fun Modifier.parentWithoutNonVisibleItems(): Modifier {
        return this.modifierLocalProvider(ModifierLocalBeyondBoundsLayout) {
            object : BeyondBoundsLayout {
                override fun <T> layout(
                    direction: LayoutDirection,
                    block: BeyondBoundsScope.() -> T?
                ): T? = null
            }
        }
    }

    private fun Modifier.parentWithFiveNonVisibleItems(): Modifier {
        return this.modifierLocalProvider(ModifierLocalBeyondBoundsLayout) {
            object : BeyondBoundsLayout {
                override fun <T> layout(
                    direction: LayoutDirection,
                    block: BeyondBoundsScope.() -> T?
                ): T? {
                    var count = 5
                    var result: T? = null
                    while (count-- > 0 && result == null) {
                        result = block.invoke(
                            object : BeyondBoundsScope {
                                override val hasMoreContent: Boolean
                                    get() = count > 0
                            }
                        )
                    }
                    return result
                }
            }
        }
    }
}
