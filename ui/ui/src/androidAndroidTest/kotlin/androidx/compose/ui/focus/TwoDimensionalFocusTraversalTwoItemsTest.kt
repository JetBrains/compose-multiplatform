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

package androidx.compose.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val invalid = "Not applicable to a 2D focus search."

@RunWith(Parameterized::class)
class TwoDimensionalFocusTraversalTwoItemsTest(param: Param) {
    @get:Rule
    val rule = createComposeRule()

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(val focusDirection: FocusDirection) {
        override fun toString() = focusDirection.toString()
    }

    private lateinit var focusManager: FocusManager
    private val initialFocus: FocusRequester = FocusRequester()
    private val focusedItem = mutableStateOf(false)
    private val candidate = mutableStateOf(false)
    private val focusDirection = param.focusDirection

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(Param(Left), Param(Right), Param(Up), Param(Down))
    }

    /**
     *                       ____________
     *                      | candidate |
     *                      |___________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @MediumTest
    @Test
    fun nonOverlappingCandidate1() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 10, 10)
            FocusableBox(focusedItem, 0, 20, 30, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Down, Left, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                         ____________
     *                        | candidate |
     *                        |___________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate2() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 10, 0, 10, 10)
            FocusableBox(focusedItem, 0, 20, 30, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Down, Left, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                           ____________
     *                          | candidate |
     *                          |___________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate3() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 20, 0, 10, 10)
            FocusableBox(focusedItem, 0, 20, 30, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Down, Left, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |   candidate   |
     *                      |_______________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate4() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 10, 10)
            FocusableBox(focusedItem, 0, 20, 10, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Down, Left, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ___________________
     *                      |     candidate    |
     *                      |__________________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate5() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 30, 10)
            FocusableBox(focusedItem, 0, 20, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Down, Left, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                               ________________
     *                              |   candidate   |
     *                              |_______________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate6() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 10, 0, 20, 10)
            FocusableBox(focusedItem, 0, 20, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up, Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                       ________________
     *                                      |   candidate   |
     *                                      |_______________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate7() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 20, 0, 20, 10)
            FocusableBox(focusedItem, 0, 20, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up, Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                          ________________
     *                                         |   candidate   |
     *                                         |_______________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate8() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 30, 0, 20, 10)
            FocusableBox(focusedItem, 0, 20, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up, Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                          ________________
     *                                         |   candidate   |
     *                       ________________  |_______________|
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate9() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 30, 0, 20, 10)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up, Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                          ________________
     *                       ________________  |   candidate   |
     *                      |  focusedItem  |  |_______________|
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate10() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 30, 0, 20, 10)
            FocusableBox(focusedItem, 0, 5, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up, Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                          ________________
     *                       ________________  |               |
     *                      |  focusedItem  |  |   candidate   |
     *                      |_______________|  |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate11() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 30, 0, 20, 20)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                          ________________
     *                       ________________  |               |
     *                      |  focusedItem  |  |   candidate   |
     *                      |_______________|  |               |
     *                                         |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate12() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
            FocusableBox(candidate, 30, 0, 20, 30)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________   ________________
     *                      |               |  |   candidate   |
     *                      |  focusedItem  |  |_______________|
     *                      |               |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate13() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
            FocusableBox(candidate, 30, 0, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |               |   ________________
     *                      |  focusedItem  |  |   candidate   |
     *                      |               |  |_______________|
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate14() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 30, initialFocus)
            FocusableBox(candidate, 30, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |               |
     *                      |  focusedItem  |   ________________
     *                      |               |  |   candidate   |
     *                      |_______________|  |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate15() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
            FocusableBox(candidate, 30, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________   ________________
     *                      |  focusedItem  |  |   candidate   |
     *                      |_______________|  |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate16() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 30, 0, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________   ________________
     *                      |  focusedItem  |  |               |
     *                      |_______________|  |   candidate   |
     *                                         |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate17() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 30, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |   ________________
     *                      |_______________|  |   candidate   |
     *                                         |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate18() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 30, 5, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|   ________________
     *                                         |   candidate   |
     *                                         |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate19() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 30, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                                          ________________
     *                                         |   candidate   |
     *                                         |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate20() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 30, 20, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                                       ________________
     *                                      |   candidate   |
     *                                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate21() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 20, 20, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                               ________________
     *                              |   candidate   |
     *                              |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate22() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 10, 20, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Left -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                       _____________________
     *                      |      candidate     |
     *                      |____________________|
     */
    @MediumTest
    @Test
    fun nonOverlappingCandidate23() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 0, 20, 30, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Left, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                       ________________
     *                      |   candidate   |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate24() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 0, 20, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Left, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                         ______________
     *                        |  candidate  |
     *                        |_____________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate25() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 30, 10, initialFocus)
            FocusableBox(candidate, 10, 20, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Left, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                         ____________
     *                        | candidate |
     *                        |___________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate26() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 30, 10, initialFocus)
            FocusableBox(candidate, 10, 20, 10, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Left, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                       ____________
     *                      | candidate |
     *                      |___________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate27() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 30, 10, initialFocus)
            FocusableBox(candidate, 0, 20, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Left, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                  _____________________
     *                 |      candidate     |
     *                 |____________________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate28() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 10, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 0, 20, 30, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Left, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                ________________
     *               |   candidate   |
     *               |_______________|
     */
    @MediumTest
    @Test
    fun nonOverlappingCandidate29() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 20, 20, 10)
            FocusableBox(focusedItem, 10, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *       ________________
     *      |   candidate   |
     *      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate30() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 20, 20, 10)
            FocusableBox(focusedItem, 20, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *    ________________
     *   |   candidate   |
     *   |_______________|
     */
    @MediumTest
    @Test
    fun nonOverlappingCandidate31() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 20, 20, 10)
            FocusableBox(focusedItem, 30, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *    ________________  |_______________|
     *   |   candidate   |
     *   |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate32() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 10, 20, 10)
            FocusableBox(focusedItem, 30, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *    ________________  |  focusedItem  |
     *   |   candidate   |  |_______________|
     *   |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate33() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 5, 20, 10)
            FocusableBox(focusedItem, 30, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ________________    ________________
     *  |               |   |  focusedItem  |
     *  |   candidate   |   |_______________|
     *  |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate34() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 15)
            FocusableBox(focusedItem, 30, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ________________   ________________
     *  |   candidate   |  |  focusedItem  |
     *  |_______________|  |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate35() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 30, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ________________    ________________
     *  |   candidate   |   |               |
     *  |_______________|   |  focusedItem  |
     *                      |               |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate36() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *   ________________   |               |
     *  |   candidate   |   |  focusedItem  |
     *  |_______________|   |               |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate37() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 10, 20, 10)
            FocusableBox(focusedItem, 30, 0, 20, 30, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |               |
     *   ________________   |  focusedItem  |
     *  |   candidate   |   |               |
     *  |_______________|   |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate38() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 10, 20, 10)
            FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ________________
     *  |               |    ________________
     *  |   candidate   |   |  focusedItem  |
     *  |               |   |_______________|
     *  |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate39() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 30)
            FocusableBox(focusedItem, 30, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *    ________________
     *   |               |   ________________
     *   |   candidate   |  |  focusedItem  |
     *   |_______________|  |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate40() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 20)
            FocusableBox(focusedItem, 30, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Up, Down, Right -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *    ________________
     *   |   candidate   |   ________________
     *   |_______________|  |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate41() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 30, 5, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *    ________________
     *   |   candidate   |
     *   |_______________|   ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate42() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 30, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *    ________________
     *   |   candidate   |
     *   |_______________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate43() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 30, 20, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *       ________________
     *      |   candidate   |
     *      |_______________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate44() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 20, 20, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *               ________________
     *              |   candidate   |
     *              |_______________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate45() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 10, 20, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                    ___________________
     *                   |     candidate    |
     *                   |__________________|
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun nonOverlappingCandidate46() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 30, 10)
            FocusableBox(focusedItem, 10, 20, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ____________
     *                      | candidate |
     *                      |___________|____
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary1() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 10, 10)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                         ____________
     *                        | candidate |
     *                       _|___________|__
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @MediumTest
    @Test
    fun candidateWithCommonBoundary2() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 5, 0, 10, 10)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                           ____________
     *                          | candidate |
     *                       ___|___________|
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary3() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 10, 0, 10, 10)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |   candidate   |
     *                      |_______________|
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary4() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ___________________
     *                      |     candidate    |
     *                      |__________________|
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary5() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 30, 10)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                               ________________
     *                              |   candidate   |
     *                       _______|_______________|
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary6() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 10, 0, 20, 10)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                       ________________
     *                                      |   candidate   |
     *                       _______________|_______________|
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary7() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 20, 0, 20, 10)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                       ________________
     *                       _______________|   candidate   |
     *                      |  focusedItem  |_______________|
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary8() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 5, 20, 10, initialFocus)
            FocusableBox(candidate, 20, 0, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                       ________________
     *                       _______________|               |
     *                      |  focusedItem  |   candidate   |
     *                      |_______________|_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary9() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
            FocusableBox(candidate, 20, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                       ________________
     *                       _______________|               |
     *                      |  focusedItem  |   candidate   |
     *                      |_______________|               |
     *                                      |_______________|
     */
    @MediumTest
    @Test
    fun candidateWithCommonBoundary10() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 5, 20, 10, initialFocus)
            FocusableBox(candidate, 20, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________________________
     *                      |               |   candidate   |
     *                      |  focusedItem  |_______________|
     *                      |               |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary11() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
            FocusableBox(candidate, 20, 0, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |               |________________
     *                      |  focusedItem  |   candidate   |
     *                      |               |_______________|
     *                      |_______________|
     */
    @MediumTest
    @Test
    fun candidateWithCommonBoundary12() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
            FocusableBox(candidate, 20, 5, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |               |
     *                      |  focusedItem  |________________
     *                      |               |   candidate   |
     *                      |_______________|_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary13() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
            FocusableBox(candidate, 20, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________________________
     *                      |  focusedItem  |   candidate   |
     *                      |_______________|_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary14() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 20, 0, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________________________
     *                      |  focusedItem  |               |
     *                      |_______________|   candidate   |
     *                                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary15() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 20, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |________________
     *                      |_______________|   candidate   |
     *                                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary16() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 20, 5, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|________________
     *                                      |   candidate   |
     *                                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary17() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 20, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|________
     *                              |   candidate   |
     *                              |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary18() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 10, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|_____
     *                      |      candidate     |
     *                      |____________________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary19() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 0, 10, 30, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                      |   candidate   |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary20() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 0, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                        |  candidate  |
     *                        |_____________|     *
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary21() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 10, 10, 10, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                        | candidate |
     *                        |___________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary22() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 5, 10, 10, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                      | candidate |
     *                      |___________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary23() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 0, 10, 10, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                  ____|_______________|
     *                 |      candidate     |
     *                 |____________________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary24() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 10, 0, 10, 10, initialFocus)
            FocusableBox(candidate, 0, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                ______|_______________|
     *               |   candidate   |
     *               |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary25() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 10, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 0, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *       _______________|_______________|
     *      |   candidate   |
     *      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary26() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 20, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 0, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                    ________________
     *    _______________|  focusedItem  |
     *   |   candidate   |_______________|
     *   |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary27() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 5, 20, 10)
            FocusableBox(focusedItem, 20, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ________________________________
     *  |               |  focusedItem  |
     *  |   candidate   |_______________|
     *  |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary28() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 20)
            FocusableBox(focusedItem, 20, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ________________________________
     *  |   candidate   |  focusedItem  |
     *  |_______________|_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary29() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 20, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ________________________________
     *  |   candidate   |               |
     *  |_______________|  focusedItem  |
     *                  |               |
     *                  |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary30() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 20, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                   ________________
     *   _______________|               |
     *  |   candidate   |  focusedItem  |
     *  |_______________|               |
     *                  |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary31() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 5, 20, 10)
            FocusableBox(focusedItem, 20, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                   ________________
     *                  |               |
     *   _______________|  focusedItem  |
     *  |   candidate   |               |
     *  |_______________|_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary32() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 10, 20, 10)
            FocusableBox(focusedItem, 20, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ________________
     *  |               |________________
     *  |   candidate   |  focusedItem  |
     *  |               |_______________|
     *  |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary33() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 20)
            FocusableBox(focusedItem, 20, 5, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *    ________________
     *   |               |________________
     *   |   candidate   |  focusedItem  |
     *   |_______________|_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary34() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 20)
            FocusableBox(focusedItem, 20, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *    ________________
     *   |   candidate   |________________
     *   |_______________|  focusedItem  |
     *                   |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary35() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 20, 5, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *    ________________
     *   |   candidate   |
     *   |_______________|________________
     *                   |  focusedItem  |
     *                   |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary36() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 20, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *               ________________
     *              |   candidate   |
     *              |_______________|________
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary37() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 10, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                    ___________________
     *                   |     candidate    |
     *                   |__________________|
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun candidateWithCommonBoundary38() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ____________
     *                      | candidate |
     *                      |___________|____
     *                      |___________|   |
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate1() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 10, 15)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                         ____________
     *                        | candidate |
     *                       _|___________|__
     *                      | |___________| |
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @MediumTest
    @Test
    fun overlappingCandidate2() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 5, 0, 10, 15)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                           ____________
     *                          | candidate |
     *                       ___|___________|
     *                      |   |___________|
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate3() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 10, 0, 10, 15)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |   candidate   |
     *                      |_______________|
     *                      |_______________|
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate4() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 15)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ___________________
     *                      |     candidate    |
     *                      |________________  |
     *                      |_______________|__|
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate5() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 25, 15)
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                       ________________
     *                       _______________|__  candidate   |
     *                      |  focusedItem  |__|_____________|
     *                      |__________________|
     */
    @MediumTest
    @Test
    fun overlappingCandidate6() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 5, 20, 10, initialFocus)
            FocusableBox(candidate, 10, 0, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                       __________________
     *                       _______________|___              |
     *                      |  focusedItem  |  |  candidate   |
     *                      |_______________|__|______________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate7() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 10, 20, 10, initialFocus)
            FocusableBox(candidate, 10, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                                       _________________
     *                       _______________|___             |
     *                      |  focusedItem  |  | candidate   |
     *                      |_______________|__|             |
     *                                      |________________|
     */
    @MediumTest
    @Test
    fun overlappingCandidate8() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 5, 20, 10, initialFocus)
            FocusableBox(candidate, 10, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       __________________________________
     *                      |               |  |  candidate   |
     *                      |  focusedItem  |__|______________|
     *                      |                  |
     *                      |__________________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate9() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
            FocusableBox(candidate, 10, 0, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ___________________
     *                      |                __|________________
     *                      |  focusedItem  |  |   candidate   |
     *                      |               |__|_______________|
     *                      |__________________|
     */
    @MediumTest
    @Test
    fun overlappingCandidate10() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
            FocusableBox(candidate, 10, 5, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ___________________
     *                      |                  |
     *                      |  focusedItem   __|________________
     *                      |               |  |   candidate   |
     *                      |_______________|__|_______________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate11() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
            FocusableBox(candidate, 10, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ___________________________________
     *                      |  focusedItem  |  |   candidate   |
     *                      |_______________|__|_______________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate12() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 10, 0, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ___________________________________
     *                      |  focusedItem  |  |               |
     *                      |_______________|__|   candidate   |
     *                                      |__________________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate13() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 10, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ___________________
     *                      |  focusedItem   __|________________
     *                      |_______________|__|   candidate   |
     *                                      |__________________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate14() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 10, initialFocus)
            FocusableBox(candidate, 10, 5, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Right, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|_____
     *                      |_______________|    |
     *                      |      candidate     |
     *                      |____________________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate15() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 15, initialFocus)
            FocusableBox(candidate, 0, 10, 30, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |_______________|
     *                      |_______________|
     *                      |   candidate   |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate16() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 15, initialFocus)
            FocusableBox(candidate, 0, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |  _____________|
     *                      |_|_____________|
     *                        |  candidate  |
     *                        |_____________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate17() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
            FocusableBox(candidate, 10, 10, 10, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |  ____________ |
     *                      |_|___________|_|
     *                        | candidate |
     *                        |___________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate18() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 15, initialFocus)
            FocusableBox(candidate, 5, 10, 10, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                      |____________   |
     *                      |___________|___|
     *                      | candidate |
     *                      |___________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate19() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 20, 15, initialFocus)
            FocusableBox(candidate, 0, 10, 10, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                       ________________
     *                      |  focusedItem  |
     *                  ____|_______________|
     *                 |    |_______________|
     *                 |      candidate     |
     *                 |____________________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate20() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 10, 0, 10, 15, initialFocus)
            FocusableBox(candidate, 0, 10, 20, 10)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                    ___________________
     *    _______________|___  focusedItem  |
     *   |   candidate   |__|_______________|
     *   |__________________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate21() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 5, 20, 10)
            FocusableBox(focusedItem, 10, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Down -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ___________________________________
     *  |               |  |  focusedItem  |
     *  |   candidate   |__|_______________|
     *  |__________________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate22() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 20)
            FocusableBox(focusedItem, 10, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ___________________________________
     *  |   candidate   |  |  focusedItem  |
     *  |_______________|__|_______________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate23() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 10, 0, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ___________________________________
     *  |   candidate   |  |               |
     *  |_______________|__|  focusedItem  |
     *                  |                  |
     *                  |__________________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate24() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 10, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                   ___________________
     *   _______________|___               |
     *  |   candidate   |  |  focusedItem  |
     *  |_______________|__|               |
     *                  |__________________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate25() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 5, 20, 10)
            FocusableBox(focusedItem, 10, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                   ___________________
     *                  |                  |
     *   _______________|___  focusedItem  |
     *  |   candidate   |  |               |
     *  |_______________|__}_______________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate26() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 10, 20, 10)
            FocusableBox(focusedItem, 10, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *   ___________________
     *  |                __|_________________
     *  |   candidate   |  |   focusedItem  |
     *  |               |__|________________|
     *  |__________________|
     */
    @MediumTest
    @Test
    fun overlappingCandidate27() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 20)
            FocusableBox(focusedItem, 10, 5, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *    ___________________
     *   |                __|_________________
     *   |   candidate   |  |   focusedItem  |
     *   |_______________|__|________________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate28() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 20)
            FocusableBox(focusedItem, 10, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Up, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *    ___________________
     *   |   candidate    __|________________
     *   |_______________|__|  focusedItem  |
     *                   |__________________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate29() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 10, 5, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Left, Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                    ___________________
     *                   |     candidate    |
     *                   |   _______________|
     *                   |__|_______________|
     *                      |  focusedItem  |
     *                      |_______________|
     */
    @LargeTest
    @Test
    fun overlappingCandidate30() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 20, 10)
            FocusableBox(focusedItem, 10, 5, 10, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            when (focusDirection) {
                Up -> {
                    assertThat(focusedItem.value).isFalse()
                    assertThat(candidate.value).isTrue()
                }
                Left, Right, Down -> {
                    assertThat(focusedItem.value).isTrue()
                    assertThat(candidate.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *        ------------------------------
     *       |  candidate  |               |
     *       |_____________|               |
     *       |               focusedItem   |
     *       |_____________________________|
     */
    @LargeTest
    @Test
    fun candidateWithinBoundsOfFocusedItem1() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 0, 10, 10)
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |       |  candidate  |       |
     *       |       |_____________|       |
     *       |         focusedItem         |
     *       |_____________________________|
     */
    @LargeTest
    @Test
    fun candidateWithinBoundsOfFocusedItem2() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 5, 0, 10, 10)
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |             |   candidate   |
     *       |             |_______________|
     *       |  focusedItem                |
     *       |_____________________________|
     */
    @LargeTest
    @Test
    fun candidateWithinBoundsOfFocusedItem3() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 10, 0, 10, 10)
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |               ______________|
     *       | focusedItem  |   candidate  |
     *       |              |______________|
     *       |_____________________________|
     */
    @LargeTest
    @Test
    fun candidateWithinBoundsOfFocusedItem4() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 10, 5, 10, 10)
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |  focusedItem                |
     *       |               ______________|
     *       |              |   candidate  |
     *       |______________|______________|
     */
    @LargeTest
    @Test
    fun candidateWithinBoundsOfFocusedItem5() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 10, 10, 10, 10)
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |          focusedItem        |
     *       |        _______________      |
     *       |       |   candidate  |      |
     *       |_______|______________|______|
     */
    @LargeTest
    @Test
    fun candidateWithinBoundsOfFocusedItem6() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 5, 10, 10, 10)
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |                focusedItem  |
     *       |______________               |
     *       |  candidate  |               |
     *       |_____________|_______________|
     */
    @LargeTest
    @Test
    fun candidateWithinBoundsOfFocusedItem7() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 10, 10, 10)
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |______________               |
     *       |  candidate  |  focusedItem  |
     *       |_____________|               |
     *       |_____________________________|
     */
    @LargeTest
    @Test
    fun candidateWithinBoundsOfFocusedItem8() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 0, 5, 10, 10)
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |  focusedItem                |
     *       |       ______________        |
     *       |      |  candidate  |        |
     *       |      |_____________|        |
     *       |_____________________________|
     */
    @LargeTest
    @Test
    fun candidateWithinBoundsOfFocusedItem9() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 5, 5, 10, 10)
            FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |  focusedItem  |             |
     *       |_______________|             |
     *       |                 candidate   |
     *       |_____________________________|
     */
    @LargeTest
    @Test
    fun focusedItemWithinBoundsOfCandidate1() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 10, 10, initialFocus)
            FocusableBox(candidate, 0, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |      |  focusedItem  |      |
     *       |      |_______________|      |
     *       |          candidate          |
     *       |_____________________________|
     */
    @LargeTest
    @Test
    fun focusedItemWithinBoundsOfCandidate2() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 5, 0, 10, 10, initialFocus)
            FocusableBox(candidate, 0, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |             |  focusedItem  |
     *       |             |_______________|
     *       |  candidate                  |
     *       |_____________________________|
     */
    @MediumTest
    @Test
    fun focusedItemWithinBoundsOfCandidate3() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 10, 0, 10, 10, initialFocus)
            FocusableBox(candidate, 0, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |              _______________|
     *       |  candidate  |  focusedItem  |
     *       |             |_______________|
     *       |_____________________________|
     */
    @LargeTest
    @Test
    fun focusedItemWithinBoundsOfCandidate4() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 10, 5, 10, 10, initialFocus)
            FocusableBox(candidate, 0, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |   candidate                 |
     *       |             ________________|
     *       |            |   focusedItem  |
     *       |____________|________________|
     */
    @LargeTest
    @Test
    fun focusedItemWithinBoundsOfCandidate5() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
            FocusableBox(candidate, 0, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |           candidate         |
     *       |        _______________      |
     *       |       |  focusedItem |      |
     *       |_______|______________|______|
     */
    @LargeTest
    @Test
    fun focusedItemWithinBoundsOfCandidate6() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 5, 10, 10, 10, initialFocus)
            FocusableBox(candidate, 0, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |                 candidate   |
     *       |_______________              |
     *       |  focusedItem |              |
     *       |______________|______________|
     */
    @LargeTest
    @Test
    fun focusedItemWithinBoundsOfCandidate7() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 10, 10, 10, initialFocus)
            FocusableBox(candidate, 0, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |________________             |
     *       |  focusedItem  |  candidate  |
     *       |_______________|             |
     *       |_____________________________|
     */
    @LargeTest
    @Test
    fun focusedItemWithinBoundsOfCandidate8() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 5, 10, 10, initialFocus)
            FocusableBox(candidate, 0, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *        ------------------------------
     *       |   candidate                 |
     *       |       _______________       |
     *       |      |  focusedItem |       |
     *       |      |______________|       |
     *       |_____________________________|
     */
    @MediumTest
    @Test
    fun focusedItemWithinBoundsOfCandidate9() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 5, 5, 10, 10, initialFocus)
            FocusableBox(candidate, 0, 0, 20, 20)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    /**
     *                        ______________________   *        _________________
     *    _________________  |            ________ |   *       |  next sibling  |           ^
     *   |  next sibling  |  |  current  | child | |   *       |________________|           |
     *   |________________|  |  focus    |_______| |   *      ______________________    Direction
     *                       |_____________________|   *     |            ________ |    of Search
     *                                                 *     |  current  | child | |        |
     *          <---- Direction of Search ---          *     |  focus    |_______| |        |
     *                                                 *     |_____________________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *    ______________________                       *      ______________________
     *   |            ________ |   _________________   *     |            ________ |        |
     *   |  current  | child | |  |  next sibling  |   *     |  current  | child | |        |
     *   |  focus    |_______| |  |________________|   *     |  focus    |_______| |    Direction
     *   |_____________________|                       *     |_____________________|    of Search
     *                                                 *        _________________           |
     *          ---- Direction of Search --->          *       |  next sibling  |           |
     *                                                 *       |________________|           v
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @MediumTest
    @Test
    fun picksSiblingAndNotChild() {
        // Arrange.
        val (focusedItem, child, nextSibling) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(nextSibling, 0, 10, 10, 10)
                    FocusableBox(focusedItem, 20, 0, 30, 30, initialFocus) {
                        FocusableBox(child, 10, 10, 10, 10)
                    }
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 30, 30, initialFocus) {
                        FocusableBox(child, 10, 10, 10, 10)
                    }
                    FocusableBox(nextSibling, 40, 10, 10, 10)
                }
                Up -> {
                    FocusableBox(nextSibling, 10, 0, 10, 10)
                    FocusableBox(focusedItem, 0, 20, 30, 30, initialFocus) {
                        FocusableBox(child, 10, 10, 10, 10)
                    }
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 30, 30, initialFocus) {
                        FocusableBox(child, 10, 10, 10, 10)
                    }
                    FocusableBox(nextSibling, 10, 40, 10, 10)
                }
                else -> error(invalid)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isFalse()
            assertThat(child.value).isFalse()
            assertThat(nextSibling.value).isTrue()
        }
    }

    @MediumTest
    @Test
    fun sameBoundsForFocusedItemAndCandidate() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(candidate, 10, 10, 20, 10)
            FocusableBox(focusedItem, 10, 10, 20, 10, initialFocus)
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(focusDirection)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusedItem.value).isTrue()
            assertThat(candidate.value).isFalse()
        }
    }

    private fun ComposeContentTestRule.setContentForTest(composable: @Composable () -> Unit) {
        setContent {
            focusManager = LocalFocusManager.current
            composable()
        }
        rule.runOnIdle { initialFocus.requestFocus() }
    }
}
