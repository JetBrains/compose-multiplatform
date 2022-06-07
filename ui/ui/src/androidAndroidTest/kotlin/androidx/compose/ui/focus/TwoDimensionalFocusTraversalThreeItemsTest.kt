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
class TwoDimensionalFocusTraversalThreeItemsTest(param: Param) {
    @get:Rule
    val rule = createComposeRule()

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(val focusDirection: FocusDirection) {
        override fun toString() = focusDirection.toString()
    }

    private lateinit var focusManager: FocusManager
    private val initialFocus: FocusRequester = FocusRequester()
    private val focusDirection = param.focusDirection

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(Param(Left), Param(Right), Param(Up), Param(Down))
    }

    /**
     *   __________                    __________       *                            __________
     *  |   Next  |                   |  Closer |       *              ^            |   Next  |
     *  |   Item  |                   |   Item  |       *              |            |   Item  |
     *  |_________|                   |_________|       *          Direction        |_________|
     *                        ____________              *          of Search
     *                       |  focused  |              *              |
     *                       |    Item   |              *              |
     *                       |___________|              *         ____________
     *                                                  *        |  focused  |       __________
     *                                                  *        |    Item   |      |  Closer |
     *          <---- Direction of Search ---           *        |___________|      |  Item   |
     *                                                  *                           |_________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *   __________                    _________        *                            __________
     *  |  Closer |                   |  Next  |        *                           |  Closer |
     *  |   Item  |                   |  Item  |        *         ____________      |   Item  |
     *  |_________|                   |________|        *        |  focused  |      |_________|
     *           ____________                           *        |    Item   |
     *          |  focused  |                           *        |___________|
     *          |    Item   |                           *
     *          |___________|                           *              |              _________
     *                                                  *          Direction         |  Next  |
     *          ---- Direction of Search --->           *          of Search         |  Item  |
     *                                                  *              |             |________|
     *                                                  *              V
     */
    @MediumTest
    @Test
    fun validItemIsPickedEvenThoughThereIsACloserItem1() {
        // Arrange.
        val (focusedItem, closerItem, nextItem) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 30, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 40, 0, 20, 20)
                    FocusableBox(nextItem, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 40, 20, 20)
                    FocusableBox(nextItem, 30, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 10, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 0, 20, 20)
                    FocusableBox(nextItem, 40, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 10, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(nextItem, 30, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(nextItem.value).isTrue()
        }
    }

    /**
     *                                                  *        __________
     *                                                  *       |   Next  |          ^
     *                        ____________              *       |   Item  |          |
     *                       |  focused  |              *       |_________|       Direction
     *                       |    Item   |              *                         of Search
     *                       |___________|              *                            |
     *   _________                     __________       *                            |
     *  |  Next  |                    |  Closer |       *                       ____________
     *  |  Item  |                    |   Item  |       *        __________    |  focused  |
     *  |________|                    |_________|       *       |  Closer |    |    Item   |
     *          <---- Direction of Search ---           *       |  Item   |    |___________|
     *                                                  *       |_________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *           ____________                           *         __________
     *          |  focused  |                           *        |  Closer |
     *          |    Item   |                           *        |   Item  |     ____________
     *          |___________|                           *        |_________|    |  focused  |
     *    __________                    _________       *                       |    Item   |
     *   |  Closer |                   |  Next  |       *                       |___________|
     *   |   Item  |                   |  Item  |       *
     *   |_________|                   |________|       *          _________          |
     *                                                  *         |  Next  |      Direction
     *          ---- Direction of Search --->           *         |  Item  |      of Search
     *                                                  *         |________|          |
     *                                                  *                             V
     */
    @LargeTest
    @Test
    fun validItemIsPickedEvenThoughThereIsACloserItem2() {
        // Arrange.
        val (focusedItem, closerItem, nextItem) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 40, 30, 20, 20)
                    FocusableBox(nextItem, 0, 30, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 40, 20, 20)
                    FocusableBox(nextItem, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 10, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(nextItem, 40, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 10, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 0, 20, 20)
                    FocusableBox(nextItem, 0, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(nextItem.value).isTrue()
        }
    }

    /**
     *    _________                                     *   _________
     *   |  Next  |                                     *  |  Next  |     ^
     *   |  Item  |                                     *  |  Item  |     |
     *   |________|                                     *  |________|  Direction
     *                        ____________              *             of Search
     *                       |  focused  |              *                 |
     *                       |    Item   |              *                 |
     *                       |___________|              *          ____________
     *                               __________         *         |  focused  |
     *                              |  Closer |         *         |    Item   |      __________
     *                              |   Item  |         *         |___________|     |  Closer |
     *                              |_________|         *                           |   Item  |
     *          <---- Direction of Search ---           *                           |_________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *   __________                                     *                            __________
     *  |  Closer |                                     *                           |  Closer |
     *  |   Item  |                                     *           ____________    |   Item  |
     *  |_________|                                     *          |  focused  |    |_________|
     *          ____________                            *          |    Item   |
     *         |  focused  |                            *          |___________|
     *         |    Item   |                            *                 |
     *         |___________|                            *   _________  Direction
     *                                 _________        *  |  Next  |  of Search
     *                                |  Next  |        *  |  Item  |     |
     *                                |  Item  |        *  |________|     |
     *                                |________|        *                 V
     *          ---- Direction of Search --->           *
     *                                                  *
     *                                                  *
     */
    @LargeTest
    @Test
    fun validItemIsPickedEvenThoughThereIsACloserItem3() {
        // Arrange.
        val (focusedItem, closerItem, nextItem) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 30, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 40, 60, 20, 20)
                    FocusableBox(nextItem, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 10, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 40, 40, 20, 20)
                    FocusableBox(nextItem, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 10, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 0, 20, 20)
                    FocusableBox(nextItem, 40, 60, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 10, 10, 20, 20, initialFocus)
                    FocusableBox(closerItem, 40, 0, 20, 20)
                    FocusableBox(nextItem, 30, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(nextItem.value).isTrue()
        }
    }

    /**
     *                                __________        *                               _________
     *                               |  Closer |        *                     ^        |  Next  |
     *                               |   Item  |        *                     |        |  Item  |
     *                               |_________|        *                  Direction   |________|
     *                        ____________              *                  of Search
     *                       |  focused  |              *                     |
     *                       |    Item   |              *                     |
     *                       |___________|              *                ____________
     *      _________                                   *               |  focused  |
     *     |  Next  |                                   *   __________  |    Item   |
     *     |  Item  |                                   *  |  Closer |  |___________|
     *     |________|                                   *  |   Item  |
     *          <---- Direction of Search ---           *  |_________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                _________         *    __________
     *                               |  Next  |         *   |  Closer |
     *                               |  Item  |         *   |   Item  |   ____________
     *                               |________|         *   |_________|  |  focused  |
     *           ____________                           *                |    Item   |
     *          |  focused  |                           *                |___________|
     *          |    Item   |                           *                      |
     *          |___________|                           *                  Direction    _________
     *   __________                                     *                  of Search   |  Next  |
     *  |  Closer |                                     *                      |       |  Item  |
     *  |   Item  |                                     *                      |       |________|
     *  |_________|                                     *                      V
     *          ---- Direction of Search --->           *
     *                                                  *
     *                                                  *
     */
    @LargeTest
    @Test
    fun validItemIsPickedEvenThoughThereIsACloserItem4() {
        // Arrange.
        val (focusedItem, closerItem, nextItem) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 30, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 40, 0, 20, 20)
                    FocusableBox(nextItem, 0, 60, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 40, 20, 20)
                    FocusableBox(nextItem, 60, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 10, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 60, 20, 20)
                    FocusableBox(nextItem, 40, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 10, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 0, 20, 20)
                    FocusableBox(nextItem, 60, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(nextItem.value).isTrue()
        }
    }

    /**
     *                  __________                      *         ____________          ^
     *                 |  Closer |                      *        |    Item   |          |
     *                 |   Item  |                      *        |  in beam  |      Direction
     *                 |_________|                      *        |___________|     of Search
     *     __________         ____________              *                               |
     *    |  Item   |        |  focused  |              *                               |
     *    | in beam |        |    Item   |              *                            __________
     *    |_________|        |___________|              *         ____________      |  Closer |
     *                                                  *        |  focused  |      |  Item   |
     *                                                  *        |    Item   |      |_________|
     *          <---- Direction of Search ---           *        |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *         ____________
     *                |  Closer |                       *        |  focused  |
     *                |   Item  |                       *        |    Item   |       __________
     *                |_________|                       *        |___________|      |  Closer |
     *         ____________         __________          *                           |   Item  |
     *        |  focused  |        |  Item   |          *                           |_________|
     *        |    Item   |        | in beam |          *
     *        |___________|        |_________|          *         ____________
     *                                                  *        |    Item   |          |
     *          ---- Direction of Search --->           *        |  in beam  |      Direction
     *                                                  *        |___________|      of Search
     *                                                  *                               |
     *                                                  *                               V
     */
    @MediumTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis1() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 20)
                    FocusableBox(itemInBeam, 0, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                      ___________                 *              _________        ^
     *                     |  Closer  |                 *             |  Item  |        |
     *                     |   Item   |                 *             | in beam|     Direction
     *                     |__________|                 *             |________|     of Search
     *   _______________                                *                               |
     *  | Item in Beam |         ____________           *                               |
     *  |______________|        |  focused  |           *                            __________
     *                          |    Item   |           *        ____________       |  Closer |
     *                          |___________|           *       |  focused  |       |  Item   |
     *                                                  *       |    Item   |       |_________|
     *          <---- Direction of Search ---           *       |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *        ____________
     *                |  Closer |                       *       |  focused  |
     *                |   Item  |                       *       |    Item   |        __________
     *                |_________|                       *       |___________|       |  Closer |
     *                              _______________     *                           |   Item  |
     *         ____________        | Item in Beam |     *                           |_________|
     *        |  focused  |        |______________|     *
     *        |    Item   |                             *              _________
     *        |___________|                             *             |  Item  |        |
     *                                                  *             | in beam|    Direction
     *          ---- Direction of Search --->           *             |________|    of Search
     *                                                  *                               |
     *                                                  *                               V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis2() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 20, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 40, 20, 20)
                    FocusableBox(itemInBeam, 60, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 10, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 60, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 10, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 0, 20, 20)
                    FocusableBox(itemInBeam, 60, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                       ___________                *            _________          ^
     *                      |  Closer  |                *           |  Item  |          |
     *                      |   Item   |                *           | in beam|       Direction
     *                      |__________|                *           |________|       of Search
     *   _______________         ____________           *                               |
     *  | Item in Beam |        |  focused  |           *                               |
     *  |______________|        |    Item   |           *                            __________
     *                          |___________|           *         ____________      |  Closer |
     *                                                  *        |  focused  |      |  Item   |
     *                                                  *        |    Item   |      |_________|
     *          <---- Direction of Search ---           *        |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *         ____________
     *                |  Closer |                       *        |  focused  |
     *                |   Item  |                       *        |    Item   |       __________
     *                |_________|                       *        |___________|      |  Closer |
     *         ____________          _______________    *                           |   Item  |
     *        |  focused  |         | Item in Beam |    *                           |_________|
     *        |    Item   |         |______________|    *
     *        |___________|                             *            _________
     *                                                  *           |  Item  |          |
     *          ---- Direction of Search --->           *           | in beam|      Direction
     *                                                  *           |________|      of Search
     *                                                  *                               |
     *                                                  *                               V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis3() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 10, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 30, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 20)
                    FocusableBox(itemInBeam, 10, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                          __________              *           _______             ^
     *                         |  Closer |              *          | Item |             |
     *                         |   Item  |              *          |  in  |          Direction
     *                         |_________|              *          | Beam |          of Search
     *                               ____________       *          |______|             |
     *      _______________         |           |       *                               |
     *     | Item in Beam |         |  focused  |       *                            __________
     *     |______________|         |    Item   |       *         ____________      |  Closer |
     *                              |___________|       *        |  focused  |      |  Item   |
     *                                                  *        |    Item   |      |_________|
     *          <---- Direction of Search ---           *        |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *         ____________
     *                |  Closer |                       *        |  focused  |
     *                |   Item  |                       *        |    Item   |       __________
     *                |_________|                       *        |___________|      |  Closer |
     *         ____________                             *                           |   Item  |
     *        |           |           _______________   *                           |_________|
     *        |  focused  |          | Item in Beam |   *
     *        |    Item   |          |______________|   *            _______
     *        |___________|                             *           | Item |            |
     *                                                  *           |  in  |        Direction
     *                                                  *           | Beam |        of Search
     *         ---- Direction of Search --->            *           |______|            |
     *                                                  *                               V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis4() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 30, 20, 30, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 40, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 30, 20, initialFocus)
                    FocusableBox(closerItem, 40, 30, 20, 20)
                    FocusableBox(itemInBeam, 10, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 30, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 40, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 40, 10, 20, 20)
                    FocusableBox(itemInBeam, 10, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                        __________                *         _________             ^
     *                       |  Closer |                *        |        |             |
     *                       |   Item  |                *        |  Item  |          Direction
     *                       |_________|                *        | in beam|          of Search
     *                           ____________           *        |________|             |
     *    _______________       |  focused  |           *                               |
     *   | Item in Beam |       |    Item   |           *                            __________
     *   |______________|       |___________|           *         ______________    |  Closer |
     *                                                  *        |   focused   |    |  Item   |
     *                                                  *        |    Item     |    |_________|
     *          <---- Direction of Search ---           *        |_____________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *         _____________
     *                |  Closer |                       *        |   focused  |
     *                |   Item  |                       *        |    Item    |       __________
     *                |_________|                       *        |____________|      |  Closer |
     *         ____________                             *                            |   Item  |
     *        |  focused  |         _______________     *                            |_________|
     *        |    Item   |        | Item in Beam |     *
     *        |___________|        |______________|     *         _________
     *                                                  *        |        |             |
     *          ---- Direction of Search --->           *        |  Item  |         Direction
     *                                                  *        | in beam|         of Search
     *                                                  *        |        |             |
     *                                                  *        |________|             V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis5() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 40, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 40, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 20)
                    FocusableBox(itemInBeam, 0, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                         __________               *                               ^
     *                        |  Closer |               *      _________                |
     *                        |   Item  |               *     |  Item  |             Direction
     *                        |_________|               *     | in beam|             of Search
     *                             ____________         *     |________|                |
     *                            |  focused  |         *                               |
     *    _______________         |    Item   |         *                           __________
     *   |              |         |___________|         *                          |  Closer |
     *   | Item in Beam |                               *          ____________    |  Item   |
     *   |______________|                               *         |  focused  |    |_________|
     *                                                  *         |    Item   |
     *          <---- Direction of Search ---           *         |___________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *          ____________
     *                |  Closer |                       *         |  focused  |
     *                |   Item  |                       *         |    Item   |      __________
     *                |_________|                       *         |___________|     |  Closer |
     *         ____________                             *                           |   Item  |
     *        |  focused  |                             *                           |_________|
     *        |    Item   |         _______________     *
     *        |___________|        |              |     *      _________
     *                             | Item in Beam |     *     |  Item   |                |
     *                             |______________|     *     | in beam |            Direction
     *                                                  *     |________ |            of Search
     *         ---- Direction of Search --->            *                                |
     *                                                  *                                V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis6() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 40, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 10, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 40, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 40, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 10, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 40, 10, 20, 20)
                    FocusableBox(itemInBeam, 0, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                  __________                      *         ____________            ^
     *                 |  Closer |                      *        |    Item   |            |
     *                 |   Item  |                      *        |  in beam  |        Direction
     *     __________  |_________|________              *        |___________|       of Search
     *    |  Item   |        |  focused  |              *                                 |
     *    | in beam |        |    Item   |              *                     _________   |
     *    |_________|        |___________|              *         ___________|  Closer |
     *                                                  *        |  focused  |  Item   |
     *                                                  *        |    Item   |_________|
     *          <---- Direction of Search ---           *        |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *         ____________
     *                |  Closer |                       *        |  focused  |
     *                |   Item  |                       *        |    Item   |__________
     *         _______|_________|   __________          *        |___________|  Closer |
     *        |  focused  |        |  Item   |          *                    |   Item  |
     *        |    Item   |        | in beam |          *                    |_________|
     *        |___________|        |_________|          *
     *                                                  *         ____________
     *          ---- Direction of Search --->           *        |    Item   |          |
     *                                                  *        |  in beam  |      Direction
     *                                                  *        |___________|      of Search
     *                                                  *                               |
     *                                                  *                               V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis7() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 20, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 20, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 20, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 20, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 10, 20, 20)
                    FocusableBox(itemInBeam, 0, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *              _________        ^
     *                        ___________               *             |  Item  |        |
     *                       |  Closer  |               *             | in beam|     Direction
     *   _______________     |   Item   |               *             |________|     of Search
     *  | Item in Beam |     |__________|________       *                               |
     *  |______________|            |  focused  |       *                               |
     *                              |    Item   |       *                    __________
     *                              |___________|       *        ___________|  Closer |
     *                                                  *       |  focused  |  Item   |
     *                                                  *       |    Item   |_________|
     *          <---- Direction of Search ---           *       |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *            __________                            *        ____________
     *           |  Closer |                            *       |  focused  |
     *           |   Item  |        _______________     *       |    Item   |__________
     *    _______|_________|       | Item in Beam |     *       |___________|  Closer |
     *   |  focused  |             |______________|     *                   |   Item  |
     *   |    Item   |                                  *                   |_________|
     *   |___________|                                  *
     *                                                  *              _________
     *                                                  *             |  Item  |        |
     *          ---- Direction of Search --->           *             | in beam|    Direction
     *                                                  *             |________|    of Search
     *                                                  *                               |
     *                                                  *                               V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis8() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 20, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 10, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 30, 20, 20)
                    FocusableBox(itemInBeam, 10, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 20, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 10, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 10, 20, 20)
                    FocusableBox(itemInBeam, 10, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                      ___________                 *            _________          ^
     *                     |  Closer  |                 *           |        |          |
     *                     |   Item   |                 *           |  Item  |       Direction
     *   _______________   |__________|________         *           | in beam|       of Search
     *  | Item in Beam |          |  focused  |         *           |________|          |
     *  |______________|          |    Item   |         *                               |
     *                            |___________|         *                     __________
     *                                                  *         ___________|  Closer |
     *                                                  *        |  focused  |  Item   |
     *          <---- Direction of Search ---           *        |    Item   |_________|
     *                                                  *        |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *         ____________
     *                |  Closer |                       *        |  focused  |
     *                |   Item  |                       *        |    Item   |__________
     *         _______|_________|    _______________    *        |___________|  Closer |
     *        |  focused  |         | Item in Beam |    *                    |   Item  |
     *        |    Item   |         |______________|    *                    |_________|
     *        |___________|                             *
     *                                                  *            _________
     *                                                  *           |        |          |
     *          ---- Direction of Search --->           *           |  Item  |      Direction
     *                                                  *           | in beam|      of Search
     *                                                  *           |________|          |
     *                                                  *                               V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis9() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 20, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 20, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 30, 20, 20)
                    FocusableBox(itemInBeam, 10, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 20, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 20, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 10, 20, 20)
                    FocusableBox(itemInBeam, 10, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                            __________            *           _______             ^
     *                           |  Closer |            *          | Item |             |
     *                           |   Item  |            *          |  in  |          Direction
     *                           |_________|__________  *          | Beam |          of Search
     *      _______________              |           |  *          |______|             |
     *     | Item in Beam |              |  focused  |  *                               |
     *     |______________|              |    Item   |  *                     __________
     *                                   |___________|  *        ____________|  Closer |
     *                                                  *       |  focused   |  Item   |
     *          <---- Direction of Search ---           *       |    Item    |_________|
     *                                                  *       |____________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *        _____________
     *                |  Closer |                       *       |   focused  |
     *                |   Item  |                       *       |     Item   |__________
     *         _______|_________|                       *       |____________|  Closer |
     *        |           |           _______________   *                    |   Item  |
     *        |  focused  |          | Item in Beam |   *                    |_________|
     *        |    Item   |          |______________|   *
     *        |___________|                             *           _______
     *                                                  *          | Item |            |
     *                                                  *          |  in  |        Direction
     *         ---- Direction of Search --->            *          | Beam |        of Search
     *                                                  *          |______|            |
     *                                                  *                              V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis10() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 20, 20, 30, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 30, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 10, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 20, 20, 30, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 30, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 20)
                    FocusableBox(itemInBeam, 10, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                        __________                *         _________             ^
     *                       |  Closer |                *        |        |             |
     *                       |   Item  |                *        |  Item  |          Direction
     *                       |_________|______          *        | in beam|          of Search
     *    _______________        |  focused  |          *        |________|             |
     *   | Item in Beam |        |    Item   |          *                               |
     *   |______________|        |___________|          *                      __________
     *                                                  *         ____________|  Closer |
     *                                                  *        |   focused  |  Item   |
     *                                                  *        |    Item    |_________|
     *          <---- Direction of Search ---           *        |____________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                  *         _____________
     *                 __________                       *        |   focused  |
     *                |  Closer |                       *        |    Item    |__________
     *                |   Item  |                       *        |____________|  Closer |
     *         _______|_________|                       *                     |   Item  |
     *        |  focused  |         _______________     *                     |_________|
     *        |    Item   |        | Item in Beam |     *
     *        |___________|        |______________|     *         _________
     *                                                  *        |        |             |
     *          ---- Direction of Search --->           *        |  Item  |         Direction
     *                                                  *        | in beam|         of Search
     *                                                  *        |________|             |
     *                                                  *                               V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis11() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 20, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 20, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 30, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 10, 20, 20)
                    FocusableBox(itemInBeam, 0, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                          __________              *    _________                  ^
     *                         |  Closer |              *   |  Item  |                  |
     *                         |   Item  |              *   | in beam|               Direction
     *                         |_________|_________     *   |________|               of Search
     *                                |  focused  |     *                               |
     *    _______________             |    Item   |     *                               |
     *   | Item in Beam |             |___________|     *                     __________
     *   |______________|                               *         ___________|  Closer |
     *                                                  *        |  focused  |  Item   |
     *          <---- Direction of Search ---           *        |    Item   |_________|
     *                                                  *        |___________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *            __________                            *         ____________
     *           |  Closer |                            *        |  focused  |
     *           |   Item  |                            *        |    Item   |__________
     *    _______|_________|                            *        |___________|  Closer |
     *   |  focused  |                                  *                    |   Item  |
     *   |    Item   |              _______________     *                    |_________|
     *   |___________|             | Item in Beam |     *
     *                             |______________|     *    _________
     *                                                  *   |  Item  |                  |
     *         ---- Direction of Search --->            *   | in beam|              Direction
     *                                                  *   |________|              of Search
     *                                                  *                               |
     *                                                  *                               V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis12() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 20, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 10, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 20, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 10, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 20)
                    FocusableBox(itemInBeam, 0, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *     __________         ____________              *          ^           ____________
     *    |  Item   |        |  focused  |              *          |          |    Item   |
     *    | in beam |        |    Item   |              *      Direction      |  in beam  |
     *    |_________|        |___________|              *     of Search       |___________|
     *                   __________                     *          |
     *                  |  Closer |                     *          |
     *                  |   Item  |                     *       __________
     *                  |_________|                     *      |  Closer |     ____________
     *                                                  *      |  Item   |    |  focused  |
     *                                                  *      |_________|    |    Item   |
     *          <---- Direction of Search ---           *                     |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                  *                      ____________
     *         ____________            __________       *                     |  focused  |
     *        |  focused  |           |  Item   |       *       __________    |    Item   |
     *        |    Item   |           | in beam |       *      |  Closer |    |___________|
     *        |___________|           |_________|       *      |   Item  |
     *                 __________                       *      |_________|
     *                |  Closer |                       *
     *                |   Item  |                       *                      ____________
     *                |_________|                       *          |          |    Item   |
     *                                                  *      Direction      |  in beam  |
     *        ---- Direction of Search --->             *      of Search      |___________|
     *                                                  *          |
     *                                                  *          V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis13() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 30, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 30, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *   _______________                                *         ^                 _________
     *  | Item in Beam |          ____________          *         |                |  Item  |
     *  |______________|         |  focused  |          *      Direction           | in beam|
     *                           |    Item   |          *      of Search           |________|
     *                           |___________|          *         |
     *                     ___________                  *         |
     *                    |  Closer  |                  *      __________
     *                    |   Item   |                  *     |  Closer |     ____________
     *                    |__________|                  *     |  Item   |    |  focused  |
     *                                                  *     |_________|    |    Item   |
     *          <---- Direction of Search ---           *                    |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                _______________   *                     ____________
     *         ____________          | Item in Beam |   *                    |  focused  |
     *        |  focused  |          |______________|   *     __________     |    Item   |
     *        |    Item   |                             *    |  Closer |     |___________|
     *        |___________|                             *    |   Item  |
     *                  __________                      *    |_________|
     *                 |  Closer |                      *
     *                 |   Item  |                      *                           _________
     *                 |_________|                      *        |                 |  Item  |
     *                                                  *    Direction             | in beam|
     *         ---- Direction of Search --->            *    of Search             |________|
     *                                                  *        |
     *                                                  *        V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis14() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 10, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 40, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 10, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 40, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 40, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *         ^             _________
     *   _______________              ____________      *         |            |  Item  |
     *  | Item in Beam |             |  focused  |      *     Direction        | in beam|
     *  |______________|             |    Item   |      *     of Search        |________|
     *                               |___________|      *         |
     *                         ___________              *         |
     *                        |  Closer  |              *     __________
     *                        |   Item   |              *    |  Closer |     ____________
     *                        |__________|              *    |  Item   |    |  focused  |
     *                                                  *    |_________|    |    Item   |
     *          <---- Direction of Search ---           *                   |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                  *                    ____________
     *     ____________             _______________     *                   |  focused  |
     *    |  focused  |            | Item in Beam |     *     __________    |    Item   |
     *    |    Item   |            |______________|     *    |  Closer |    |___________|
     *    |___________|                                 *    |   Item  |
     *             __________                           *    |_________|
     *            |  Closer |                           *
     *            |   Item  |                           *                       _________
     *            |_________|                           *         |            |  Item  |
     *                                                  *     Direction        | in beam|
     *         ---- Direction of Search --->            *     of Search        |________|
     *                                                  *         |
     *                                                  *         V
     *
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis15() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 30, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 40, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                  ____________    *       ^               _______
     *   _______________               |           |    *       |              | Item |
     *  | Item in Beam |               |  focused  |    *    Direction         |  in  |
     *  |______________|               |    Item   |    *    of Search         | Beam |
     *                                 |___________|    *       |              |______|
     *                            __________            *       |
     *                           |  Closer |            *    __________
     *                           |   Item  |            *   |  Closer |       ____________
     *                           |_________|            *   |  Item   |      |  focused  |
     *                                                  *   |_________|      |    Item   |
     *          <---- Direction of Search ---           *                    |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *      ____________                                *                    ____________
     *     |           |              _______________   *                   |  focused  |
     *     |  focused  |             | Item in Beam |   *     __________    |    Item   |
     *     |    Item   |             |______________|   *    |  Closer |    |___________|
     *     |___________|                                *    |   Item  |
     *              __________                          *    |_________|
     *             |  Closer |                          *
     *             |   Item  |                          *                       _______
     *             |_________|                          *        |             | Item |
     *                                                  *    Direction         |  in  |
     *           ---- Direction of Search --->          *    of Search         | Beam |
     *                                                  *        |             |______|
     *                                                  *        V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis16() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 30, 40, 20, 20)
                    FocusableBox(itemInBeam, 0, 10, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 40, 30, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 10, 40, 20, 20)
                    FocusableBox(itemInBeam, 40, 10, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 40, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *           ^             _________
     *                                 ____________     *           |            |  Item  |
     *    _______________             |  focused  |     *        Direction       | in beam|
     *   | Item in Beam |             |    Item   |     *        of Search       |________|
     *   |______________|             |___________|     *           |
     *                           __________             *           |
     *                          |  Closer |             *        __________
     *                          |   Item  |             *       |  Closer |       _____________
     *                          |_________|             *       |  Item   |      |  focused   |
     *                                                  *       |_________|      |    Item    |
     *          <---- Direction of Search ---           *                        |____________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *       ____________                               *                          _____________
     *      |  focused  |           _______________     *                         |  focused   |
     *      |    Item   |          | Item in Beam |     *        __________       |    Item    |
     *      |___________|          |______________|     *       |  Closer |       |____________|
     *             __________                           *       |   Item  |
     *            |  Closer |                           *       |_________|
     *            |   Item  |                           *
     *            |_________|                           *                          _________
     *                                                  *           |             |  Item  |
     *        ---- Direction of Search --->             *       Direction         | in beam|
     *                                                  *       of Search         |________|
     *                                                  *           |
     *                                                  *           V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis17() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 10, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 40, 30, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 30, 20, 20)
                    FocusableBox(itemInBeam, 40, 10, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 30, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                            ____________          *           ^          _________
     *                           |  focused  |          *           |         |  Item  |
     *   _______________         |    Item   |          *        Direction    | in beam|
     *  |              |         |___________|          *        of Search    |________|
     *  | Item in Beam |                                *           |
     *  |______________|                                *           |
     *                       __________                 *       __________
     *                      |  Closer |                 *      |  Closer |          ____________
     *                      |   Item  |                 *      |  Item   |         |  focused  |
     *                      |_________|                 *      |_________|         |    Item   |
     *          <---- Direction of Search ---           *                          |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *        ____________                              *                           ____________
     *       |  focused  |                              *                          |  focused  |
     *       |    Item   |         _______________      *       __________         |    Item   |
     *       |___________|        |              |      *      |  Closer |         |___________|
     *                            | Item in Beam |      *      |   Item  |
     *                            |______________|      *      |_________|
     *                                                  *
     *              __________                          *           |          __________
     *             |  Closer |                          *       Direction     |  Item   |
     *             |   Item  |                          *       of Search     | in beam |
     *             |_________|                          *           |         |_________|
     *                                                  *           V
     *        ---- Direction of Search --->             *
     *                                                  *
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis18() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 40, 20, 20)
                    FocusableBox(itemInBeam, 0, 10, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 40, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 40, 20, 20)
                    FocusableBox(itemInBeam, 40, 10, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 30, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *                                 ^
     *     __________           ____________            *         ____________            |
     *    |  Item   |          |  focused  |            *        |    Item   |        Direction
     *    | in beam |          |    Item   |            *        |  in beam  |       of Search
     *    |_________|     _____|___________|            *        |___________|            |
     *                   |  Closer |                    *                     __________  |
     *                   |   Item  |                    *                    |  Closer |
     *                   |_________|                    *         ___________|  Item   |
     *                                                  *        |  focused  |_________|
     *          <---- Direction of Search ---           *        |    Item   |
     *                                                  *        |___________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                  *                   ____________
     *                                                  *                  |  focused  |
     *                                                  *          ________|    Item   |
     *         ____________            __________       *         | Closer |___________|
     *        |  focused  |           |  Item   |       *         |  Item  |
     *        |    Item   |           | in beam |       *         |________|
     *        |___________|______     |_________|       *                   ____________
     *                |  Closer |                       *           |      |    Item   |
     *                |   Item  |                       *       Direction  |  in beam  |
     *                |_________|                       *       of Search  |___________|
     *                                                  *           |
     *         ---- Direction of Search --->            *           V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis19() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 20, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 20, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 20, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 20, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *       ^             _________
     *   _______________                                *       |            |  Item  |
     *  | Item in Beam |            ____________        *    Direction       | in beam|
     *  |______________|           |  focused  |        *    of Search       |________|
     *                             |    Item   |        *       |
     *                       ______|___________|        *       |
     *                      |  Closer  |                *    __________
     *                      |   Item   |                *   |  Closer |___________
     *                      |__________|                *   |  Item   |  focused |
     *                                                  *   |_________|    Item  |
     *          <---- Direction of Search ---           *             |__________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                              _______________     *               ____________
     *   ____________              | Item in Beam |     *              |  focused  |
     *  |  focused  |              |______________|     *     _________|   Item    |
     *  |    Item   |                                   *    |  Closer |___________|
     *  |___________|______                             *    |   Item  |
     *          |  Closer |                             *    |_________|
     *          |   Item  |                             *
     *          |_________|                             *         |            _________
     *                                                  *     Direction       |  Item  |
     *            ---- Direction of Search --->         *     of Search       | in beam|
     *                                                  *         |           |________|
     *                                                  *         V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis20() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 10, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 20, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 10, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 30, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 20, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 30, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *         ^             _________
     *   _______________            ____________        *         |            |  Item  |
     *  | Item in Beam |           |  focused  |        *     Direction        | in beam|
     *  |______________|           |    Item   |        *     of Search        |________|
     *                       ______|___________|        *         |
     *                      |  Closer  |                *       __________
     *                      |   Item   |                *      |  Closer |
     *                      |__________|                *      |  Item   |_______________
     *                                                  *      |_________|    focused   |
     *          <---- Direction of Search ---           *                |      Item    |
     *                                                  *                |______________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                  *                 _______________
     *                                                  *                |   focused    |
     *                                                  *       _________|     Item     |
     *    ____________            _______________       *      |  Closer |______________|
     *   |  focused  |           | Item in Beam |       *      |   Item  |
     *   |    Item   |           |______________|       *      |_________|
     *   |___________|______                            *
     *           |  Closer |                            *         |             _________
     *           |   Item  |                            *     Direction        |  Item  |
     *           |_________|                            *     of Search        | in beam|
     *                                                  *         |            |________|
     *                                                  *         V
     *          ---- Direction of Search --->           *
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis21() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 20, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 20, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 20, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 20, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 30, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                 ____________     *                   _______           ^
     *      _______________           |           |     *                  | Item |           |
     *     | Item in Beam |           |  focused  |     *                  |  in  |        Direction
     *     |______________|           |    Item   |     *                  | Beam |        of Search
     *                         _______|___________|     *                  |______|           |
     *                        |  Closer |               *                                     |
     *                        |   Item  |               *     __________
     *                        |_________|               *    |  Closer |
     *                                                  *    |  Item   |______________
     *          <---- Direction of Search ---           *    |_________|   focused   |
     *                                                  *              |     Item    |
     *                                                  *              |_____________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *      ____________                                *               ______________
     *     |           |              _______________   *              |   focused   |
     *     |  focused  |             | Item in Beam |   *     _________|     Item    |
     *     |    Item   |             |______________|   *    |  Closer |_____________|
     *     |___________|______                          *    |   Item  |
     *             |  Closer |                          *    |_________|
     *             |   Item  |                          *
     *             |_________|                          *                   _______         |
     *                                                  *                  | Item |     Direction
     *         ---- Direction of Search --->            *                  |  in  |     of Search
     *                                                  *                  | Beam |         |
     *                                                  *                  |______|         V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis22() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 10, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 20, 40, 30, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 10, 30, 20, 20)
                    FocusableBox(itemInBeam, 40, 10, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 20, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 30, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *              _________           ^
     *                           ____________           *             |  Item  |           |
     *    _______________       |  focused  |           *             | in beam|        Direction
     *   | Item in Beam |       |    Item   |           *             |________|        of Search
     *   |______________|    ___|___________|           *                                  |
     *                      |  Closer |                 *    __________                               |
     *                      |   Item  |                 *   |  Closer |
     *                      |_________|                 *   |  Item   |____________
     *                                                  *   |_________|  focused  |
     *                                                  *             |    Item   |
     *          <---- Direction of Search ---           *             |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *         ____________                             *              ____________
     *        |  focused  |           _______________   *             |  focused  |
     *        |    Item   |          | Item in Beam |   *    _________|    Item   |
     *        |___________|______    |______________|   *   |  Closer |___________|
     *                |  Closer |                       *   |   Item  |                     |
     *                |   Item  |                       *   |_________|                 Direction
     *                |_________|                       *              _________       of Search
     *                                                  *             |  Item  |           |
     *                                                  *             | in beam|           V
     *       ---- Direction of Search --->              *             |________|
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis23() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 20, 20, 20)
                    FocusableBox(itemInBeam, 0, 10, 20, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 20, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 20, 0, 10, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 20, 20, 20)
                    FocusableBox(itemInBeam, 40, 10, 20, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 20, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 20, 40, 10, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *          _________            ^
     *                             ____________         *         |  Item  |            |
     *                            |  focused  |         *         | in beam|         Direction
     *     _______________        |    Item   |         *         |________|         of Search
     *    | Item in Beam |      __|___________|         *                               |
     *    |______________|     |  Closer |              *                               |
     *                         |   Item  |              *    __________
     *                         |_________|              *   |  Closer |____________
     *                                                  *   |  Item   |  focused  |
     *          <---- Direction of Search ---           *   |_________|    Item   |
     *                                                  *             |___________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *     ____________                                 *              ____________
     *    |  focused  |                                 *             |  focused  |
     *    |    Item   |            _______________      *    _________|    Item   |
     *    |___________|______     | Item in Beam |      *   |  Closer |___________|
     *            |  Closer |     |______________|      *   |   Item  |
     *            |   Item  |                           *   |_________|
     *            |_________|                           *                               |
     *                                                  *          _________        Direction
     *         ---- Direction of Search --->            *         |  Item  |        of Search
     *                                                  *         | in beam|            |
     *                                                  *         |________|            V
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemWithOverlappingMajorAxis24() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 20, 20, 20)
                    FocusableBox(itemInBeam, 0, 10, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 20, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 10, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 20, 20, 20)
                    FocusableBox(itemInBeam, 40, 10, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 20, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 10, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                  __________                      *                               ^
     *                 |  Closer |                      *                               |
     *                 |   Item  |                      *         ____________      Direction
     *                 |_________|                      *        |    Item   |      of Search
     *     ______________________________               *        |  in beam  |          |
     *    |  Item    |   |   Focused    |               *        |___________|          |
     *    | in beam  |   |    Item      |               *        |           |       __________
     *    |__________|___|______________|               *        |___________|      |  Closer |
     *                                                  *        |  focused  |      |  Item   |
     *                                                  *        |    Item   |      |_________|
     *          <---- Direction of Search ---           *        |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *              __________                          *
     *             |  Closer |                          *         ____________
     *             |   Item  |                          *        |  focused  |       __________
     *             |_________|                          *        |    Item   |      |  Closer |
     *         ______________________________           *        |___________|      |   Item  |
     *        |  focused  |    |    Item    |           *        |           |      |_________|
     *        |    Item   |    |   in beam  |           *        |___________|
     *        |___________|____|____________|           *        |    Item   |          |
     *                                                  *        |  in beam  |      Direction
     *          ---- Direction of Search --->           *        |___________|      of Search
     *                                                  *                               |
     *                                                  *                               V
     */
    @MediumTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis1() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 0, 40, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 40, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 20, 20, 40, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 40)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 40, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 20, 30, 40, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 40, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 20)
                    FocusableBox(itemInBeam, 0, 20, 20, 40)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *                             ^
     *                                                  *                             |
     *                  __________                      *                          Direction
     *                 |  Closer |                      *                          of Search
     *                 |   Item  |                      *                             |
     *                 |_________|                      *            _________        |
     *     ________________________________             *           |  Item  |    __________
     *    |  Item in Beam  |  |  focused  |             *           | in beam|   |         |
     *    |________________|__|    Item   |             *         __|________|   |  Closer |
     *                     |______________|             *        |  |________|   |  Item   |
     *                                                  *        |  focused  |   |_________|
     *                                                  *        |    Item   |
     *          <---- Direction of Search ---           *        |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *         ____________
     *                |  Closer |                       *        |  focused  |
     *                |   Item  |                       *        |    Item   |       __________
     *                |_________|                       *        |   ________|      |  Closer |
     *         ______________________________           *        |__|________|      |   Item  |
     *        |  focused  |  | Item in Beam |           *           |  Item  |      |_________|
     *        |    Item   |__|______________|           *           | in beam|          |
     *        |______________|                          *           |________|      Direction
     *                                                  *                           of Search
     *          ---- Direction of Search --->           *                               |
     *                                                  *                               V
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis2() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 30, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 0, 30, 30, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 20, 20, 30, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 30)
                    FocusableBox(itemInBeam, 10, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 20, 30, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 30)
                    FocusableBox(itemInBeam, 10, 20, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *                            ^
     *                  __________                      *                            |
     *                 |  Closer |                      *                         Direction
     *                 |   Item  |                      *                         of Search
     *                 |_________|                      *                            |
     *                      _______________             *        _________           |
     *     ________________|___  focused  |             *       |  Item  |       __________
     *    |  Item in Beam  |  |   Item    |             *       | in beam|      |         |
     *    |________________|__|___________|             *       |________|___   |  Closer |
     *                                                  *       |________|  |   |  Item   |
     *                                                  *       |  focused  |   |_________|
     *          <---- Direction of Search ---           *       |    Item   |
     *                                                  *       |___________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *
     *                |  Closer |                       *        ____________
     *                |   Item  |                       *       |  focused  |
     *                |_________|                       *       |    Item   |       __________
     *         _______________                          *       |_________  |      |  Closer |
     *        |  focused   __|_______________           *       |________|__|      |   Item  |
     *        |    Item   |  | Item in Beam |           *       |  Item  |         |_________|
     *        |___________|__|______________|           *       | in beam|             |
     *                                                  *       |________|         Direction
     *          ---- Direction of Search --->           *                          of Search
     *                                                  *                              |
     *                                                  *                              V
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis3() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 30, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 0, 40, 30, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 20, 20, 30, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 30)
                    FocusableBox(itemInBeam, 0, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 20, 40, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 30)
                    FocusableBox(itemInBeam, 0, 20, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                  __________                      *                                 ^
     *                 |  Closer |                      *                                 |
     *                 |   Item  |                      *                             Direction
     *                 |_________|                      *                             of Search
     *                      _______________             *           _________             |
     *      _______________|___           |             *          |  Item  |             |
     *     | Item in Beam  |  |  focused  |             *          | in beam|         __________
     *     |_______________|__|    Item   |             *         _|________|__      |  Closer |
     *                     |______________|             *        | |________| |      |  Item   |
     *                                                  *        |   focused  |      |_________|
     *          <---- Direction of Search ---           *        |    Item    |
     *                                                  *        |____________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *         ____________
     *                |  Closer |                       *        |  focused   |
     *                |   Item  |                       *        |    Item    |       __________
     *                |_________|                       *        |  _________ |      |  Closer |
     *         _______________                          *        |_|________|_|      |   Item  |
     *        |            __|________________          *          |  Item  |        |_________|
     *        |  focused  |  | Item in Beam  |          *          | in beam|            |
     *        |    Item   |__|_______________|          *          |________|        Direction
     *        |______________|                          *                            of Search
     *                                                  *                                |
     *                                                  *                                V
     *         ---- Direction of Search --->            *
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis4() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 30, 30, 30, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 0, 40, 30, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 20, 30, 30, initialFocus)
                    FocusableBox(closerItem, 40, 10, 20, 30)
                    FocusableBox(itemInBeam, 10, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 30, 30, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 20, 40, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 30, 30, initialFocus)
                    FocusableBox(closerItem, 40, 10, 20, 30)
                    FocusableBox(itemInBeam, 10, 20, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                  __________                      *
     *                 |  Closer |                      *    ____________                 ^
     *                 |   Item  |                      *   |    Item   |                 |
     *     ____________|_________|_______               *   |  in beam  |             Direction
     *    |  Item    |   |   Focused    |               *   |___________|             of Search
     *    | in beam  |   |    Item      |               *   |           |__________       |
     *    |__________|___|______________|               *   |___________|  Closer |       |
     *                                                  *   |  focused  |  Item   |
     *                                                  *   |    Item   |_________|
     *          <---- Direction of Search ---           *   |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *              __________                          *    ____________
     *             |  Closer |                          *   |  focused  |__________
     *             |   Item  |                          *   |    Item   |  Closer |
     *         ____|_________|______________            *   |___________|   Item  |       |
     *        |  focused  |   |    Item    |            *   |           |_________|   Direction
     *        |    Item   |   |   in beam  |            *   |___________|             of Search
     *        |___________|___|____________|            *   |    Item   |                 |
     *                                                  *   |  in beam  |                 V
     *          ---- Direction of Search --->           *   |___________|
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis5() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 10, 20, 40, 20, initialFocus)
                    FocusableBox(closerItem, 20, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 20, 30, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 10, 20, 40, initialFocus)
                    FocusableBox(closerItem, 20, 20, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 20, 40, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 20, 20, 30, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 40, initialFocus)
                    FocusableBox(closerItem, 20, 10, 20, 20)
                    FocusableBox(itemInBeam, 0, 20, 20, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *                                   ^
     *                  __________                      *                                   |
     *                 |  Closer |                      *                                Direction
     *                 |   Item  |                      *            _________           of Search
     *     ____________|_________|_________             *           |  Item  |__________    |
     *    |  Item in Beam  |  |  focused  |             *           | in beam|         |    |
     *    |________________|__|    Item   |             *         __|________|  Closer |
     *                     |______________|             *        |  |________|  Item   |
     *                                                  *        |  focused  |_________|
     *                                                  *        |    Item   |
     *          <---- Direction of Search ---           *        |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                  *         ____________
     *                 __________                       *        |  focused  |
     *                |  Closer |                       *        |    Item   |__________
     *                |   Item  |                       *        |   ________|         |
     *         _______|_________|____________           *        |__|________|  Closer |    |
     *        |  focused  |  | Item in Beam |           *           |  Item  |   Item  | Direction
     *        |    Item   |__|______________|           *           | in beam|_________| of Search
     *        |______________|                          *           |________|              |
     *                                                  *                                   V
     *          ---- Direction of Search --->           *
     *                                                  *
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis6() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 20, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 0, 20, 30, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 20, 20, 30, initialFocus)
                    FocusableBox(closerItem, 20, 10, 20, 30)
                    FocusableBox(itemInBeam, 10, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 20, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 20, 20, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 20, 10, 20, 30)
                    FocusableBox(itemInBeam, 10, 20, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                  __________                      *
     *                 |  Closer |                      *
     *                 |   Item  |                      *                                   ^
     *                 |_________|_________             *        _________                  |
     *     ________________|___  focused  |             *       |  Item  |   __________  Direction
     *    |  Item in Beam  |  |   Item    |             *       | in beam|  |         |  of Search
     *    |________________|__|___________|             *       |________|__|  Closer |     |
     *                                                  *       |________|  |  Item   |     |
     *                                                  *       |  focused  |_________|
     *          <---- Direction of Search ---           *       |    Item   |
     *                                                  *       |___________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *        ____________
     *                |  Closer |                       *       |  focused  |
     *                |   Item  |                       *       |    Item   |
     *         _______|_________|                       *       |           |__________
     *        |  focused   __|_______________           *       |_________  |  Closer |      |
     *        |    Item   |  | Item in Beam |           *       |________|__|   Item  |  Direction
     *        |___________|__|______________|           *       |  Item  |  |_________|  of Search
     *                                                  *       | in beam|                   |
     *          ---- Direction of Search --->           *       |________|                   V
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis7() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 20, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 0, 30, 30, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 20, 20, 30, initialFocus)
                    FocusableBox(closerItem, 20, 10, 20, 30)
                    FocusableBox(itemInBeam, 0, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 20, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 20, 30, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 20, 10, 20, 30)
                    FocusableBox(itemInBeam, 0, 20, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                  __________                      *                                  ^
     *                 |  Closer |                      *                                  |
     *                 |   Item  |                      *                              Direction
     *                 |_________|_________             *           _________          of Search
     *      _______________|___           |             *          |  Item  |              |
     *     | Item in Beam  |  |  focused  |             *          | in beam|  __________  |
     *     |_______________|__|    Item   |             *         _|________|_|  Closer |
     *                     |______________|             *        | |________| |  Item   |
     *                                                  *        |   focused  |_________|
     *          <---- Direction of Search ---           *        |    Item    |
     *                                                  *        |____________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                 __________                       *         _____________
     *                |  Closer |                       *        |  focused   |
     *                |   Item  |                       *        |    Item    |__________
     *         _______|_________|                       *        |  _________ |  Closer |
     *        |            __|________________          *        |_|________|_|   Item  |
     *        |  focused  |  | Item in Beam  |          *          |  Item  | |_________|    |
     *        |    Item   |__|_______________|          *          | in beam|            Direction
     *        |______________|                          *          |________|            of Search
     *                                                  *                                    |
     *                                                  *                                    V
     *         ---- Direction of Search --->            *
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis8() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 20, 30, 30, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 0, 30, 30, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 20, 30, 30, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 30)
                    FocusableBox(itemInBeam, 10, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 20, 30, 30, initialFocus)
                    FocusableBox(closerItem, 10, 0, 30, 20)
                    FocusableBox(itemInBeam, 20, 30, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 30, 30, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 30)
                    FocusableBox(itemInBeam, 10, 20, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *           ^
     *     ______________________________               *           |
     *    |  Item    |   |   Focused    |               *       Direction         ____________
     *    | in beam  |   |    Item      |               *       of Search        |    Item   |
     *    |__________|___|______________|               *           |            |  in beam  |
     *                  __________                      *           |            |___________|
     *                 |  Closer |                      *        __________      |           |
     *                 |   Item  |                      *       |  Closer |      |___________|
     *                 |_________|                      *       |  Item   |      |  focused  |
     *                                                  *       |_________|      |    Item   |
     *          <---- Direction of Search ---           *                        |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *         _____________________________            *
     *        |  focused  |   |    Item    |            *                         ____________
     *        |    Item   |   |   in beam  |            *        __________      |  focused  |
     *        |___________|___|____________|            *       |  Closer |      |    Item   |
     *             __________                           *       |   Item  |      |___________|
     *            |  Closer |                           *       |_________|      |           |
     *            |   Item  |                           *                        |___________|
     *            |_________|                           *           |            |    Item   |
     *                                                  *       Direction        |  in beam  |
     *            ---- Direction of Search --->         *       of Search        |___________|
     *                                                  *           |
     *                                                  *           V
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis9() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 10, 0, 40, 20, initialFocus)
                    FocusableBox(closerItem, 20, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 30, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 10, 20, 40, initialFocus)
                    FocusableBox(closerItem, 0, 20, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 20, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 40, 20, initialFocus)
                    FocusableBox(closerItem, 10, 30, 20, 20)
                    FocusableBox(itemInBeam, 20, 0, 30, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 40, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 30, 20, 20, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *         ^
     *                                                  *         |
     *     ________________________________             *      Direction
     *    |  Item in Beam  |  |  focused  |             *      of Search
     *    |________________|__|    Item   |             *         |
     *                     |______________|             *         |              _________
     *                  __________                      *     __________        |  Item  |
     *                 |  Closer |                      *    |         |        | in beam|
     *                 |   Item  |                      *    |  Closer |      __|________|
     *                 |_________|                      *    |  Item   |     |  |________|
     *                                                  *    |_________|     |  focused  |
     *                                                  *                    |    Item   |
     *          <---- Direction of Search ---           *                    |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *         ______________________________           *                     ____________
     *        |  focused  |  | Item in Beam |           *     __________     |  focused  |
     *        |    Item   |__|______________|           *    |  Closer |     |    Item   |
     *        |______________|                          *    |   Item  |     |   ________|
     *                 __________                       *    |         |     |__|________|
     *                |  Closer |                       *    |_________|        |  Item  |
     *                |   Item  |                       *        |              | in beam|
     *                |_________|                       *    Direction          |________|
     *                                                  *    of Search
     *          ---- Direction of Search --->           *        |
     *                                                  *        V
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis10() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 30, 30, 20)
                    FocusableBox(itemInBeam, 0, 0, 30, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 20, 20, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 30)
                    FocusableBox(itemInBeam, 40, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 30, 30, 20)
                    FocusableBox(itemInBeam, 20, 0, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 30)
                    FocusableBox(itemInBeam, 40, 20, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *        ^
     *                                                  *        |
     *                      _______________             *     Direction
     *     ________________|___  focused  |             *     of Search
     *    |  Item in Beam  |  |   Item    |             *        |
     *    |________________|__|___________|             *        |           _________
     *                  __________                      *    __________     |  Item  |
     *                 |  Closer |                      *   |         |     | in beam|
     *                 |   Item  |                      *   |  Closer |     |________|___
     *                 |_________|                      *   |  Item   |     |________|  |
     *                                                  *   |_________|     |  focused  |
     *          <---- Direction of Search ---           *                   |    Item   |
     *                                                  *                   |___________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *         _______________                          *
     *        |  focused   __|_______________           *                    ____________
     *        |    Item   |  | Item in Beam |           *                   |  focused  |
     *        |___________|__|______________|           *    __________     |    Item   |
     *                 __________                       *   |  Closer |     |_________  |
     *                |  Closer |                       *   |   Item  |     |________|__|
     *                |   Item  |                       *   |_________|     |  Item  |
     *                |_________|                       *       |           | in beam|
     *                                                  *   Direction       |________|
     *           ---- Direction of Search --->          *   of Search
     *                                                  *       |
     *                                                  *       V
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis11() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 30, 30, 20)
                    FocusableBox(itemInBeam, 0, 10, 30, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 20, 20, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 30)
                    FocusableBox(itemInBeam, 30, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 30, 30, 20)
                    FocusableBox(itemInBeam, 20, 10, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 30)
                    FocusableBox(itemInBeam, 30, 20, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                      _______________             *           ^
     *      _______________|___           |             *           |
     *     | Item in Beam  |  |  focused  |             *       Direction
     *     |_______________|__|    Item   |             *       of Search
     *                     |______________|             *           |            _________
     *                  __________                      *           |           |  Item  |
     *                 |  Closer |                      *       __________      | in beam|
     *                 |   Item  |                      *      |  Closer |     _|________|__
     *                 |_________|                      *      |  Item   |    | |________| |
     *                                                  *      |_________|    |   focused  |
     *          <---- Direction of Search ---           *                     |    Item    |
     *                                                  *                     |____________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *         _______________                          *                      ____________
     *        |            __|________________          *                     |  focused   |
     *        |  focused  |  | Item in Beam  |          *       __________    |    Item    |
     *        |    Item   |__|_______________|          *      |  Closer |    |  _________ |
     *        |______________|                          *      |   Item  |    |_|________|_|
     *                 __________                       *      |_________|      |  Item  |
     *                |  Closer |                       *          |            | in beam|
     *                |   Item  |                       *      Direction        |________|
     *                |_________|                       *      of Search
     *                                                  *          |
     *         ---- Direction of Search --->            *          V
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis12() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 0, 30, 30, initialFocus)
                    FocusableBox(closerItem, 10, 40, 30, 20)
                    FocusableBox(itemInBeam, 0, 10, 30, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 20, 30, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 30)
                    FocusableBox(itemInBeam, 40, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 30, 30, initialFocus)
                    FocusableBox(closerItem, 10, 40, 30, 20)
                    FocusableBox(itemInBeam, 20, 10, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 30, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 30)
                    FocusableBox(itemInBeam, 40, 20, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *     ______________________________               *                 ____________       ^
     *    |  Item    |   |   Focused    |               *                |    Item   |       |
     *    | in beam  |   |    Item      |               *                |  in beam  |   Direction
     *    |__________|___|______________|               *                |___________|   of Search
     *                 |  Closer |                      *       _________|           |       |
     *                 |   Item  |                      *      |  Closer |___________|       |
     *                 |_________|                      *      |  Item   |  focused  |
     *                                                  *      |_________|    Item   |
     *          <---- Direction of Search ---           *                |___________|
     *                                                  *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *         _____________________________            *                 ____________
     *        |  focused  |   |    Item    |            *       _________|  focused  |
     *        |    Item   |   |   in beam  |            *      |  Closer |    Item   |
     *        |___________|___|____________|            *      |   Item  |___________|
     *            |  Closer |                           *      |_________|           |       |
     *            |   Item  |                           *                |___________|   Direction
     *            |_________|                           *                |    Item   |   of Search
     *                                                  *                |  in beam  |       |
     *          ---- Direction of Search --->           *                |___________|       V
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis13() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 10, 0, 40, 20, initialFocus)
                    FocusableBox(closerItem, 20, 20, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 30, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 20, 10, 20, 40, initialFocus)
                    FocusableBox(closerItem, 0, 20, 20, 20)
                    FocusableBox(itemInBeam, 20, 0, 20, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 40, 20, initialFocus)
                    FocusableBox(closerItem, 10, 20, 20, 20)
                    FocusableBox(itemInBeam, 20, 0, 30, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 20, 0, 20, 40, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 20, 20, 20, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *     ________________________________             *                                ^
     *    |  Item in Beam  |  |  focused  |             *                _________       |
     *    |________________|__|    Item   |             *   __________  |  Item  |    Direction
     *                  ___|______________|             *  |         |  | in beam|    of Search
     *                 |  Closer |                      *  |  Closer |__|________|       |
     *                 |   Item  |                      *  |  Item   |  |________|       |
     *                 |_________|                      *  |_________|  focused  |       |
     *                                                  *            |    Item   |
     *          <---- Direction of Search ---           *            |___________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *         ______________________________           *             ____________
     *        |  focused  |  | Item in Beam |           *  __________|  focused  |
     *        |    Item   |__|______________|           *  |  Closer |           |
     *        |______________|___                       *  |   Item  |   ________|        |
     *                |  Closer |                       *  |         |__|________|    Direction
     *                |   Item  |                       *  |_________|  |        |    of Search
     *                |_________|                       *               |  Item  |        |
     *                                                  *               | in beam|        V
     *           ---- Direction of Search --->          *               |________|
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis14() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 20, 30, 20)
                    FocusableBox(itemInBeam, 0, 0, 30, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 20, 20, 20, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 30)
                    FocusableBox(itemInBeam, 30, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 20, 30, 20)
                    FocusableBox(itemInBeam, 20, 0, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 20, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 30)
                    FocusableBox(itemInBeam, 20, 30, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                      _______________             *
     *     ________________|___  focused  |             *              _________         ^
     *    |  Item in Beam  |  |   Item    |             *    _________|  Item  |         |
     *    |________________|__|___________|             *   |         | in beam|      Direction
     *                 |  Closer |                      *   |  Closer |________|__    of Search
     *                 |   Item  |                      *   |  Item   |________|  |      |
     *                 |_________|                      *   |_________|  focused  |
     *                                                  *             |    Item   |
     *          <---- Direction of Search ---           *             |___________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *         _______________                          *              ____________
     *        |  focused   __|_______________           *    _________|  focused  |
     *        |    Item   |  | Item in Beam |           *   |         |    Item   |
     *        |___________|__|______________|           *   |  Closer |_________  |
     *                |  Closer |                       *   |   Item  |________|__|       |
     *                |   Item  |                       *   |_________|        |      Direction
     *                |_________|                       *             |  Item  |      of Search
     *                                                  *             | in beam|          |
     *          ---- Direction of Search --->           *             |________|          V
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis15() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 20, 30, 20)
                    FocusableBox(itemInBeam, 0, 10, 30, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 20, 20, 20, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 30)
                    FocusableBox(itemInBeam, 20, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 30, 20, initialFocus)
                    FocusableBox(closerItem, 10, 20, 30, 20)
                    FocusableBox(itemInBeam, 20, 10, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 20, 0, 20, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 30)
                    FocusableBox(itemInBeam, 20, 20, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                      _______________             *               _________
     *      _______________|___           |             *              |  Item  |
     *     | Item in Beam  |  |  focused  |             *   __________ | in beam|         ^
     *     |_______________|__|    Item   |             *  |  Closer |_|________|__       |
     *                  ___|______________|             *  |  Item   | |________| |   Direction
     *                 |  Closer |                      *  |_________|   focused  |   of Search
     *                 |   Item  |                      *            |    Item    |       |
     *                 |_________|                      *            |____________|       |
     *                                                  *
     *       <---- Direction of Search ---              *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *         _______________                          *             _____________
     *        |            __|________________          *            |  focused   |
     *        |  focused  |  | Item in Beam  |          *   _________|    Item    |       |
     *        |    Item   |__|_______________|          *  |  Closer |  _________ |   Direction
     *        |______________|___                       *  |   Item  |_|________|_|   of Search
     *                |  Closer |                       *  |_________| |  Item  |         |
     *                |   Item  |                       *              | in beam|         V
     *                |_________|                       *              |________|
     *                                                  *
     *         ---- Direction of Search --->            *
     */
    @LargeTest
    @Test
    fun inBeamOverlappingItemWinsOverCloserItemWithOverlappingMajorAxis16() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 20, 0, 30, 30, initialFocus)
                    FocusableBox(closerItem, 10, 30, 30, 20)
                    FocusableBox(itemInBeam, 0, 10, 30, 10)
                }
                Up -> {
                    FocusableBox(focusedItem, 20, 20, 30, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 30)
                    FocusableBox(itemInBeam, 30, 0, 10, 30)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 30, 30, initialFocus)
                    FocusableBox(closerItem, 10, 30, 30, 20)
                    FocusableBox(itemInBeam, 20, 10, 30, 10)
                }
                Down -> {
                    FocusableBox(focusedItem, 20, 0, 30, 30, initialFocus)
                    FocusableBox(closerItem, 0, 10, 0, 10)
                    FocusableBox(itemInBeam, 30, 20, 10, 30)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *     __________                                   *         ____________       __________
     *    |  Closer |                                   *        |    Item   |      |  Closer |
     *    |   Item  |                                   *        |  in beam  |      |   Item  |
     *    |_________|                                   *        |___________|      |_________|
     *     __________         ____________              *
     *    |  Item   |        |  focused  |              *
     *    | in beam |        |    Item   |              *                               ^
     *    |_________|        |___________|              *         ____________          |
     *                                                  *        |  focused  |      Direction
     *                                                  *        |    Item   |      of Search
     *          <---- Direction of Search ---           *        |___________|          |
     *                                                  *                               |
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                             __________           *
     *                            |  Closer |           *
     *                            |   Item  |           *         ____________          |
     *                            |_________|           *        |  focused  |      Direction
     *         ____________        __________           *        |    Item   |      of Search
     *        |  focused  |       |  Item   |           *        |___________|          |
     *        |    Item   |       | in beam |           *                               V
     *        |___________|       |_________|           *
     *                                                  *         ____________       __________
     *          ---- Direction of Search --->           *        |    Item   |      |  Closer |
     *                                                  *        |  in beam  |      |   Item  |
     *                                                  *        |___________|      |_________|
     */
    @MediumTest
    @Test
    fun inBeamWinsOverOtherItemWithSameMajorAxisDistance1() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 30, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 30, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *                                                  *      __________       ____________
     *     __________         ____________              *     |  Closer |      |    Item   |
     *    |  Item   |        |  focused  |              *     |   Item  |      |  in beam  |
     *    | in beam |        |    Item   |              *     |_________|      |___________|
     *    |_________|        |___________|              *
     *     __________                                   *
     *    |  Closer |                                   *         ^
     *    |   Item  |                                   *         |             ____________
     *    |_________|                                   *     Direction        |  focused  |
     *                                                  *     of Search        |    Item   |
     *          <---- Direction of Search ---           *         |            |___________|
     *                                                  *         |
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *     ____________        __________               *          |            ____________
     *    |  focused  |       |  Item   |               *      Direction       |  focused  |
     *    |    Item   |       | in beam |               *      of Search       |    Item   |
     *    |___________|       |_________|               *          |           |___________|
     *                         __________               *          V
     *                        |  Closer |               *
     *                        |   Item  |               *       __________      ____________
     *                        |_________|               *      |  Closer |     |    Item   |
     *                                                  *      |   Item  |     |  in beam  |
     *          ---- Direction of Search --->           *      |_________|     |___________|
     */
    @LargeTest
    @Test
    fun inBeamWinsOverOtherItemWithSameMajorAxisDistance2() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 0, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 30, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *     _____________                                *         ____________       __________
     *    |   Closer   |                                *        |    Item   |      |         |
     *    |    Item    |                                *        |  in beam  |      |  Closer |
     *    |____________|                                *        |___________|      |   Item  |
     *     __________         ____________              *                           |_________|
     *    |  Item   |        |  focused  |              *
     *    | in beam |        |    Item   |              *                               ^
     *    |_________|        |___________|              *         ____________          |
     *                                                  *        |  focused  |      Direction
     *                                                  *        |    Item   |      of Search
     *          <---- Direction of Search ---           *        |___________|          |
     *                                                  *                               |
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                          _____________           *
     *                         |   Closer   |           *
     *                         |    Item    |           *         ____________          |
     *                         |____________|           *        |  focused  |      Direction
     *         ____________        __________           *        |    Item   |      of Search
     *        |  focused  |       |  Item   |           *        |___________|          |
     *        |    Item   |       | in beam |           *                               V
     *        |___________|       |_________|           *                            __________
     *                                                  *         ____________      |         |
     *          ---- Direction of Search --->           *        |    Item   |      |  Closer |
     *                                                  *        |  in beam  |      |   Item  |
     *                                                  *        |___________|      |_________|
     */
    @MediumTest
    @Test
    fun inBeamWinsOverOtherItemWithSameFarEdge1() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 0, 30, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 30)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 30, 20)
                    FocusableBox(itemInBeam, 40, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 30)
                    FocusableBox(itemInBeam, 0, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *     __________         ____________              *      __________       ____________
     *    |  Item   |        |  focused  |              *     |         |      |    Item   |
     *    | in beam |        |    Item   |              *     |  Closer |      |  in beam  |
     *    |_________|        |___________|              *     |   Item  |      |___________|
     *                                                  *     |_________|
     *     _____________                                *
     *    |   Closer   |                                *         ^
     *    |    Item    |                                *         |             ____________
     *    |____________|                                *     Direction        |  focused  |
     *                                                  *     of Search        |    Item   |
     *         <---- Direction of Search ---            *         |            |___________|
     *                                                  *         |
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *         ____________        __________           *          |            ____________
     *        |  focused  |       |  Item   |           *      Direction       |  focused  |
     *        |    Item   |       | in beam |           *      of Search       |    Item   |
     *        |___________|       |_________|           *          |           |___________|
     *                          _____________           *          V
     *                         |   Closer   |           *       __________
     *                         |    Item    |           *      |         |      ____________
     *                         |____________|           *      |  Closer |     |    Item   |
     *                                                  *      |   Item  |     |  in beam  |
     *        ---- Direction of Search --->             *      |_________|     |___________|
     */
    @LargeTest
    @Test
    fun inBeamWinsOverOtherItemWithSameFarEdge2() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 30, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 0, 20, 30)
                    FocusableBox(itemInBeam, 30, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 30, 20)
                    FocusableBox(itemInBeam, 40, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 30)
                    FocusableBox(itemInBeam, 30, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *         ___________                              *         ____________
     *        |  Closer  |                              *        |    Item   |       __________
     *        |   Item   |                              *        |  in beam  |      |  Closer |
     *        |__________|                              *        |___________|      |   Item  |
     *     __________         ____________              *                           |_________|
     *    |  Item   |        |  focused  |              *
     *    | in beam |        |    Item   |              *                               ^
     *    |_________|        |___________|              *         ____________          |
     *                                                  *        |  focused  |      Direction
     *                                                  *        |    Item   |      of Search
     *          <---- Direction of Search ---           *        |___________|          |
     *                                                  *                               |
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                         ___________              *
     *                        |  Closer  |              *
     *                        |   Item   |              *         ____________          |
     *                        |__________|              *        |  focused  |      Direction
     *         ____________        __________           *        |    Item   |      of Search
     *        |  focused  |       |  Item   |           *        |___________|          |
     *        |    Item   |       | in beam |           *                               V
     *        |___________|       |_________|           *                            __________
     *                                                  *         ____________      |  Closer |
     *          ---- Direction of Search --->           *        |    Item   |      |   Item  |
     *                                                  *        |  in beam  |      |_________|
     *                                                  *        |___________|
     */
    @MediumTest
    @Test
    fun inBeamWinsOverOtherItemWithFarEdgeGreaterThanInBeamCloserEdge1() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 10, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *     __________         ____________              *                        ____________
     *    |  Item   |        |  focused  |              *       __________      |    Item   |
     *    | in beam |        |    Item   |              *      |  Closer |      |  in beam  |
     *    |_________|        |___________|              *      |   Item  |      |___________|
     *         ___________                              *      |_________|
     *        |  Closer  |                              *
     *        |   Item   |                              *          ^
     *        |__________|                              *          |             ____________
     *                                                  *      Direction        |  focused  |
     *          <---- Direction of Search ---           *      of Search        |    Item   |
     *                                                  *          |            |___________|
     *                                                  *          |
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *       ____________        __________             *          |             ____________
     *      |  focused  |       |  Item   |             *      Direction        |  focused  |
     *      |    Item   |       | in beam |             *      of Search        |    Item   |
     *      |___________|       |_________|             *          |            |___________|
     *                       ___________                *          V
     *                      |  Closer  |                *       __________
     *                      |   Item   |                *      |  Closer |       ____________
     *                      |__________|                *      |   Item  |      |    Item   |
     *                                                  *      |_________|      |  in beam  |
     *            ---- Direction of Search --->         *                       |___________|
     */
    @LargeTest
    @Test
    fun inBeamWinsOverOtherItemWithFarEdgeGreaterThanInBeamCloserEdge2() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 10, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 10, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 40, 20, 20)
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
            assertThat(closerItem.value).isFalse()
            assertThat(focusedItem.value).isFalse()
            assertThat(itemInBeam.value).isTrue()
        }
    }

    /**
     *               ___________                       *         ____________
     *              |  Closer  |                       *        |    Item   |
     *              |   Item   |                       *        |  in beam  |
     *              |__________|                       *        |___________|       __________
     *     __________              ____________        *                           |  Closer |
     *    |  Item   |             |  focused  |        *                           |   Item  |
     *    | in beam |             |    Item   |        *                           |_________|
     *    |_________|             |___________|        *                                ^
     *                                                 *         ____________           |
     *                                                 *        |  focused  |       Direction
     *          <---- Direction of Search ---          *        |    Item   |       of Search
     *                                                 *        |___________|           |
     *                                                 *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                   ___________                   *         ____________           |
     *                  |  Closer  |                   *        |  focused  |       Direction
     *                  |   Item   |                   *        |    Item   |       of Search
     *                  |__________|                   *        |___________|           |
     *    ____________              __________         *                                V
     *   |  focused  |             |  Item   |         *                             _________
     *   |    Item   |             | in beam |         *                            | Closer |
     *   |___________|             |_________|         *                            |  Item  |
     *                                                 *         ____________       |________|
     *          ---- Direction of Search --->          *        |    Item   |
     *                                                 *        |  in beam  |
     *                                                 *        |___________|
     */
    @MediumTest
    @Test
    fun inBeamWinsOverOtherItemWithFarEdgeEqualToInBeamCloserEdge_forHorizontalSearch1() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 50, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 50, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 20, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 50, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 50, 20, 20)
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
            when (focusDirection) {
                Left, Right -> {
                    assertThat(closerItem.value).isFalse()
                    assertThat(itemInBeam.value).isTrue()
                }
                Up, Down -> {
                    assertThat(closerItem.value).isTrue()
                    assertThat(itemInBeam.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *     __________              ____________        *                       ____________
     *    |  Item   |             |  focused  |        *                      |    Item   |
     *    | in beam |             |    Item   |        *                      |  in beam  |
     *    |_________|             |___________|        *        __________    |___________|
     *               ___________                       *       |  Closer |
     *              |  Closer  |                       *       |   Item  |
     *              |   Item   |                       *       |_________|
     *              |__________|                       *            ^
     *                                                 *            |          ____________
     *          <---- Direction of Search ---          *        Direction     |  focused  |
     *                                                 *        of Search     |    Item   |
     *                                                 *            |         |___________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *    ____________              __________         *            |          ____________
     *   |  focused  |             |  Item   |         *        Direction     |  focused  |
     *   |    Item   |             | in beam |         *        of Search     |    Item   |
     *   |___________|             |_________|         *            |         |___________|
     *                   ___________                   *            V
     *                  |  Closer  |                   *         _________
     *                  |   Item   |                   *        | Closer |
     *                  |__________|                   *        |  Item  |
     *                                                 *        |________|     ____________
     *          ---- Direction of Search --->          *                      |    Item   |
     *                                                 *                      |  in beam  |
     *                                                 *                      |___________|
     */
    @LargeTest
    @Test
    fun inBeamWinsOverOtherItemWithFarEdgeEqualToInBeamCloserEdge_forHorizontalSearch2() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 50, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 50, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 20, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 50, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 50, 20, 20)
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
            when (focusDirection) {
                Left, Right -> {
                    assertThat(closerItem.value).isFalse()
                    assertThat(itemInBeam.value).isTrue()
                }
                Up, Down -> {
                    assertThat(closerItem.value).isTrue()
                    assertThat(itemInBeam.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *               ___________                      *         ____________
     *              |  Closer  |                      *        |    Item   |
     *              |   Item   |                      *        |  in beam  |
     *              |__________|                      *        |___________|         _________
     *     __________           ____________          *                             | Closer |
     *    |  Item   |          |  focused  |          *                             |  Item  |
     *    | in beam |          |    Item   |          *         ____________        |________|
     *    |_________|          |___________|          *        |  focused  |            ^
     *                                                *        |    Item   |            |
     *                                                *        |___________|        Direction
     *          <---- Direction of Search ---         *                             of Search
     *                                                *                                 |
     *                                                *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                ___________                     *                                 |
     *               |  Closer  |                     *                             Direction
     *               |   Item   |                     *         ____________        of Search
     *               |__________|                     *        |  focused  |            |
     *    ____________           __________           *        |    Item   |            V
     *   |  focused  |          |  Item   |           *        |___________|         _________
     *   |    Item   |          | in beam |           *                             | Closer |
     *   |___________|          |_________|           *                             |  Item  |
     *                                                *         ____________        |________|
     *          ---- Direction of Search --->         *        |    Item   |
     *                                                *        |  in beam  |
     *                                                *        |___________|
     */
    @MediumTest
    @Test
    fun inBeamWinsOverCloserItemForHorizontalSearchButNotForVerticalSearch1() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 20, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 0, 20, 20)
                    FocusableBox(itemInBeam, 40, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 20, 20, 20)
                    FocusableBox(itemInBeam, 0, 40, 20, 20)
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
            when (focusDirection) {
                Left, Right -> {
                    assertThat(closerItem.value).isFalse()
                    assertThat(itemInBeam.value).isTrue()
                }
                Up, Down -> {
                    assertThat(closerItem.value).isTrue()
                    assertThat(itemInBeam.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                  ___________                   *         ____________
     *                 |  Closer  |                   *        |    Item   |
     *                 |   Item   |                   *        |  in beam  |
     *                 |__________|                   *        |___________|
     *     __________                ____________     *                              _________
     *    |  Item   |               |  focused  |     *                             | Closer |
     *    | in beam |               |    Item   |     *                             |  Item  |
     *    |_________|               |___________|     *                             |________|
     *                                                *         ____________            ^
     *                                                *        |  focused  |            |
     *          <---- Direction of Search ---         *        |    Item   |        Direction
     *                                                *        |___________|        of Search
     *                                                *                                 |
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                   __________                   *
     *                  | Closer  |                   *                                 |
     *                  |  Item   |                   *         ____________        Direction
     *                  |_________|                   *        |  focused  |        of Search
     *    ____________               __________       *        |    Item   |            |
     *   |  focused  |              |  Item   |       *        |___________|            V
     *   |    Item   |              | in beam |       *                              _________
     *   |___________|              |_________|       *                             | Closer |
     *                                                *                             |  Item  |
     *          ---- Direction of Search --->         *                             |________|
     *                                                *         ____________
     *                                                *        |    Item   |
     *                                                *        |  in beam  |
     *                                                *        |___________|
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemForHorizontalSearchButNotForVerticalSearch2() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 60, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 60, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 60, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 60, 20, 20)
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
            when (focusDirection) {
                Left, Right -> {
                    assertThat(closerItem.value).isFalse()
                    assertThat(itemInBeam.value).isTrue()
                }
                Up, Down -> {
                    assertThat(closerItem.value).isTrue()
                    assertThat(itemInBeam.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *                  ___________                   *         ____________
     *                 |  Closer  |                   *        |    Item   |
     *                 |   Item   |                   *        |  in beam  |
     *                 |__________|                   *        |___________|
     *     __________              ____________       *                              _________
     *    |  Item   |             |  focused  |       *                             | Closer |
     *    | in beam |             |    Item   |       *                             |  Item  |
     *    |_________|             |___________|       *         ____________        |________|
     *                                                *        |  focused  |            ^
     *                                                *        |    Item   |            |
     *          <---- Direction of Search ---         *        |___________|        Direction
     *                                                *                             of Search
     *                                                *                                 |
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                __________                      *                                 |
     *               | Closer  |                      *                             Direction
     *               |  Item   |                      *         ____________        of Search
     *               |_________|                      *        |  focused  |            |
     *    ____________             __________         *        |    Item   |            V
     *   |  focused  |            |  Item   |         *        |___________|         _________
     *   |    Item   |            | in beam |         *                             | Closer |
     *   |___________|            |_________|         *                             |  Item  |
     *                                                *                             |________|
     *          ---- Direction of Search --->         *         ____________
     *                                                *        |    Item   |
     *                                                *        |  in beam  |
     *                                                *        |___________|
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemForHorizontalSearchButNotForVerticalSearch3() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 50, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 0, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 50, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 30, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 0, 20, 20)
                    FocusableBox(itemInBeam, 50, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 20, 20, 20)
                    FocusableBox(itemInBeam, 0, 50, 20, 20)
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
            when (focusDirection) {
                Left, Right -> {
                    assertThat(closerItem.value).isFalse()
                    assertThat(itemInBeam.value).isTrue()
                }
                Up, Down -> {
                    assertThat(closerItem.value).isTrue()
                    assertThat(itemInBeam.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *     __________           ____________          *                    ____________
     *    |  Item   |          |  focused  |          *                   |    Item   |
     *    | in beam |          |    Item   |          *                   |  in beam  |
     *    |_________|          |___________|          *     _________     |___________|
     *               ___________                      *    | Closer |
     *              |  Closer  |                      *    |  Item  |
     *              |   Item   |                      *    |________|      ____________
     *              |__________|                      *        ^          |  focused  |
     *                                                *        |          |    Item   |
     *          <---- Direction of Search ---         *    Direction      |___________|
     *                                                *    of Search
     *                                                *        |
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *    ____________           __________           *        |
     *   |  focused  |          |  Item   |           *    Direction
     *   |    Item   |          | in beam |           *    of Search       ____________
     *   |___________|          |_________|           *        |          |  focused  |
     *                ___________                     *        V          |    Item   |
     *               |  Closer  |                     *     _________     |___________|
     *               |   Item   |                     *    | Closer |
     *               |__________|                     *    |  Item  |
     *                                                *    |________|      ____________
     *        ---- Direction of Search --->           *                   |    Item   |
     *                                                *                   |  in beam  |
     *                                                *                   |___________|
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemForHorizontalSearchButNotForVerticalSearch4() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 40, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 20, 20, 20)
                    FocusableBox(itemInBeam, 0, 30, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 30, 20, 20)
                    FocusableBox(itemInBeam, 40, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 20, 20, 20)
                    FocusableBox(itemInBeam, 30, 40, 20, 20)
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
            when (focusDirection) {
                Left, Right -> {
                    assertThat(closerItem.value).isFalse()
                    assertThat(itemInBeam.value).isTrue()
                }
                Up, Down -> {
                    assertThat(closerItem.value).isTrue()
                    assertThat(itemInBeam.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *     __________                ____________     *                      ____________
     *    |  Item   |               |  focused  |     *                     |    Item   |
     *    | in beam |               |    Item   |     *                     |  in beam  |
     *    |_________|               |___________|     *                     |___________|
     *                  ___________                   *      _________
     *                 |  Closer  |                   *     | Closer |
     *                 |   Item   |                   *     |  Item  |
     *                 |__________|                   *     |________|
     *                                                *         ^            ____________
     *         <---- Direction of Search ---          *         |           |  focused  |
     *                                                *     Direction       |    Item   |
     *                                                *     of Search       |___________|
     *                                                *         |
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *    ____________               __________       *
     *   |  focused  |              |  Item   |       *         |
     *   |    Item   |              | in beam |       *     Direction        ____________
     *   |___________|              |_________|       *     of Search       |  focused  |
     *                   __________                   *         |           |    Item   |
     *                  | Closer  |                   *         V           |___________|
     *                  |  Item   |                   *      _________
     *                  |_________|                   *     | Closer |
     *                                                *     |  Item  |
     *          ---- Direction of Search --->         *     |________|
     *                                                *                      ____________
     *                                                *                     |    Item   |
     *                                                *                     |  in beam  |
     *                                                *                     |___________|
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemForHorizontalSearchButNotForVerticalSearch5() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 60, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 60, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 60, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 60, 20, 20)
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
            when (focusDirection) {
                Left, Right -> {
                    assertThat(closerItem.value).isFalse()
                    assertThat(itemInBeam.value).isTrue()
                }
                Up, Down -> {
                    assertThat(closerItem.value).isTrue()
                    assertThat(itemInBeam.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *     __________              ____________       *                      ____________
     *    |  Item   |             |  focused  |       *                     |    Item   |
     *    | in beam |             |    Item   |       *                     |  in beam  |
     *    |_________|             |___________|       *                     |___________|
     *                  ___________                   *      _________
     *                 |  Closer  |                   *     | Closer |
     *                 |   Item   |                   *     |  Item  |
     *                 |__________|                   *     |________|       ____________
     *                                                *         ^           |  focused  |
     *           <---- Direction of Search ---        *         |           |    Item   |
     *                                                *     Direction       |___________|
     *                                                *     of Search
     *                                                *         |
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *    ____________             __________         *         |
     *   |  focused  |            |  Item   |         *     Direction
     *   |    Item   |            | in beam |         *     of Search        ____________
     *   |___________|            |_________|         *         |           |  focused  |
     *                __________                      *         V           |    Item   |
     *               | Closer  |                      *      _________      |___________|
     *               |  Item   |                      *     | Closer |
     *               |_________|                      *     |  Item  |
     *                                                *     |________|
     *                                                *                      ____________
     *       ---- Direction of Search --->            *                     |    Item   |
     *                                                *                     |  in beam  |
     *                                                *                     |___________|
     */
    @LargeTest
    @Test
    fun inBeamWinsOverCloserItemForHorizontalSearchButNotForVerticalSearch6() {
        // Arrange.
        val (focusedItem, closerItem, itemInBeam) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 50, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 30, 30, 20, 20)
                    FocusableBox(itemInBeam, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 50, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 30, 20, 20)
                    FocusableBox(itemInBeam, 30, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 20, 30, 20, 20)
                    FocusableBox(itemInBeam, 50, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 30, 0, 20, 20, initialFocus)
                    FocusableBox(closerItem, 0, 20, 20, 20)
                    FocusableBox(itemInBeam, 30, 50, 20, 20)
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
            when (focusDirection) {
                Left, Right -> {
                    assertThat(closerItem.value).isFalse()
                    assertThat(itemInBeam.value).isTrue()
                }
                Up, Down -> {
                    assertThat(closerItem.value).isTrue()
                    assertThat(itemInBeam.value).isFalse()
                }
                else -> error(invalid)
            }
        }
    }

    /**
     *    ____________   ____________   ____________  *    ____________
     *   |  In Beam  |  |  In Beam  |  |  focused  |  *   |  In Beam  |
     *   |  Farther  |  |   Closer  |  |    Item   |  *   |  Farther  |
     *   |___________|  |___________|  |___________|  *   |___________|        ^
     *                                                *    ____________        |
     *         <---- Direction of Search ---          *   |  In Beam  |    Direction
     *                                                *   |   Closer  |    of Search
     *                                                *   |___________|        |
     *                                                *    ____________        |
     *                                                *   |  focused  |
     *                                                *   |    Item   |
     *                                                *   |___________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *    ____________   ____________   ____________  *    ____________
     *   |  focused  |  |  In Beam  |  |  In Beam  |  *   |  focused  |
     *   |    Item   |  |   Closer  |  |  Farther  |  *   |    Item   |
     *   |___________|  |___________|  |___________|  *   |___________|        |
     *                                                *    ____________        |
     *        ---- Direction of Search --->           *   |  In Beam  |    Direction
     *                                                *   |   Closer  |    of Search
     *                                                *   |___________|        |
     *                                                *    ____________        v
     *                                                *   |  In Beam  |
     *                                                *   |  Farther  |
     *                                                *   |___________|
     */
    @MediumTest
    @Test
    fun closerItemWinsWhenThereAreMultipleItemsInBeam1() {
        // Arrange.
        val (focusedItem, inBeamCloser, inBeamFarther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 60, 0, 20, 20, initialFocus)
                    FocusableBox(inBeamCloser, 30, 0, 20, 20)
                    FocusableBox(inBeamFarther, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 60, 20, 20, initialFocus)
                    FocusableBox(inBeamCloser, 0, 30, 20, 20)
                    FocusableBox(inBeamFarther, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(inBeamCloser, 30, 0, 20, 20)
                    FocusableBox(inBeamFarther, 60, 0, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(inBeamCloser, 0, 30, 20, 20)
                    FocusableBox(inBeamFarther, 0, 60, 20, 20)
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
            assertThat(inBeamCloser.value).isTrue()
            assertThat(inBeamFarther.value).isFalse()
        }
    }

    /**
     *                   ____________                 *
     *                  |           |   ____________  *
     *                  |  In Beam  |  |           |  *       ___________
     *    ____________  |   Closer  |  |           |  *      | In Beam  |
     *   |  In Beam  |  |___________|  |  focused  |  *      | Farther  |
     *   |  Farther  |                 |    Item   |  *      |__________|               ^
     *   |___________|                 |           |  *            _____________        |
     *                                 |___________|  *           |  In Beam   |    Direction
     *                                                *           |   Closer   |    of Search
     *         <---- Direction of Search ---          *           |____________|        |
     *                                                *   __________________            |
     *                                                *  |     focused     |
     *                                                *  |      Item       |
     *                                                *  |_________________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *    ____________                                *   __________________
     *   |           |                                *  |     focused     |
     *   |           |                  ____________  *  |      Item       |
     *   |  focused  |                 |  In Beam  |  *  |_________________|            |
     *   |    Item   |   ____________  |  Farther  |  *            _____________        |
     *   |           |  |           |  |___________|  *           |   In Beam  |    Direction
     *   |           |  |  In Beam  |                 *           |    Closer  |    of Search
     *   |___________|  |   Closer  |                 *           |____________|        |
     *                  |___________|                 *       ___________               v
     *                                                *      |  In Beam |
     *        ---- Direction of Search --->           *      |  Farther |
     *                                                *      |__________|
     */
    @LargeTest
    @Test
    fun closerItemWinsWhenThereAreMultipleItemsInBeam2() {
        // Arrange.
        val (focusedItem, inBeamCloser, inBeamFarther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 60, 10, 20, 40, initialFocus)
                    FocusableBox(inBeamCloser, 30, 0, 20, 30)
                    FocusableBox(inBeamFarther, 0, 20, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 60, 40, 20, initialFocus)
                    FocusableBox(inBeamCloser, 20, 30, 30, 20)
                    FocusableBox(inBeamFarther, 10, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 40, initialFocus)
                    FocusableBox(inBeamCloser, 30, 0, 20, 30)
                    FocusableBox(inBeamFarther, 60, 10, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 40, 20, initialFocus)
                    FocusableBox(inBeamCloser, 20, 30, 20, 20)
                    FocusableBox(inBeamFarther, 10, 60, 20, 20)
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
            assertThat(inBeamCloser.value).isTrue()
            assertThat(inBeamFarther.value).isFalse()
        }
    }

    /**
     *                   ____________   ____________  *        ___________
     *    ____________  |  In Beam  |  |           |  *       | In Beam  |
     *   |  In Beam  |  |   Closer  |  |  focused  |  *       | Farther  |
     *   |  Farther  |  |___________|  |    Item   |  *       |__________|           ^
     *   |___________|                 |           |  *          _____________       |
     *                                 |___________|  *         |   In Beam  |   Direction
     *                                                *         |    Closer  |   of Search
     *         <---- Direction of Search ---          *         |____________|       |
     *                                                *    ___________________       |
     *                                                *   |      focused     |
     *                                                *   |        Item      |
     *                                                *   |__________________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                *    ___________________
     *    ____________                                *   |      focused     |
     *   |           |                  ____________  *   |        Item      |
     *   |  focused  |   ____________  |  In Beam  |  *   |__________________|       |
     *   |    Item   |  |  In Beam  |  |  Farther  |  *          _____________       |
     *   |           |  |   Closer  |  |___________|  *         |  In Beam   |   Direction
     *   |___________|  |___________|                 *         |   Closer   |   of Search
     *                                                *         |____________|       |
     *        ---- Direction of Search --->           *        ___________           v
     *                                                *       |  In Beam |
     *                                                *       |  Farther |
     *                                                *       |__________|
     */
    @LargeTest
    @Test
    fun closerItemWinsWhenThereAreMultipleItemsInBeam3() {
        // Arrange.
        val (focusedItem, inBeamCloser, inBeamFarther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 60, 0, 20, 40, initialFocus)
                    FocusableBox(inBeamCloser, 30, 0, 20, 20)
                    FocusableBox(inBeamFarther, 0, 10, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 60, 40, 20, initialFocus)
                    FocusableBox(inBeamCloser, 20, 30, 20, 20)
                    FocusableBox(inBeamFarther, 10, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 40, initialFocus)
                    FocusableBox(inBeamCloser, 30, 20, 20, 20)
                    FocusableBox(inBeamFarther, 60, 10, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 40, 20, initialFocus)
                    FocusableBox(inBeamCloser, 20, 30, 20, 20)
                    FocusableBox(inBeamFarther, 10, 60, 20, 20)
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
            assertThat(inBeamCloser.value).isTrue()
            assertThat(inBeamFarther.value).isFalse()
        }
    }

    /**
     *                                  ____________  *       ____________
     *    ____________   ____________  |           |  *      |  In Beam  |
     *   |  In Beam  |  |  In Beam  |  |  focused  |  *      |  Farther  |
     *   |  Farther  |  |   Closer  |  |    Item   |  *      |___________|        ^
     *   |___________|  |___________|  |           |  *       ____________        |
     *                                 |___________|  *      |  In Beam  |    Direction
     *                                                *      |   Closer  |    of Search
     *         <---- Direction of Search ---          *      |___________|        |
     *                                                *     ________________      |
     *                                                *    |    focused    |
     *                                                *    |      Item     |
     *                                                *    |_______________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                *     ________________
     *    ____________                                *    |    focused    |
     *   |           |   ____________   ____________  *    |      Item     |
     *   |  focused  |  |  In Beam  |  |  In Beam  |  *    |_______________|      |
     *   |    Item   |  |   Closer  |  |  Farther  |  *       ____________        |
     *   |           |  |___________|  |___________|  *      |  In Beam  |    Direction
     *   |___________|                                *      |   Closer  |    of Search
     *                                                *      |___________|        |
     *        ---- Direction of Search --->           *       ____________        v
     *                                                *      |  In Beam  |
     *                                                *      |  Farther  |
     *                                                *      |___________|
     */
    @LargeTest
    @Test
    fun closerItemWinsWhenThereAreMultipleItemsInBeam4() {
        // Arrange.
        val (focusedItem, inBeamCloser, inBeamFarther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 60, 0, 20, 40, initialFocus)
                    FocusableBox(inBeamCloser, 30, 10, 20, 20)
                    FocusableBox(inBeamFarther, 0, 10, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 60, 40, 20, initialFocus)
                    FocusableBox(inBeamCloser, 10, 30, 20, 20)
                    FocusableBox(inBeamFarther, 10, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 40, initialFocus)
                    FocusableBox(inBeamCloser, 30, 10, 20, 20)
                    FocusableBox(inBeamFarther, 60, 10, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 40, 20, initialFocus)
                    FocusableBox(inBeamCloser, 10, 30, 20, 20)
                    FocusableBox(inBeamFarther, 10, 60, 20, 20)
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
            assertThat(inBeamCloser.value).isTrue()
            assertThat(inBeamFarther.value).isFalse()
        }
    }

    /**
     *                                  ____________  *
     *                                 }           |  *            ___________
     *    ____________                 |           |  *           | In Beam  |
     *   |  In Beam  |   ____________  |  focused  |  *           | Farther  |
     *   |  Farther  |  |  In Beam  |  |    Item   |  *           |__________|        ^
     *   |___________|  |   Closer  |  |           |  *      ____________             |
     *                  |___________|  |___________|  *     |  In Beam  |          Direction
     *                                                *     |   Closer  |          of Search
     *         <---- Direction of Search ---          *     |___________|             |
     *                                                *      ____________________     |
     *                                                *     |      focused      |
     *                                                *     |        Item       |
     *                                                *     |___________________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                *      ____________________
     *    ____________   ____________                 *     |      focused      |
     *   |           |  |  In Beam  |   ____________  *     |        Item       |
     *   |  focused  |  |   Closer  |  |  In Beam  |  *     |___________________|      |
     *   |    Item   |  |___________|  |  Farther  |  *      ____________              |
     *   |           |                 |___________|  *     |  In Beam  |          Direction
     *   |           |                                *     |   Closer  |          of Search
     *   |___________|                                *     |___________|              |
     *                                                *           ___________          v
     *          ---- Direction of Search --->         *          |  In Beam |
     *                                                *          |  Farther |
     *                                                *          |__________|
     */
    @LargeTest
    @Test
    fun closerItemWinsWhenThereAreMultipleItemsInBeam5() {
        // Arrange.
        val (focusedItem, inBeamCloser, inBeamFarther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 60, 0, 20, 40, initialFocus)
                    FocusableBox(inBeamCloser, 30, 20, 20, 20)
                    FocusableBox(inBeamFarther, 0, 10, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 0, 60, 40, 20, initialFocus)
                    FocusableBox(inBeamCloser, 0, 30, 20, 20)
                    FocusableBox(inBeamFarther, 10, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 40, initialFocus)
                    FocusableBox(inBeamCloser, 30, 20, 20, 20)
                    FocusableBox(inBeamFarther, 60, 10, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 40, 20, initialFocus)
                    FocusableBox(inBeamCloser, 0, 30, 20, 20)
                    FocusableBox(inBeamFarther, 10, 60, 20, 20)
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
            assertThat(inBeamCloser.value).isTrue()
            assertThat(inBeamFarther.value).isFalse()
        }
    }

    /**
     *                                  ____________  *
     *                                 |           |  *          ___________
     *    ____________                 |           |  *         | In Beam  |
     *   |  In Beam  |                 |  focused  |  *         | Farther  |
     *   |  Farther  |   ____________  |    Item   |  *         |__________|           ^
     *   |___________|  |           |  |           |  *   ____________                 |
     *                  |  In Beam  |  |           |  *  |  In Beam  |             Direction
     *                  |   Closer  |  |___________|  *  |   Closer  |             of Search
     *                  |___________|                 *  |___________|                 |
     *                                                *      ___________________       |
     *                                                *     |     focused      |
     *         <---- Direction of Search ---          *     |       Item       |
     *                                                *     |__________________|
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                   ____________                 *      ___________________
     *    ____________  |           |                 *     |     focused      |
     *   |           |  |  In Beam  |   ____________  *     |       Item       |
     *   |  focused  |  |   Closer  |  |  In Beam  |  *     |__________________|       |
     *   |    Item   |  |___________|  |  Farther  |  *   ____________                 |
     *   |           |                 |___________|  *  |  In Beam  |             Direction
     *   |           |                                *  |   Closer  |             of Search
     *   |___________|                                *  |___________|                 |
     *                                                *          ___________           v
     *                                                *         |  In Beam |
     *        ---- Direction of Search --->           *         |  Farther |
     *                                                *         |__________|
     */
    @LargeTest
    @Test
    fun closerItemWinsWhenThereAreMultipleItemsInBeam6() {
        // Arrange.
        val (focusedItem, inBeamCloser, inBeamFarther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 60, 0, 20, 40, initialFocus)
                    FocusableBox(inBeamCloser, 30, 20, 20, 30)
                    FocusableBox(inBeamFarther, 0, 10, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 10, 60, 40, 20, initialFocus)
                    FocusableBox(inBeamCloser, 0, 30, 30, 20)
                    FocusableBox(inBeamFarther, 20, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 10, 20, 40, initialFocus)
                    FocusableBox(inBeamCloser, 30, 0, 20, 30)
                    FocusableBox(inBeamFarther, 60, 20, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 10, 0, 40, 20, initialFocus)
                    FocusableBox(inBeamCloser, 0, 30, 30, 20)
                    FocusableBox(inBeamFarther, 20, 60, 20, 20)
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
            assertThat(inBeamCloser.value).isTrue()
            assertThat(inBeamFarther.value).isFalse()
        }
    }

    /**
     *                  ____________                 *              ____________            ^
     *                 |           |                 *             |           |            |
     *   ____________  |   Closer  |                 *             |  Farther  |        Direction
     *  |           |  |___________|                 *             |___________|        of Search
     *  |  Farther  |                                *        ____________                  |
     *  |___________|                                *       |           |                  |
     *                                 ____________  *       |   Closer  |
     *                                |  focused  |  *       |___________|
     *                                |    Item   |  *                                 ____________
     *                                |___________|  *                                |  focused  |
     *                                               *                                |    Item   |
     *        <---- Direction of Search ---          *                                |___________|
     *                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                               *   ____________
     *        ---- Direction of Search --->          *  |  focused  |
     *   ____________                                *  |    Item   |
     *  |  focused  |                                *  |___________|
     *  |    Item   |                                *                          ____________
     *  |___________|                                *                         |           |
     *                                 ____________  *      |                  |   Closer  |
     *                                |           |  *      |                  |___________|
     *                  ____________  |  Farther  |  *   Direction        ____________
     *                 |           |  |___________|  *   of Search       |           |
     *                 |   Closer  |                 *      |            |  Farther  |
     *                 |___________|                 *      v            |___________|
     */
    @MediumTest
    @Test
    fun closerItemWinsForItemsOutsideBeam1() {
        // Arrange.
        val (focusedItem, closer, farther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 60, 40, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 0, 20, 20)
                    FocusableBox(farther, 0, 10, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 40, 60, 20, 20, initialFocus)
                    FocusableBox(closer, 0, 30, 20, 20)
                    FocusableBox(farther, 10, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 40, 20, 20)
                    FocusableBox(farther, 60, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 40, 30, 20, 20)
                    FocusableBox(farther, 30, 60, 20, 20)
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
            assertThat(closer.value).isTrue()
            assertThat(farther.value).isFalse()
        }
    }

    /**
     *                                               *              ____________            ^
     *                                               *             |           |            |
     *   ____________   ____________                 *             |  Farther  |        Direction
     *  |           |  |           |                 *             |___________|        of Search
     *  |  Farther  |  |   Closer  |                 *              ____________            |
     *  |___________|  |___________|                 *             |           |            |
     *                                 ____________  *             |   Closer  |
     *                                |  focused  |  *             |___________|
     *                                |    Item   |  *                                 ____________
     *                                |___________|  *                                |  focused  |
     *                                               *                                |    Item   |
     *        <---- Direction of Search ---          *                                |___________|
     *                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                               *   ____________
     *        ---- Direction of Search --->          *  |  focused  |
     *   ____________                                *  |    Item   |
     *  |  focused  |                                *  |___________|
     *  |    Item   |                                *                    ____________
     *  |___________|                                *                   |           |
     *                  ____________   ____________  *      |            |   Closer  |
     *                 |           |  |           |  *      |            |___________|
     *                 |   Closer  |  |  Farther  |  *   Direction        ____________
     *                 |___________|  |___________|  *   of Search       |           |
     *                                               *      |            |  Farther  |
     *                                               *      v            |___________|
     */
    @LargeTest
    @Test
    fun closerItemWinsForItemsOutsideBeam2() {
        // Arrange.
        val (focusedItem, closer, farther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 60, 30, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 0, 20, 20)
                    FocusableBox(farther, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 30, 60, 20, 20, initialFocus)
                    FocusableBox(closer, 0, 30, 20, 20)
                    FocusableBox(farther, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 30, 20, 20)
                    FocusableBox(farther, 60, 30, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 30, 20, 20)
                    FocusableBox(farther, 30, 60, 20, 20)
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
            assertThat(closer.value).isTrue()
            assertThat(farther.value).isFalse()
        }
    }

    /**
     *   ____________                                *        ____________                  ^
     *  |           |                                *       |           |                  |
     *  |  Farther  |   ____________                 *       |  Farther  |              Direction
     *  |___________|  |           |                 *       |___________|              of Search
     *                 |   Closer  |                 *               ____________            |
     *                 |___________|                 *              |           |            |
     *                                 ____________  *              |   Closer  |
     *                                |  focused  |  *              |___________|
     *                                |    Item   |  *                                 ____________
     *                                |___________|  *                                |  focused  |
     *                                               *                                |    Item   |
     *        <---- Direction of Search ---          *                                |___________|
     *                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                               *   ____________
     *        ---- Direction of Search --->          *  |  focused  |
     *   ____________                                *  |    Item   |
     *  |  focused  |                                *  |___________|
     *  |    Item   |                                *                  ____________
     *  |___________|                                *                 |           |
     *                  ____________                 *      |          |   Closer  |
     *                 |           |                 *      |          |___________|
     *                 |   Closer  |   ____________  *   Direction             ____________
     *                 |___________|  |           |  *   of Search            |           |
     *                                |  Farther  |  *      |                 |  Farther  |
     *                                |___________|  *      v                 |___________|
     */
    @LargeTest
    @Test
    fun closerItemWinsForItemsOutsideBeam3() {
        // Arrange.
        val (focusedItem, closer, farther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 60, 40, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 10, 20, 20)
                    FocusableBox(farther, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 40, 60, 20, 20, initialFocus)
                    FocusableBox(closer, 10, 30, 20, 20)
                    FocusableBox(farther, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 30, 20, 20)
                    FocusableBox(farther, 60, 40, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 30, 20, 20)
                    FocusableBox(farther, 40, 60, 20, 20)
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
            assertThat(closer.value).isTrue()
            assertThat(farther.value).isFalse()
        }
    }

    /**
     *   ____________
     *  |           |
     *  |  Farther  |                                     ^
     *  |___________|                                     |
     *                  ____________                  Direction
     *                 |           |                  of Search
     *                 |   Closer  |                      |
     *                 |___________|                      |
     *                                 ____________
     *                                |  focused  |
     *                                |    Item   |
     *                                |___________|
     *
     *        <---- Direction of Search ---
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     *        ---- Direction of Search --->
     *   ____________
     *  |  focused  |
     *  |    Item   |
     *  |___________|
     *                  ____________                      |
     *                 |           |                      |
     *                 |   Closer  |                   Direction
     *                 |___________|                   of Search
     *                                 ____________       |
     *                                |           |       v
     *                                |  Farther  |
     *                                |___________|
     */
    @LargeTest
    @Test
    fun closerItemWinsForItemsOutsideBeam4() {
        // Arrange.
        val (focusedItem, closer, farther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left, Up -> {
                    FocusableBox(focusedItem, 60, 60, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 30, 20, 20)
                    FocusableBox(farther, 0, 0, 20, 20)
                }
                Right, Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 30, 20, 20)
                    FocusableBox(farther, 60, 60, 20, 20)
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
            assertThat(closer.value).isTrue()
            assertThat(farther.value).isFalse()
        }
    }

    /**
     *          ____________                         *
     *         |           |                         *
     *         |  Farther  |                         *                                      ^
     *         |___________|                         *                                      |
     *                ____________                   *  ____________                    Direction
     *               |           |                   * |           |                    of Search
     *               |   Closer  |                   * |  Farther  |   ____________         |
     *               |___________|                   * |___________|  |           |         |
     *                                 ____________  *                |   Closer  |
     *                                |  focused  |  *                |___________|
     *                                |    Item   |  *                                 ____________
     *                                |___________|  *                                |  focused  |
     *                                               *                                |    Item   |
     *        <---- Direction of Search ---          *                                |___________|
     *                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                               *   ____________
     *        ---- Direction of Search --->          *  |  focused  |
     *   ____________                                *  |    Item   |
     *  |  focused  |                                *  |___________|
     *  |    Item   |                                *                  ____________
     *  |___________|                                *                 |           |
     *                   ____________                *      |          |   Closer  |    ____________
     *                  |           |                *      |          |___________|   |           |
     *                  |   Closer  |                *   Direction                     |  Farther  |
     *                  |___________|                *   of Search                     |___________|
     *                           ____________        *      |
     *                          |           |        *      v
     *                          |  Farther  |        *
     *                          |___________|        *
     */
    @LargeTest
    @Test
    fun closerItemWinsForItemsOutsideBeam5() {
        // Arrange.
        val (focusedItem, closer, farther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 60, 20, 20, initialFocus)
                    FocusableBox(closer, 10, 30, 20, 20)
                    FocusableBox(farther, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 60, 40, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 10, 20, 20)
                    FocusableBox(farther, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 30, 20, 20)
                    FocusableBox(farther, 40, 60, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 30, 20, 20)
                    FocusableBox(farther, 60, 40, 20, 20)
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
            assertThat(closer.value).isTrue()
            assertThat(farther.value).isFalse()
        }
    }

    /**
     *                ____________                   *
     *               |           |                   *
     *               |  Farther  |                   *                                      ^
     *               |___________|                   *                                      |
     *                ____________                   *                                  Direction
     *               |           |                   *                                  of Search
     *               |   Closer  |                   *  ____________   ____________         |
     *               |___________|                   * |           |  |           |         |
     *                                 ____________  * |  Farther  |  |   Closer  |
     *                                |  focused  |  * |___________|  |___________|
     *                                |    Item   |  *                                 ____________
     *                                |___________|  *                                |  focused  |
     *                                               *                                |    Item   |
     *        <---- Direction of Search ---          *                                |___________|
     *                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                               *   ____________
     *        ---- Direction of Search --->          *  |  focused  |
     *   ____________                                *  |    Item   |
     *  |  focused  |                                *  |___________|
     *  |    Item   |                                *                  ____________    ____________
     *  |___________|                                *                 |           |   |           |
     *                   ____________                *      |          |   Closer  |   |  Farther  |
     *                  |           |                *      |          |___________|   |___________|
     *                  |   Closer  |                *   Direction
     *                  |___________|                *   of Search
     *                   ____________                *      |
     *                  |           |                *      v
     *                  |  Farther  |                *
     *                  |___________|                *
     */
    @LargeTest
    @Test
    fun closerItemWinsForItemsOutsideBeam6() {
        // Arrange.
        val (focusedItem, closer, farther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 30, 60, 20, 20, initialFocus)
                    FocusableBox(closer, 0, 30, 20, 20)
                    FocusableBox(farther, 0, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 60, 30, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 0, 20, 20)
                    FocusableBox(farther, 0, 0, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 30, 20, 20)
                    FocusableBox(farther, 30, 60, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 30, 20, 20)
                    FocusableBox(farther, 60, 30, 20, 20)
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
            assertThat(closer.value).isTrue()
            assertThat(farther.value).isFalse()
        }
    }

    /**
     *                ____________                   *
     *               |           |                   *
     *               |  Farther  |                   *                                      ^
     *               |___________|                   *                                      |
     *           ____________                        *                 ____________     Direction
     *          |           |                        *                |           |     of Search
     *          |   Closer  |                        *  ____________  |   Closer  |         |
     *          |___________|                        * |           |  |___________|         |
     *                                 ____________  * |  Farther  |
     *                                |  focused  |  * |___________|
     *                                |    Item   |  *                                 ____________
     *                                |___________|  *                                |  focused  |
     *                                               *                                |    Item   |
     *        <---- Direction of Search ---          *                                |___________|
     *                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                               *  ____________
     *        ---- Direction of Search --->          * |  focused  |
     *   ____________                                * |    Item   |
     *  |  focused  |                                * |___________|
     *  |    Item   |                                *                                  ____________
     *  |___________|                                *                                 |           |
     *                            ____________       *      |           ____________   |  Farther  |
     *                           |           |       *      |          |           |   |___________|
     *                           |   Closer  |       *   Direction     |   Closer  |
     *                           |___________|       *   of Search     |___________|
     *                      ____________             *      |
     *                     |           |             *      v
     *                     |  Farther  |             *
     *                     |___________|             *
     */
    @MediumTest
    @Test
    fun fartherItemWinsWhenTheMinorAxisDistanceIsMuchSmaller() {
        // Arrange.
        val (focusedItem, closer, farther) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            when (focusDirection) {
                Left -> {
                    FocusableBox(focusedItem, 40, 60, 20, 20, initialFocus)
                    FocusableBox(closer, 0, 30, 20, 20)
                    FocusableBox(farther, 10, 0, 20, 20)
                }
                Up -> {
                    FocusableBox(focusedItem, 60, 40, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 0, 20, 20)
                    FocusableBox(farther, 0, 10, 20, 20)
                }
                Right -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 40, 30, 20, 20)
                    FocusableBox(farther, 30, 60, 20, 20)
                }
                Down -> {
                    FocusableBox(focusedItem, 0, 0, 20, 20, initialFocus)
                    FocusableBox(closer, 30, 40, 20, 20)
                    FocusableBox(farther, 60, 30, 20, 20)
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
            assertThat(closer.value).isFalse()
            assertThat(farther.value).isTrue()
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
