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
import androidx.compose.ui.focus.FocusDirection.Companion.Out
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TwoDimensionalFocusTraversalOutTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var focusManager: FocusManager
    private val initialFocus: FocusRequester = FocusRequester()
    private val focusedItem = mutableStateOf(false)

    /**
     *      ________________
     *     |  focusedItem  |
     *     |_______________|
     */
    @Test
    fun focusOut_noParent_focusStateUnchanged() {
        // Arrange.
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 10, 10, initialFocus)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Out) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(focusedItem.value).isTrue()
        }
    }

    /**
     *      _____________________
     *     |  parent            |
     *     |   _______________  |
     *     |  | focusedItem  |  |
     *     |  |______________|  |
     *     |____________________|
     */
    @Test
    fun focusOut_focusesOnParent() {
        // Arrange.
        val parent = mutableStateOf(false)
        rule.setContentForTest {
            FocusableBox(parent, 0, 0, 30, 30) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Out) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isTrue()
        }
    }

    /**
     *      _____________________
     *     |  parent            |
     *     |   _______________  |
     *     |  | focusedItem  |  |
     *     |  |______________|  |
     *     |____________________|
     */
    @Test
    fun focusOut_doesNotFocusOnDeactivatedParent() {
        // Arrange.
        val parent = mutableStateOf(false)
        rule.setContentForTest {
            FocusableBox(parent, 0, 0, 30, 30, deactivated = true) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Out) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(focusedItem.value).isTrue()
            assertThat(parent.value).isFalse()
        }
    }

    /**
     *      __________________________
     *     |  grandparent            |
     *     |   ____________________  |
     *     |  |  parent           |  |
     *     |  |   ______________  |  |
     *     |  |  | focusedItem |  |  |
     *     |  |  |_____________|  |  |
     *     |  |___________________|  |
     *     |_________________________|
     */
    @Test
    fun focusOut_focusesOnImmediateParent() {
        // Arrange.
        val (parent, grandparent) = List(2) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(grandparent, 0, 0, 50, 50) {
                FocusableBox(parent, 10, 10, 30, 30) {
                    FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                }
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Out) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isTrue()
            assertThat(grandparent.value).isFalse()
        }
    }

    /**
     *      __________________________
     *     |  grandparent            |
     *     |   ____________________  |
     *     |  |  parent           |  |
     *     |  |   ______________  |  |
     *     |  |  | focusedItem |  |  |
     *     |  |  |_____________|  |  |
     *     |  |___________________|  |
     *     |_________________________|
     */
    @Test
    fun focusOut_skipsImmediateParentIfItIsDeactivated() {
        // Arrange.
        val (parent, grandparent) = List(2) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(grandparent, 0, 0, 50, 50) {
                FocusableBox(parent, 10, 10, 30, 30, deactivated = true) {
                    FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                }
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Out) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(grandparent.value).isTrue()
        }
    }

    /**
     *      __________________________
     *     |  grandparent            |
     *     |   ____________________  |
     *     |  |  parent           |  |
     *     |  |   ______________  |  |
     *     |  |  | focusedItem |  |  |
     *     |  |  |_____________|  |  |
     *     |  |___________________|  |
     *     |_________________________|
     */
    @Test
    fun focusOut_doesNotChangeIfAllParentsAreDeactivated() {
        // Arrange.
        val (parent, grandparent) = List(2) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(grandparent, 0, 0, 50, 50, deactivated = true) {
                FocusableBox(parent, 10, 10, 30, 30, deactivated = true) {
                    FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                }
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Out) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(focusedItem.value).isTrue()
            assertThat(parent.value).isFalse()
            assertThat(grandparent.value).isFalse()
        }
    }

    /**
     *      _____________________
     *     |  parent            |
     *     |   _______________  |   ____________
     *     |  | focusedItem  |  |  | nextItem  |
     *     |  |______________|  |  |___________|
     *     |____________________|
     */
    @Test
    fun focusRight_focusesOnSiblingOfParent() {
        // Arrange.
        val (parent, nextItem) = List(2) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(parent, 0, 0, 30, 30) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
            }
            FocusableBox(nextItem, 40, 10, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(nextItem.value).isTrue()
        }
    }

    /**
     *      _____________________
     *     |  parent            |
     *     |   _______________  |   ___________________   ____________
     *     |  | focusedItem  |  |  | deactivatedItem  |  | nextItem  |
     *     |  |______________|  |  |__________________|  |___________|
     *     |____________________|
     */
    @Test
    fun focusRight_focusesOnNonDeactivatedSiblingOfParent() {
        // Arrange.
        val (parent, deactivated, nextItem) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(parent, 0, 0, 30, 30) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
            }
            FocusableBox(deactivated, 40, 10, 10, 10, deactivated = true)
            FocusableBox(nextItem, 60, 10, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(deactivated.value).isFalse()
            assertThat(nextItem.value).isTrue()
        }
    }

    /**
     *      _____________________
     *     |  parent            |
     *     |   _______________  |   ___________________________________________
     *     |  | focusedItem  |  |  | deactivatedItem & nextItem (overlapping) |
     *     |  |______________|  |  |__________________________________________|
     *     |____________________|
     */
    @Test
    fun focusRight_focusesOnNonDeactivatedSiblingOfParent_withOverlappingDeactivatedItem() {
        // Arrange.
        val (parent, deactivated, nextItem) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(parent, 0, 0, 30, 30) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
            }
            FocusableBox(deactivated, 40, 10, 10, 10, deactivated = true)
            FocusableBox(nextItem, 40, 10, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(deactivated.value).isFalse()
            assertThat(nextItem.value).isTrue()
        }
    }

    /**
     *    ___________________________
     *   |  grandparent             |
     *   |   _____________________  |
     *   |  |  parent            |  |
     *   |  |   _______________  |  |   ____________
     *   |  |  | focusedItem  |  |  |  | nextItem  |
     *   |  |  |______________|  |  |  |___________|
     *   |  |____________________|  |
     *   |__________________________|
     */
    @Test
    fun focusRight_focusesOnSiblingOfGrandparent() {
        // Arrange.
        val (grandparent, parent, nextItem) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(parent, 0, 0, 50, 50) {
                FocusableBox(parent, 10, 0, 30, 30) {
                    FocusableBox(focusedItem, 20, 10, 10, 10, initialFocus)
                }
            }
            FocusableBox(nextItem, 60, 10, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(grandparent.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(nextItem.value).isTrue()
        }
    }

    /**
     *                       _____________________
     *                      |  parent            |
     *    _______________   |   _______________  |
     *   |    item1     |   |  |  focusedItem |  |
     *   |______________|   |  |______________|  |
     *    _______________   |   _______________  |
     *   |    item2     |   |  |    item4     |  |
     *   |______________|   |  |______________|  |
     *    _______________   |   _______________  |
     *   |    item3     |   |  |    item5     |  |
     *   |______________|   |  |______________|  |
     *                      |____________________|
     */
    @Test
    fun focusLeft_fromItemOnLeftEdge_movesFocusOutsideParent() {
        // Arrange.
        val parent = mutableStateOf(false)
        val (item1, item2, item3, item4, item5) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(item1, 0, 10, 10, 10)
            FocusableBox(item2, 0, 30, 10, 10)
            FocusableBox(item3, 0, 50, 10, 10)
            FocusableBox(parent, 20, 0, 30, 70) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                FocusableBox(item4, 10, 30, 10, 10)
                FocusableBox(item5, 10, 50, 10, 10)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Left) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isTrue()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
            assertThat(item4.value).isFalse()
            assertThat(item5.value).isFalse()
        }
    }

    /**
     *                       _____________________
     *                      |  parent            |
     *    _______________   |   _______________  |
     *   |    item1     |   |  |  focusedItem |  |
     *   |______________|   |  |______________|  |
     *    _______________   |   _______________  |
     *   |    item2     |   |  |    item4     |  |
     *   |______________|   |  |______________|  |
     *    _______________   |   _______________  |
     *   |    item3     |   |  |    item5     |  |
     *   |______________|   |  |______________|  |
     *                      |____________________|
     */
    @Test
    fun focusLeft_fromItemOnLeftEdge_movesFocusOutsideDeactivatedParent() {
        // Arrange.
        val parent = mutableStateOf(false)
        val (item1, item2, item3, item4, item5) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(item1, 0, 10, 10, 10)
            FocusableBox(item2, 0, 30, 10, 10)
            FocusableBox(item3, 0, 50, 10, 10)
            FocusableBox(parent, 20, 0, 30, 70, deactivated = true) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                FocusableBox(item4, 10, 30, 10, 10)
                FocusableBox(item5, 10, 50, 10, 10)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Left) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isTrue()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
            assertThat(item4.value).isFalse()
            assertThat(item5.value).isFalse()
        }
    }

    /**
     *      _____________________
     *     |  parent            |
     *     |   _______________  |    _______________
     *     |  | focusedItem  |  |   |    item3     |
     *     |  |______________|  |   |______________|
     *     |   _______________  |    _______________
     *     |  |    item1     |  |   |    item4     |
     *     |  |______________|  |   |______________|
     *     |   _______________  |    _______________
     *     |  |    item2     |  |   |    item5     |
     *     |  |______________|  |   |______________|
     *     |____________________|
     */
    @Test
    fun focusRight_fromItemOnRightEdge_movesFocusOutsideParent() {
        // Arrange.
        val parent = mutableStateOf(false)
        val (item1, item2, item3, item4, item5) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(parent, 0, 0, 30, 70) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                FocusableBox(item1, 10, 30, 10, 10)
                FocusableBox(item2, 10, 50, 10, 10)
            }
            FocusableBox(item3, 40, 10, 10, 10)
            FocusableBox(item4, 40, 30, 10, 10)
            FocusableBox(item5, 40, 50, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isTrue()
            assertThat(item4.value).isFalse()
            assertThat(item5.value).isFalse()
        }
    }

    /**
     *      _____________________
     *     |  parent            |
     *     |   _______________  |    _______________
     *     |  | focusedItem  |  |   |    item3     |
     *     |  |______________|  |   |______________|
     *     |   _______________  |    _______________
     *     |  |    item1     |  |   |    item4     |
     *     |  |______________|  |   |______________|
     *     |   _______________  |    _______________
     *     |  |    item2     |  |   |    item5     |
     *     |  |______________|  |   |______________|
     *     |____________________|
     */
    @Test
    fun focusRight_fromItemOnRightEdge_movesFocusOutsideDeactivatedParent() {
        // Arrange.
        val parent = mutableStateOf(false)
        val (item1, item2, item3, item4, item5) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(parent, 0, 0, 30, 70, deactivated = true) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                FocusableBox(item1, 10, 30, 10, 10)
                FocusableBox(item2, 10, 50, 10, 10)
            }
            FocusableBox(item3, 40, 10, 10, 10)
            FocusableBox(item4, 40, 30, 10, 10)
            FocusableBox(item5, 40, 50, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Right) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isTrue()
            assertThat(item4.value).isFalse()
            assertThat(item5.value).isFalse()
        }
    }

    /**
     *       _______________   _______________   _______________
     *      |    item1     |  |    item2     |  |    item3     |
     *      |______________|  |______________|  |______________|
     *    _________________________________________________________
     *   |   parent                                               |
     *   |   _______________   _______________   _______________  |
     *   |  | focusedItem  |  |    item4     |  |    item5     |  |
     *   |  |______________|  |______________|  |______________|  |
     *   |________________________________________________________|
     */
    @Test
    fun focusUp_fromTopmostItem_movesFocusOutsideParent() {
        // Arrange.
        val parent = mutableStateOf(false)
        val (item1, item2, item3, item4, item5) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(item1, 10, 0, 10, 10)
            FocusableBox(item2, 30, 0, 10, 10)
            FocusableBox(item3, 50, 0, 10, 10)
            FocusableBox(parent, 0, 20, 70, 30) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                FocusableBox(item4, 30, 10, 10, 10)
                FocusableBox(item5, 50, 10, 10, 10)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Up) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isTrue()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
            assertThat(item4.value).isFalse()
            assertThat(item5.value).isFalse()
        }
    }

    /**
     *       _______________   _______________   _______________
     *      |    item1     |  |    item2     |  |    item3     |
     *      |______________|  |______________|  |______________|
     *    _________________________________________________________
     *   |   parent                                               |
     *   |   _______________   _______________   _______________  |
     *   |  | focusedItem  |  |    item4     |  |    item5     |  |
     *   |  |______________|  |______________|  |______________|  |
     *   |________________________________________________________|
     */
    @Test
    fun focusUp_fromTopmostItem_movesFocusOutsideDeactivatedParent() {
        // Arrange.
        val parent = mutableStateOf(false)
        val (item1, item2, item3, item4, item5) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(item1, 10, 0, 10, 10)
            FocusableBox(item2, 30, 0, 10, 10)
            FocusableBox(item3, 50, 0, 10, 10)
            FocusableBox(parent, 0, 20, 70, 30, deactivated = true) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                FocusableBox(item4, 30, 10, 10, 10)
                FocusableBox(item5, 50, 10, 10, 10)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Up) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isTrue()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
            assertThat(item4.value).isFalse()
            assertThat(item5.value).isFalse()
        }
    }

    /**
     *    _________________________________________________________
     *   |   parent                                               |
     *   |   _______________   _______________   _______________  |
     *   |  | focusedItem  |  |    item1     |  |    item2     |  |
     *   |  |______________|  |______________|  |______________|  |
     *   |________________________________________________________|
     *       _______________   _______________   _______________
     *      |    item3     |  |    item4     |  |    item5     |
     *      |______________|  |______________|  |______________|
     */
    @Test
    fun focusDown_fromBottommostItem_movesFocusOutsideParent() {
        // Arrange.
        val parent = mutableStateOf(false)
        val (item1, item2, item3, item4, item5) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(parent, 0, 0, 70, 30) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                FocusableBox(item1, 30, 10, 10, 10)
                FocusableBox(item2, 50, 10, 10, 10)
            }
            FocusableBox(item3, 10, 40, 10, 10)
            FocusableBox(item4, 30, 40, 10, 10)
            FocusableBox(item5, 50, 40, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Down) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isTrue()
            assertThat(item4.value).isFalse()
            assertThat(item5.value).isFalse()
        }
    }

    /**
     *    _________________________________________________________
     *   |   parent                                               |
     *   |   _______________   _______________   _______________  |
     *   |  | focusedItem  |  |    item1     |  |    item2     |  |
     *   |  |______________|  |______________|  |______________|  |
     *   |________________________________________________________|
     *       _______________   _______________   _______________
     *      |    item3     |  |    item4     |  |    item5     |
     *      |______________|  |______________|  |______________|
     */
    @Test
    fun focusDown_fromBottommostItem_movesFocusOutsideDeactivatedParent() {
        // Arrange.
        val parent = mutableStateOf(false)
        val (item1, item2, item3, item4, item5) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(parent, 0, 0, 70, 30, deactivated = true) {
                FocusableBox(focusedItem, 10, 10, 10, 10, initialFocus)
                FocusableBox(item1, 30, 10, 10, 10)
                FocusableBox(item2, 50, 10, 10, 10)
            }
            FocusableBox(item3, 10, 40, 10, 10)
            FocusableBox(item4, 30, 40, 10, 10)
            FocusableBox(item5, 50, 40, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Down) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isTrue()
            assertThat(item4.value).isFalse()
            assertThat(item5.value).isFalse()
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
