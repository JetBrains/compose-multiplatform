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
@file:Suppress("PLUGIN_ERROR", "DEPRECATION")
package androidx.compose.test

import android.os.Debug
import android.widget.TextView
import androidx.compose.Composable
import androidx.compose.Direct
import androidx.compose.clearRoots
import androidx.compose.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.ui.node.UiComposer
import junit.framework.TestCase
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@MediumTest
@RunWith(AndroidJUnit4::class)
class RestartTests : BaseComposeTest() {

    val composer: UiComposer get() = error("should not be called")

    @After
    fun teardown() {
        clearRoots()
    }

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun restart_PersonModel_lambda() {
        val tvIdName = 90
        val tvIdAge = 91
        val president = frame {
            Person(
                PRESIDENT_NAME_1,
                PRESIDENT_AGE_1
            )
        }

        compose {
            RestartGroup {
                TextView(id = tvIdName, text = president.name)
                TextView(id = tvIdAge, text = "${president.age}")
            }
        }.then {
            val tvName = it.findViewById(tvIdName) as TextView
            val tvAge = it.findViewById(tvIdAge) as TextView
            TestCase.assertEquals(PRESIDENT_NAME_1, tvName.text)
            TestCase.assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)

            president.name = PRESIDENT_NAME_16
            president.age = PRESIDENT_AGE_16
        }.then {
            val tvName = it.findViewById(tvIdName) as TextView
            val tvAge = it.findViewById(tvIdAge) as TextView
            TestCase.assertEquals(PRESIDENT_NAME_16, tvName.text)
            TestCase.assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
        }
    }

    @Test
    fun restart_PersonModel_lambda_parameters() {
        val tvIdNameBase = 90
        val tvIdAgeBase = 100
        val president = frame {
            Person(
                PRESIDENT_NAME_1,
                PRESIDENT_AGE_1
            )
        }

        compose {
            Repeat(5) { index ->
                TextView(id = tvIdNameBase + index, text = president.name)
                TextView(id = tvIdAgeBase + index, text = president.age.toString())
            }
        }.then { activity ->
            repeat(5) { index ->
                val tvName = activity.findViewById(tvIdNameBase + index) as TextView
                val tvAge = activity.findViewById(tvIdAgeBase + index) as TextView
                TestCase.assertEquals(PRESIDENT_NAME_1, tvName.text)
                TestCase.assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)
            }

            president.name = PRESIDENT_NAME_16
            president.age = PRESIDENT_AGE_16
        }.then { activity ->
            repeat(5) { index ->
                val tvName = activity.findViewById(tvIdNameBase + index) as TextView
                val tvAge = activity.findViewById(tvIdAgeBase + index) as TextView
                TestCase.assertEquals(PRESIDENT_NAME_16, tvName.text)
                TestCase.assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
            }
        }
    }

    @Test
    fun restart_PersonModel_function() {
        val tvIdName = 90
        val tvIdAge = 91
        val president = frame {
            Person(
                PRESIDENT_NAME_1,
                PRESIDENT_AGE_1
            )
        }

        @Composable fun PersonView() {
            TextView(id = tvIdName, text = president.name)
            TextView(id = tvIdAge, text = president.age.toString())
        }

        compose {
            PersonView()
        }.then {
            val tvName = it.findViewById(tvIdName) as TextView
            val tvAge = it.findViewById(tvIdAge) as TextView
            TestCase.assertEquals(PRESIDENT_NAME_1, tvName.text)
            TestCase.assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)

            president.name = PRESIDENT_NAME_16
            president.age = PRESIDENT_AGE_16
        }.then {
            val tvName = it.findViewById(tvIdName) as TextView
            val tvAge = it.findViewById(tvIdAge) as TextView
            TestCase.assertEquals(PRESIDENT_NAME_16, tvName.text)
            TestCase.assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
        }
    }

    @Test
    fun allocation_Test() {
        allocationCounting {
            compose {
                Nothing()
                limitAllocations(2) {
                    Nothing()
                }
                limitAllocations(1) {
                    DirectNothing()
                }
            }.then {
                // Nothing to do
            }
        }
    }

    @Test
    fun restart_State_delete() {
        val tvStateId = 101
        val state = frame {
            mutableStateOf(true)
        }

        @Composable fun ShowSomething() {
            TextView(id = tvStateId, text = "State = ${state.value}")
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
        }.then { activity ->
            val tvState = activity.findViewById<TextView>(tvStateId)
            assertEquals("State = true", tvState.text)

            state.value = false
        }.then { activity ->
            val tvState = activity.findViewById<TextView?>(tvStateId)

            assertNull(tvState)

            state.value = true
        }.then { activity ->
            val tvState = activity.findViewById<TextView>(tvStateId)
            assertEquals("State = true", tvState.text)
        }
    }

    @Test
    fun restart_PersonModel_function_parameters() {
        val tvIdNameBase = 90
        val tvIdAgeBase = 100
        val president = frame {
            Person(
                PRESIDENT_NAME_1,
                PRESIDENT_AGE_1
            )
        }

        @Composable fun PersonView(index: Int) {
            TextView(id = tvIdNameBase + index, text = president.name)
            TextView(id = tvIdAgeBase + index, text = president.age.toString())
        }

        compose {
            Repeat(5) { index ->
                PersonView(index)
            }
        }.then { activity ->
            repeat(5) { index ->
                val tvName = activity.findViewById(tvIdNameBase + index) as TextView
                val tvAge = activity.findViewById(tvIdAgeBase + index) as TextView
                TestCase.assertEquals(PRESIDENT_NAME_1, tvName.text)
                TestCase.assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)
            }

            president.name = PRESIDENT_NAME_16
            president.age = PRESIDENT_AGE_16
        }.then { activity ->
            repeat(5) { index ->
                val tvName = activity.findViewById(tvIdNameBase + index) as TextView
                val tvAge = activity.findViewById(tvIdAgeBase + index) as TextView
                TestCase.assertEquals(PRESIDENT_NAME_16, tvName.text)
                TestCase.assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
            }
        }
    }
}

@Composable
fun RestartGroup(block: @Composable () -> Unit) {
    block()
}

@Composable
fun Repeat(count: Int, block: @Composable (index: Int) -> Unit) {
    for (i in 0 until count) {
        block(i)
    }
}

@Composable
fun Nothing() {
}

@Composable
@Direct
fun DirectNothing() {
}

inline fun <T> allocationCounting(block: () -> T): T {
    Debug.startAllocCounting()
    try {
        return block()
    } finally {
        Debug.stopAllocCounting()
    }
}

inline fun countAllocations(block: () -> Unit): Int {
    val start = Debug.getGlobalAllocCount()
    block()
    val end = Debug.getGlobalAllocCount()
    return end - start
}

inline fun limitAllocations(limit: Int, block: () -> Unit) {
    val count = countAllocations(block)
    assertTrue(count <= limit, "Exceeded allocation limit of $limit by allocation $count")
}