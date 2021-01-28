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

import androidx.compose.runtime.MutableState
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
class RememberSaveableWithMutableStateTest {

    @get:Rule
    val rule = createComposeRule()

    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun simpleRestore() {
        var state: MutableState<Int>? = null
        restorationTester.setContent {
            state = rememberSaveable { mutableStateOf(0) }
        }

        rule.runOnUiThread {
            assertThat(state!!.value).isEqualTo(0)

            state!!.value = 1
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread {
            assertThat(state!!.value).isEqualTo(1)
        }
    }

    @Test
    fun restoreWithSaver() {
        var state: MutableState<Holder>? = null
        restorationTester.setContent {
            state = rememberSaveable(stateSaver = HolderSaver) {
                mutableStateOf(Holder(0))
            }
        }

        rule.runOnIdle {
            assertThat(state!!.value).isEqualTo(Holder(0))

            state!!.value.value = 1
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(state!!.value).isEqualTo(Holder(1))
        }
    }

    @Test
    fun nullableStateRestoresNonNullValue() {
        var state: MutableState<String?>? = null
        restorationTester.setContent {
            state = rememberSaveable { mutableStateOf(null) }
        }

        rule.runOnUiThread {
            assertThat(state!!.value).isNull()

            state!!.value = "value"
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread {
            assertThat(state!!.value).isEqualTo("value")
        }
    }

    @Test
    fun nullableStateRestoresNullValue() {
        var state: MutableState<String?>? = null
        restorationTester.setContent {
            state = rememberSaveable { mutableStateOf("initial") }
        }

        rule.runOnUiThread {
            assertThat(state!!.value).isEqualTo("initial")

            state!!.value = null
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnUiThread {
            assertThat(state!!.value).isNull()
        }
    }
}
