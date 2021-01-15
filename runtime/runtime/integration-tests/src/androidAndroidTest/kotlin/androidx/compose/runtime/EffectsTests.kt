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
package androidx.compose.runtime

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class EffectsTests : BaseComposeTest() {

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
            local = remember { mutableStateOf("Hello world! ${inc++}") } // NOTYPO
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
            local1 = remember { mutableStateOf("First") }
            local2 = remember { mutableStateOf("Second") }
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

        writer = expected

        assertEquals("state object after write", expected, myState.value)
        assertEquals("reader after write", expected, reader)
        assertEquals("writer after write", expected, writer)
    }

    @Test
    fun testCommit1() {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            log("Unmountable:start")
            onCommit {
                log("onCommit")
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
                    "onCommit"
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
                    "onCommit",
                    "compose:start",
                    "compose:end",
                    "onDispose"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testCommit2() {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            onCommit {
                log("onCommit:a2")
                onDispose {
                    log("onDispose:a2")
                }
            }
            onCommit {
                log("onCommit:b2")
                onDispose {
                    log("onDispose:b2")
                }
            }
        }

        compose {
            onCommit {
                log("onCommit:a1")
                onDispose {
                    log("onDispose:a1")
                }
            }
            if (mount) {
                Unmountable()
            }
            onCommit {
                log("onCommit:b1")
                onDispose {
                    log("onDispose:b1")
                }
            }
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "onCommit:a1",
                    "onCommit:a2",
                    "onCommit:b2",
                    "onCommit:b1"
                ),
                logHistory
            )
            mount = false
            log("recompose")
        }.then { _ ->
            assertArrayEquals(
                listOf(
                    "onCommit:a1",
                    "onCommit:a2",
                    "onCommit:b2",
                    "onCommit:b1",
                    "recompose",
                    "onDispose:b2",
                    "onDispose:a2",
                    "onDispose:b1",
                    "onDispose:a1",
                    "onCommit:a1",
                    "onCommit:b1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testCommit3() {
        var x = 0
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            trigger.subscribe()
            onCommit {
                val y = x++
                log("onCommit:$y")
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
                    "onCommit:0",
                    "recompose",
                    "dispose:0",
                    "onCommit:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testCommit31() {
        var a = 0
        var b = 0
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            trigger.subscribe()
            onCommit {
                val y = a++
                log("onCommit a:$y")
                onDispose {
                    log("dispose a:$y")
                }
            }
            onCommit {
                val y = b++
                log("onCommit b:$y")
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
                    "onCommit a:0",
                    "onCommit b:0",
                    "recompose",
                    "dispose b:0",
                    "dispose a:0",
                    "onCommit a:1",
                    "onCommit b:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testCommit4() {
        var x = 0
        var key = 123
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            trigger.subscribe()
            onCommit(key) {
                val y = x++
                log("onCommit:$y")
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
                    "onCommit:0",
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
                    "onCommit:0",
                    "recompose",
                    "recompose (key -> 345)",
                    "dispose:0",
                    "onCommit:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testCommit5() {
        var a = 0
        var b = 0
        var c = 0
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Sub() {
            trigger.subscribe()
            onCommit {
                val y = c++
                log("onCommit c:$y")
                onDispose {
                    log("dispose c:$y")
                }
            }
        }

        compose {
            trigger.subscribe()
            onCommit {
                val y = a++
                log("onCommit a:$y")
                onDispose {
                    log("dispose a:$y")
                }
            }

            onCommit {
                val y = b++
                log("onCommit b:$y")
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
                    "onCommit a:0",
                    "onCommit b:0",
                    "onCommit c:0",
                    "recompose",
                    "dispose c:0",
                    "dispose b:0",
                    "dispose a:0",
                    "onCommit a:1",
                    "onCommit b:1",
                    "onCommit c:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testCommit6() {
        var readValue = 0

        @Composable
        fun UpdateStateInCommit() {
            var value by remember { mutableStateOf(1) }
            readValue = value
            onCommit {
                value = 2
            }
        }

        compose {
            UpdateStateInCommit()
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

        var scope: RecomposeScope? = null
        var ambientValue = 1

        @Composable fun SimpleComposable2() {
            val value = MyAmbient.current
            TextView(text = "$value")
        }

        @Composable fun SimpleComposable() {
            scope = currentRecomposeScope
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
            scope?.invalidate()
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

        var scope: RecomposeScope? = null
        var componentComposed = false
        var ambientValue = 1

        @Composable fun SimpleComposable2() {
            componentComposed = true
            val value = MyAmbient.current
            TextView(text = "$value")
        }

        @Composable fun SimpleComposable() {
            scope = currentRecomposeScope
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
            scope?.invalidate()
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
            val local = remember { mutableStateOf("Hello world! ${inc++}") } // NOTYPO
            TextView(id = tv1Id, text = local.value)
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("Hello world! 0", helloText.text)
        }.then { activity ->
            val helloText = activity.findViewById(tv1Id) as TextView
            assertEquals("Hello world! 0", helloText.text)
        }
    }

    @Test // regression test for 165416179
    fun recomposeNewModifiedState() {
        val id = 100
        var enabler by mutableStateOf(false)
        compose {
            // "enabler" forces the test into the recompose code path
            // rather than initial composition
            if (enabler) {
                var state by remember { mutableStateOf(0) }
                onCommit(Unit) {
                    state = 1
                }
                TextView(id = id, text = "$state")
                Button(
                    id = id + 1,
                    text = "click me",
                    onClickListener = View.OnClickListener {
                        state++
                        println("Setting state to $state")
                    }
                )
            }
        }.then { activity ->
            assertNull(
                "Text present in initial composition",
                activity.findViewById(id)
            )
            enabler = true
        }.then { activity ->
            val text = (activity.findViewById(id) as? TextView)?.text

            // Text could be either "0" or "1" here depending on timing. It is most likely still
            // "0" but could be "1" if the recompose implied by the onCommit lands before this
            // code runs.
            assertTrue(text == "0" || text == "1")
        }.then { activity ->
            // Here this should run after the recompose lands.
            assertEquals(
                "1",
                (activity.findViewById(id) as? TextView)?.text
            )

            // Cause the state to advance.
            (activity.findViewById<Button>(id + 1)?.callOnClick())
        }.then { activity ->
            assertEquals(
                "2",
                (activity.findViewById(id) as? TextView)?.text
            )
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