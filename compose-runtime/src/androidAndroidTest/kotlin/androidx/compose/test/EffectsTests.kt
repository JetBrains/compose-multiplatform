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
@file:Suppress("PLUGIN_ERROR")
package androidx.compose.test

import android.widget.Button
import android.widget.TextView
import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.State
import androidx.compose.ambientOf
import androidx.compose.clearRoots
import androidx.compose.getValue
import androidx.compose.invalidate
import androidx.compose.mutableStateOf
import androidx.compose.onCommit
import androidx.compose.onDispose
import androidx.compose.onPreCommit
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.ui.node.UiComposer
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class EffectsTests : BaseComposeTest() {

    val composer: UiComposer get() = error("should not be called")

    @After
    fun teardown() {
        clearRoots()
    }

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun testMemoization1() {
        var inc = 0

        compose {
            remember { ++inc }
        }.then { _ ->
            assertEquals(1, inc)
        }.then { _ ->
            assertEquals(1, inc)
        }
    }

    @Test
    fun testMemoization2() {
        var calculations = 0
        var compositions = 0
        var calculation = 0
        var key = 0
        val trigger = Trigger()

        compose {
            trigger.subscribe()
            compositions++
            calculation = remember(key) { 100 * ++calculations }
        }.then { _ ->
            assertEquals(1, calculations)
            assertEquals(100, calculation)
            assertEquals(1, compositions)
            trigger.recompose()
        }.then { _ ->
            assertEquals(1, calculations)
            assertEquals(100, calculation)
            assertEquals(2, compositions)
            key++
            trigger.recompose()
        }.then { _ ->
            assertEquals(2, calculations)
            assertEquals(200, calculation)
            assertEquals(3, compositions)
            trigger.recompose()
        }.then { _ ->
            assertEquals(2, calculations)
            assertEquals(200, calculation)
            assertEquals(4, compositions)
            key++
            trigger.recompose()
        }.then { _ ->
            assertEquals(3, calculations)
            assertEquals(300, calculation)
            assertEquals(5, compositions)
        }
    }

    @Test
    fun testState1() {
        val tv1Id = 100
        var inc = 0
        var local = mutableStateOf("invalid")

        compose {
            local = state { "Hello world! ${inc++}" }
            TextView(id = tv1Id, text = local.value)
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("Hello world! 0", helloText.text)
            assertEquals(local.value, helloText.text)
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("Hello world! 0", helloText.text)
            assertEquals(local.value, helloText.text)

            local.value = "New string"
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("New string", helloText.text)
            assertEquals(local.value, helloText.text)
        }
    }

    @Test
    fun testState2() {
        val tv1Id = 100
        val tv2Id = 200
        var local1 = mutableStateOf("invalid")
        var local2 = mutableStateOf("invalid")

        compose {
            local1 = state { "First" }
            local2 = state { "Second" }
            TextView(id = tv1Id, text = local1.value)
            TextView(id = tv2Id, text = local2.value)
        }.then { activity ->
            val tv1 = activity.findViewById(tv1Id) as TextView
            val tv2 = activity.findViewById(tv2Id) as TextView
            assertEquals("First", tv1.text)
            assertEquals("Second", tv2.text)
            assertEquals(local1.value, tv1.text)
            assertEquals(local2.value, tv2.text)
        }.then { activity ->
            val tv1 = activity.findViewById(tv1Id) as TextView
            val tv2 = activity.findViewById(tv2Id) as TextView
            assertEquals("First", tv1.text)
            assertEquals("Second", tv2.text)
            assertEquals(local1.value, tv1.text)
            assertEquals(local2.value, tv2.text)

            local1.value = "New First"
        }.then { activity ->
            val tv1 = activity.findViewById(tv1Id) as TextView
            val tv2 = activity.findViewById(tv2Id) as TextView
            assertEquals("New First", tv1.text)
            assertEquals("Second", tv2.text)
            assertEquals(local1.value, tv1.text)
            assertEquals(local2.value, tv2.text)
        }
    }

    @Test
    fun testState3() {
        // Test property delegation for State/MutableState
        val initial = "initial"
        val expected = "expected"
        val myState = mutableStateOf(initial)
        val readonly: State<String> = myState
        val reader by readonly
        var writer by myState

        frame {
            writer = expected

            assertEquals("state object after write", expected, myState.value)
            assertEquals("reader after write", expected, reader)
            assertEquals("writer after write", expected, writer)
        }
    }

    @Test
    fun testPreCommit1() {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            log("Unmountable:start")
            onPreCommit {
                log("onPreCommit")
                onDispose {
                    log("onDispose")
                }
            }
            log("Unmountable:end")
        }

        compose {
            log("compose:start")
            if (mount) {
                Unmountable()
            }
            log("compose:end")
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "compose:start",
                    "Unmountable:start",
                    "Unmountable:end",
                    "compose:end",
                    "onPreCommit"
                ),
                logHistory
            )
            mount = false
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "compose:start",
                    "Unmountable:start",
                    "Unmountable:end",
                    "compose:end",
                    "onPreCommit",
                    "compose:start",
                    "compose:end",
                    "onDispose"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testPreCommit2() {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            onPreCommit {
                log("onPreCommit:a2")
                onDispose {
                    log("onDispose:a2")
                }
            }
            onPreCommit {
                log("onPreCommit:b2")
                onDispose {
                    log("onDispose:b2")
                }
            }
        }

        compose {
            onPreCommit {
                log("onPreCommit:a1")
                onDispose {
                    log("onDispose:a1")
                }
            }
            if (mount) {
                Unmountable()
            }
            onPreCommit {
                log("onPreCommit:b1")
                onDispose {
                    log("onDispose:b1")
                }
            }
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "onPreCommit:a1",
                    "onPreCommit:a2",
                    "onPreCommit:b2",
                    "onPreCommit:b1"
                ),
                logHistory
            )
            mount = false
            log("recompose")
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "onPreCommit:a1",
                    "onPreCommit:a2",
                    "onPreCommit:b2",
                    "onPreCommit:b1",
                    "recompose",
                    "onDispose:b2",
                    "onDispose:a2",
                    "onDispose:b1",
                    "onDispose:a1",
                    "onPreCommit:a1",
                    "onPreCommit:b1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testPreCommit3() {
        var x = 0
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            trigger.subscribe()
            onPreCommit {
                val y = x++
                log("onPreCommit:$y")
                onDispose {
                    log("dispose:$y")
                }
            }
        }.then { _ ->
            log("recompose")
            trigger.recompose()
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "onPreCommit:0",
                    "recompose",
                    "dispose:0",
                    "onPreCommit:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testPreCommit31() {
        var a = 0
        var b = 0
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            trigger.subscribe()
            onPreCommit {
                val y = a++
                log("onPreCommit a:$y")
                onDispose {
                    log("dispose a:$y")
                }
            }
            onPreCommit {
                val y = b++
                log("onPreCommit b:$y")
                onDispose {
                    log("dispose b:$y")
                }
            }
        }.then { _ ->
            log("recompose")
            trigger.recompose()
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "onPreCommit a:0",
                    "onPreCommit b:0",
                    "recompose",
                    "dispose b:0",
                    "dispose a:0",
                    "onPreCommit a:1",
                    "onPreCommit b:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testPreCommit4() {
        var x = 0
        var key = 123
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            trigger.subscribe()
            onPreCommit(key) {
                val y = x++
                log("onPreCommit:$y")
                onDispose {
                    log("dispose:$y")
                }
            }
        }.then { _ ->
            log("recompose")
            trigger.recompose()
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "onPreCommit:0",
                    "recompose"
                ),
                logHistory
            )
            log("recompose (key -> 345)")
            key = 345
            trigger.recompose()
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "onPreCommit:0",
                    "recompose",
                    "recompose (key -> 345)",
                    "dispose:0",
                    "onPreCommit:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testPreCommit5() {
        var a = 0
        var b = 0
        var c = 0
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Sub() {
            trigger.subscribe()
            onPreCommit {
                val y = c++
                log("onPreCommit c:$y")
                onDispose {
                    log("dispose c:$y")
                }
            }
        }

        compose {
            trigger.subscribe()
            onPreCommit {
                val y = a++
                log("onPreCommit a:$y")
                onDispose {
                    log("dispose a:$y")
                }
            }

            onPreCommit {
                val y = b++
                log("onPreCommit b:$y")
                onDispose {
                    log("dispose b:$y")
                }
            }

            Sub()
        }.then { _ ->
            log("recompose")
            trigger.recompose()
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "onPreCommit a:0",
                    "onPreCommit b:0",
                    "onPreCommit c:0",
                    "recompose",
                    "dispose c:0",
                    "dispose b:0",
                    "dispose a:0",
                    "onPreCommit a:1",
                    "onPreCommit b:1",
                    "onPreCommit c:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testPreCommit6() {
        var readValue = 0

        @Composable
        fun UpdateStateInPreCommit() {
            var value by state { 1 }
            readValue = value
            onPreCommit {
                value = 2
            }
        }

        compose {
            UpdateStateInPreCommit()
        }.then { _ ->
            assertEquals(2, readValue)
        }
    }

    @Test
    fun testOnDispose1() {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun DisposeLogger(msg: String) {
            onDispose { log(msg) }
        }

        compose {
            DisposeLogger(msg = "onDispose:1")
            if (mount) {
                DisposeLogger(msg = "onDispose:2")
            }
        }.then { _ ->
            assertArrayEquals(
                emptyList<String>(),
                logHistory
            )
            mount = false
            log("recompose")
        }.then { _ ->
            assertArrayEquals(
                listOf("recompose", "onDispose:2"),
                logHistory
            )
        }
    }

    @Test
    fun testOnCommit1() {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            log("Unmountable:start")
            onCommit {
                log("onCommit 1")
                onDispose {
                    log("onDispose 1")
                }
            }
            onPreCommit {
                log("onPreCommit 2")
                onDispose {
                    log("onDispose 2")
                }
            }
            onCommit {
                log("onCommit 3")
                onDispose {
                    log("onDispose 3")
                }
            }
            log("Unmountable:end")
        }

        compose {
                log("compose:start")
                if (mount) {
                    Unmountable()
                }
                log("compose:end")
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "compose:start",
                    "Unmountable:start",
                    "Unmountable:end",
                    "compose:end",
                    "onPreCommit 2",
                    "onCommit 1",
                    "onCommit 3"
                ),
                logHistory
            )
            mount = false
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "compose:start",
                    "Unmountable:start",
                    "Unmountable:end",
                    "compose:end",
                    "onPreCommit 2",
                    "onCommit 1",
                    "onCommit 3",
                    "compose:start",
                    "compose:end",
                    "onDispose 3",
                    "onDispose 2",
                    "onDispose 1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testAmbient1() {
        val tv1Id = 100

        val Foo = ambientOf<String>()
        var current by mutableStateOf("Hello World")

        @Composable
        fun Bar() {
            val foo = Foo.current
            TextView(id = tv1Id, text = foo)
        }

        compose {
            Providers(Foo provides current) {
                Bar()
            }
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals(current, helloText.text)
            current = "abcd"
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals(current, helloText.text)
        }
    }

    @Test
    fun testAmbient2() {
        val MyAmbient = ambientOf<Int> { throw Exception("not set") }

        var requestRecompose: (() -> Unit)? = null
        var ambientValue = 1

        @Composable fun SimpleComposable2() {
            val value = MyAmbient.current
            TextView(text = "$value")
        }

        @Composable fun SimpleComposable() {
            requestRecompose = invalidate
            Providers(MyAmbient provides ambientValue++) {
                SimpleComposable2()
                Button(id = 123)
            }
        }

        @Composable fun Root() {
            SimpleComposable()
        }

        var firstButton: Button? = null

        compose {
            Root()
        }.then {
            firstButton = it.findViewById<Button>(123)
            assertTrue("Expected button to be created", firstButton != null)
            requestRecompose?.invoke()
        }.then {
            assertEquals(
                "Expected button to not be recreated",
                it.findViewById<Button>(123),
                firstButton
            )
        }
    }

    @Test
    fun testAmbient_RecomposeScope() {
        val MyAmbient = ambientOf<Int> { throw Exception("not set") }

        var requestRecompose: (() -> Unit)? = null
        var componentComposed = false
        var ambientValue = 1

        @Composable fun SimpleComposable2() {
            componentComposed = true
            val value = MyAmbient.current
            TextView(text = "$value")
        }

        @Composable fun SimpleComposable() {
            requestRecompose = invalidate
            Providers(MyAmbient provides ambientValue++) {
                SimpleComposable2()
                Button(id = 123)
            }
        }

        @Composable fun Root() {
            SimpleComposable()
        }

        var firstButton: Button? = null

        compose {
            Root()
        }.then {
            assertTrue("Expected component to be composed", componentComposed)
            firstButton = it.findViewById<Button>(123)
            assertTrue("Expected button to be created", firstButton != null)
            componentComposed = false
            requestRecompose?.invoke()
        }.then {
            assertTrue("Expected component to be composed", componentComposed)

            assertEquals(
                "Expected button to not be recreated",
                firstButton,
                it.findViewById<Button>(123)
            )
        }
    }

    @Test
    fun testUpdatedComposition() {
        val tv1Id = 100
        var inc = 0

        compose {
            val local = state { "Hello world! ${inc++}" }
            TextView(id = tv1Id, text = local.value)
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("Hello world! 0", helloText.text)
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("Hello world! 0", helloText.text)
        }
    }
}

fun <T> assertArrayEquals(
    expected: Collection<T>,
    actual: Collection<T>,
    transform: (T) -> String = { "$it" }
) {
    assertEquals(
        expected.joinToString("\n", transform = transform),
        actual.joinToString("\n", transform = transform)
    )
}

class Trigger() {
    val count = mutableStateOf(0)
    fun subscribe() { count.value }
    fun recompose() { count.value += 1 }
}