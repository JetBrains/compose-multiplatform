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

package androidx.compose.runtime

import androidx.compose.runtime.mock.MockViewValidator
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.validate
import kotlin.test.Test

class RestartTests {
    @Test
    fun restart_PersonModel_lambda() = compositionTest {
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )

        compose {
            RestartGroup {
                Text(president.name)
                Text("${president.age}")
            }
        }

        fun validate() {
            validate {
                RestartGroup {
                    Text(president.name)
                    Text("${president.age}")
                }
            }
        }
        validate()

        president.name = PRESIDENT_NAME_16
        president.age = PRESIDENT_AGE_16

        expectChanges()
        validate()
    }

    @Test
    fun restart_PersonModel_lambda_parameters() = compositionTest {
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )

        compose {
            Repeat(5) {
                Text(president.name)
                Text(president.age.toString())
            }
        }

        fun validate() {
            validate {
                Repeat(5) {
                    Text(president.name)
                    Text(president.age.toString())
                }
            }
        }
        validate()

        president.name = PRESIDENT_NAME_16
        president.age = PRESIDENT_AGE_16

        expectChanges()
        validate()
    }

    @Test
    fun restart_PersonModel_function() = compositionTest {
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )

        @Composable fun PersonView() {
            Text(president.name)
            Text(president.age.toString())
        }

        compose {
            PersonView()
        }

        fun validate() {
            validate {
                Text(president.name)
                Text(president.age.toString())
            }
        }
        validate()

        president.name = PRESIDENT_NAME_16
        president.age = PRESIDENT_AGE_16

        expectChanges()
        validate()
    }

    @Test
    fun restart_State_delete() = compositionTest {
        val state = mutableStateOf(true)

        @Composable fun ShowSomething() {
            Text("State = ${state.value}")
        }

        @Composable fun View() {
            if (state.value) {
                // This is not correct code generation as this should be called in a call function, however, this
                // tests the assumption that calling a function should produce an item (a key followed by a group).
                ShowSomething()
            }
        }

        compose {
            View()
        }

        fun validate() {
            validate {
                if (state.value) {
                    Text("State = ${state.value}")
                }
            }
        }
        validate()

        state.value = false
        expectChanges()
        validate()

        state.value = true
        expectChanges()
        validate()
    }

    @Test
    fun restart_PersonModel_function_parameters() = compositionTest {
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )

        @Composable fun PersonView(index: Int) {
            Text(index.toString())
            Text(president.name)
            Text(president.age.toString())
        }

        compose {
            Repeat(5) { index ->
                PersonView(index)
            }
        }

        fun validate() {
            validate {
                Repeat(5) { index ->
                    Text(index.toString())
                    Text(president.name)
                    Text(president.age.toString())
                }
            }
        }
        validate()

        president.name = PRESIDENT_NAME_16
        president.age = PRESIDENT_AGE_16
        expectChanges()
        validate()
    }
}

@Composable
fun RestartGroup(content: @Composable () -> Unit) {
    content()
}

@Suppress("unused")
inline fun MockViewValidator.RestartGroup(block: () -> Unit) = block()

@Composable
fun Repeat(count: Int, block: @Composable (index: Int) -> Unit) {
    for (i in 0 until count) {
        block(i)
    }
}

@Suppress("unused")
inline fun MockViewValidator.Repeat(count: Int, block: (index: Int) -> Unit) = repeat(count, block)