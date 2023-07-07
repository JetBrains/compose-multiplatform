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

package androidx.compose.runtime.saveable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RestorationInVariousScenariosTest {

    @get:Rule
    val rule = createComposeRule()

    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun insideForLoop() {
        val states = arrayOfNulls<MutableState<Int>>(2)
        restorationTester.setContent {
            for (i in 0..1) {
                states[i] = rememberSaveable { mutableStateOf(0) }
            }
        }

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(0)
            assertThat(states[1]!!.value).isEqualTo(0)

            states[0]!!.value = 1
            states[1]!!.value = 2

            // we null it to ensure recomposition happened
            states[0] = null
            states[1] = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(1)
            assertThat(states[1]!!.value).isEqualTo(2)
        }
    }

    @Test
    fun insideForLoop_withKey() {
        val states = arrayOfNulls<MutableState<Int>>(2)
        restorationTester.setContent {
            for (i in 0..1) {
                key(i) {
                    states[i] = rememberSaveable { mutableStateOf(0) }
                }
            }
        }

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(0)
            assertThat(states[1]!!.value).isEqualTo(0)

            states[0]!!.value = 1
            states[1]!!.value = 2

            // we null it to ensure recomposition happened
            states[0] = null
            states[1] = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(1)
            assertThat(states[1]!!.value).isEqualTo(2)
        }
    }

    @Test
    fun insideForLoop_withExtraFunction() {
        val states = arrayOfNulls<MutableState<Int>>(2)
        restorationTester.setContent {
            for (i in 0..1) {
                FunctionWithState(states, i)
            }
        }

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(0)
            assertThat(states[1]!!.value).isEqualTo(0)

            states[0]!!.value = 1
            states[1]!!.value = 2

            // we null it to ensure recomposition happened
            states[0] = null
            states[1] = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(1)
            assertThat(states[1]!!.value).isEqualTo(2)
        }
    }

    @Test
    fun changingLoopCountWithExtraStateAfter() {
        var number = 2
        val statesInLoop = arrayOfNulls<MutableState<Int>?>(2)
        var stateOutside: MutableState<String>? = null
        restorationTester.setContent {
            repeat(number) {
                statesInLoop[it] = rememberSaveable { mutableStateOf(0) }
            }
            stateOutside = rememberSaveable { mutableStateOf("0") }
        }

        rule.runOnIdle {
            statesInLoop[0]!!.value = 1
            statesInLoop[0] = null
            stateOutside!!.value = "1"
            stateOutside = null
            number = 1
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(statesInLoop[0]?.value).isEqualTo(1)
            assertThat(stateOutside?.value).isEqualTo("1")
        }
    }

    @Test
    fun twoStates() {
        val states = arrayOfNulls<MutableState<Int>>(2)
        restorationTester.setContent {
            states[0] = rememberSaveable { mutableStateOf(0) }
            states[1] = rememberSaveable { mutableStateOf(0) }
        }

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(0)
            assertThat(states[1]!!.value).isEqualTo(0)

            states[0]!!.value = 1
            states[1]!!.value = 2

            // we null it to ensure recomposition happened
            states[0] = null
            states[1] = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(1)
            assertThat(states[1]!!.value).isEqualTo(2)
        }
    }

    @Test
    fun twoStates_firstStateIsConditional() {
        var needFirst = true
        val states = arrayOfNulls<MutableState<Int>>(2)
        restorationTester.setContent {
            if (needFirst) {
                states[0] = rememberSaveable { mutableStateOf(0) }
            }
            states[1] = rememberSaveable { mutableStateOf(0) }
        }

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(0)
            assertThat(states[1]!!.value).isEqualTo(0)

            states[1]!!.value = 1

            // we null it to ensure recomposition happened
            states[0] = null
            states[1] = null

            needFirst = false
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread {
            assertThat(states[0]).isNull()
            assertThat(states[1]!!.value).isEqualTo(1)
        }
    }

    @Test
    fun twoStates_withExtraFunction() {
        val states = arrayOfNulls<MutableState<Int>>(2)
        restorationTester.setContent {
            FunctionWithState(states, 0)
            FunctionWithState(states, 1)
        }

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(0)
            assertThat(states[1]!!.value).isEqualTo(0)

            states[0]!!.value = 1
            states[1]!!.value = 2

            // we null it to ensure recomposition happened
            states[0] = null
            states[1] = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(1)
            assertThat(states[1]!!.value).isEqualTo(2)
        }
    }

    @Test
    fun twoStates_withExtraFunction_firstStateIsConditional() {
        var needFirst = true
        val states = arrayOfNulls<MutableState<Int>>(2)
        restorationTester.setContent {
            if (needFirst) {
                FunctionWithState(states, 0)
            }
            FunctionWithState(states, 1)
        }

        rule.runOnUiThread {
            assertThat(states[0]!!.value).isEqualTo(0)
            assertThat(states[1]!!.value).isEqualTo(0)

            states[1]!!.value = 1

            // we null it to ensure recomposition happened
            states[0] = null
            states[1] = null

            needFirst = false
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread {
            assertThat(states[0]).isNull()
            assertThat(states[1]!!.value).isEqualTo(1)
        }
    }

    @Composable
    fun FunctionWithState(states: Array<MutableState<Int>?>, index: Int) {
        states[index] = rememberSaveable { mutableStateOf(0) }
    }
}
