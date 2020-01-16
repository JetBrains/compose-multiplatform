/*
 * Copyright 2019 The Android Open Source Project
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
@file:Suppress("PLUGIN_ERROR")
package androidx.compose

import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class RestartTests: BaseComposeTest() {
    @After
    fun teardown() {
        Compose.clearRoots()
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

        @Suppress("PLUGIN_WARNING")
        compose {
            call(147, { true }) {

                RestartGroup(object : Function0<Unit> {
                    override fun invoke() {
                        startRestartGroup(54)
                        emit(93, { context -> TextView(context).apply { id = tvIdName } }) {
                            set(president.name) { text = it }
                        }
                        emit(94, { context -> TextView(context).apply { id = tvIdAge } }) {
                            set(president.age) { text = it.toString() }
                        }
                        endRestartGroup()?.updateScope(this)
                    }
                })
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

        @Suppress("PLUGIN_WARNING")
        compose {
            call(147, { true }) {

                Repeat(5, object : Function1<Int, Unit> {
                    override fun invoke(index: Int) {
                        startRestartGroup(98)
                        emit(93, { context -> TextView(context).apply {
                            id = tvIdNameBase + index
                        } }) {
                            set(president.name) { text = it }
                        }
                        emit(94, { context -> TextView(context).apply {
                            id = tvIdAgeBase + index
                        } }) {
                            set(president.age) { text = it.toString() }
                        }
                        endRestartGroup()?.updateScope { this(index) }
                    }
                })
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

        fun ViewComposer.PersonView() {
            startRestartGroup(145)
            emit(93, { context -> TextView(context).apply { id = tvIdName } }) {
                set(president.name) { text = it }
            }
            emit(94, { context -> TextView(context).apply { id = tvIdAge } }) {
                set(president.age) { text = it.toString() }
            }
            endRestartGroup()?.updateScope { PersonView() }
        }

        compose {
            call(145, { true }) {
                PersonView()
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
    fun restart_State_delete() {
        val tvStateId = 101
        val state = frame {
            mutableStateOf(true)
        }

        fun ViewComposer.ShowSomething() {
            startRestartGroup(183)
            emit(181, { context -> TextView(context).apply { id = tvStateId } }) {
                set("State = ${state.value}") { text = it }
            }
            endRestartGroup()?.updateScope { ShowSomething() }
        }

        fun ViewComposer.View() {
            startRestartGroup(191)
            if (state.value) {
                // This is not correct code generation as this should be called in a call function, however, this
                // tests the assumption that calling a function should produce an item (a key followed by a group).
                ShowSomething()
            }
            endRestartGroup()?.updateScope { View() }
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

        fun ViewComposer.PersonView(index: Int) {
            startRestartGroup(231)
            emit(93, { context -> TextView(context).apply { id = tvIdNameBase + index } }) {
                set(president.name) { text = it }
            }
            emit(94, { context -> TextView(context).apply { id = tvIdAgeBase + index } }) {
                set(president.age) { text = it.toString() }
            }
            endRestartGroup()?.updateScope { PersonView(index) }
        }

        @Suppress("PLUGIN_WARNING")
        compose {
            call(147, { true }) {
                Repeat(5) { index ->
                    PersonView(index)
                }
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
fun RestartGroup(block: () -> Unit) {
    block()
}

@Composable
fun Repeat(count: Int, block: (index: Int) -> Unit) {
    for (i in 0 until count) {
        block(i)
    }
}