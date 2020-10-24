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

package androidx.compose.runtime.savedinstancestate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.test.filters.MediumTest
import androidx.ui.test.StateRestorationTester
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(ExperimentalRestorableStateHolder::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class RestorableStateHolderTest {

    @get:Rule
    val rule = createComposeRule()

    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun stateIsRestoredWhenGoBackToScreen1() {
        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var numberOnScreen1 = -1
        var restorableNumberOnScreen1 = -1
        restorationTester.setContent {
            val holder = rememberRestorableStateHolder<Screens>()
            holder.withRestorableState(screen) {
                if (screen == Screens.Screen1) {
                    numberOnScreen1 = remember { increment++ }
                    restorableNumberOnScreen1 = rememberSavedInstanceState { increment++ }
                } else {
                    // screen 2
                    remember { 100 }
                }
            }
        }

        rule.runOnIdle {
            assertThat(numberOnScreen1).isEqualTo(0)
            assertThat(restorableNumberOnScreen1).isEqualTo(1)
            screen = Screens.Screen2
        }

        // wait for the screen switch to apply
        rule.runOnIdle {
            numberOnScreen1 = -1
            restorableNumberOnScreen1 = -1
            // switch back to screen1
            screen = Screens.Screen1
        }

        rule.runOnIdle {
            assertThat(numberOnScreen1).isEqualTo(2)
            assertThat(restorableNumberOnScreen1).isEqualTo(1)
        }
    }

    @Test
    fun simpleRestoreOnlyOneScreen() {
        var increment = 0
        var number = -1
        var restorableNumber = -1
        restorationTester.setContent {
            val holder = rememberRestorableStateHolder<Screens>()
            holder.withRestorableState(Screens.Screen1) {
                number = remember { increment++ }
                restorableNumber = rememberSavedInstanceState { increment++ }
            }
        }

        rule.runOnIdle {
            assertThat(number).isEqualTo(0)
            assertThat(restorableNumber).isEqualTo(1)
            number = -1
            restorableNumber = -1
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(number).isEqualTo(2)
            assertThat(restorableNumber).isEqualTo(1)
        }
    }

    @Test
    fun switchToScreen2AndRestore() {
        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var numberOnScreen2 = -1
        var restorableNumberOnScreen2 = -1
        restorationTester.setContent {
            val holder = rememberRestorableStateHolder<Screens>()
            holder.withRestorableState(screen) {
                if (screen == Screens.Screen2) {
                    numberOnScreen2 = remember { increment++ }
                    restorableNumberOnScreen2 = rememberSavedInstanceState { increment++ }
                }
            }
        }

        rule.runOnIdle {
            screen = Screens.Screen2
        }

        // wait for the screen switch to apply
        rule.runOnIdle {
            assertThat(numberOnScreen2).isEqualTo(0)
            assertThat(restorableNumberOnScreen2).isEqualTo(1)
            numberOnScreen2 = -1
            restorableNumberOnScreen2 = -1
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(numberOnScreen2).isEqualTo(2)
            assertThat(restorableNumberOnScreen2).isEqualTo(1)
        }
    }

    @Test
    fun stateOfScreen1IsSavedAndRestoredWhileWeAreOnScreen2() {
        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var numberOnScreen1 = -1
        var restorableNumberOnScreen1 = -1
        restorationTester.setContent {
            val holder = rememberRestorableStateHolder<Screens>()
            holder.withRestorableState(screen) {
                if (screen == Screens.Screen1) {
                    numberOnScreen1 = remember { increment++ }
                    restorableNumberOnScreen1 = rememberSavedInstanceState { increment++ }
                } else {
                    // screen 2
                    remember { 100 }
                }
            }
        }

        rule.runOnIdle {
            assertThat(numberOnScreen1).isEqualTo(0)
            assertThat(restorableNumberOnScreen1).isEqualTo(1)
            screen = Screens.Screen2
        }

        // wait for the screen switch to apply
        rule.runOnIdle {
            numberOnScreen1 = -1
            restorableNumberOnScreen1 = -1
        }

        restorationTester.emulateSavedInstanceStateRestore()

        // switch back to screen1
        rule.runOnIdle {
            screen = Screens.Screen1
        }

        rule.runOnIdle {
            assertThat(numberOnScreen1).isEqualTo(2)
            assertThat(restorableNumberOnScreen1).isEqualTo(1)
        }
    }

    @Test
    fun weCanSkipSavingForCurrentScreen() {
        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var restorableStateHolder: RestorableStateHolder<Screens>? = null
        var restorableNumberOnScreen1 = -1
        restorationTester.setContent {
            val holder = rememberRestorableStateHolder<Screens>()
            restorableStateHolder = holder
            holder.withRestorableState(screen) {
                if (screen == Screens.Screen1) {
                    restorableNumberOnScreen1 = rememberSavedInstanceState { increment++ }
                } else {
                    // screen 2
                    remember { 100 }
                }
            }
        }

        rule.runOnIdle {
            assertThat(restorableNumberOnScreen1).isEqualTo(0)
            restorableNumberOnScreen1 = -1
            restorableStateHolder!!.removeState(Screens.Screen1)
            screen = Screens.Screen2
        }

        rule.runOnIdle {
            // switch back to screen1
            screen = Screens.Screen1
        }

        rule.runOnIdle {
            assertThat(restorableNumberOnScreen1).isEqualTo(1)
        }
    }

    @Test
    fun weCanRemoveAlreadySavedState() {
        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var restorableStateHolder: RestorableStateHolder<Screens>? = null
        var restorableNumberOnScreen1 = -1
        restorationTester.setContent {
            val holder = rememberRestorableStateHolder<Screens>()
            restorableStateHolder = holder
            holder.withRestorableState(screen) {
                if (screen == Screens.Screen1) {
                    restorableNumberOnScreen1 = rememberSavedInstanceState { increment++ }
                } else {
                    // screen 2
                    remember { 100 }
                }
            }
        }

        rule.runOnIdle {
            assertThat(restorableNumberOnScreen1).isEqualTo(0)
            restorableNumberOnScreen1 = -1
            screen = Screens.Screen2
        }

        rule.runOnIdle {
            // switch back to screen1
            restorableStateHolder!!.removeState(Screens.Screen1)
            screen = Screens.Screen1
        }

        rule.runOnIdle {
            assertThat(restorableNumberOnScreen1).isEqualTo(1)
        }
    }
}

enum class Screens {
    Screen1,
    Screen2,
}
