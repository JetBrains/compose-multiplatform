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

import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.mock.validate
import kotlin.test.Test
import kotlin.test.assertEquals

class EffectsTests {
    private val NeverEqualObject = object {
        override fun equals(other: Any?): Boolean {
            return false
        }
    }

    @Test
    fun testMemoization1() = compositionTest {
        var inc = 0

        compose {
            // User the remembered value to avoid a lint error.
            val someInt = remember {
                ++inc
            }
            println(someInt)
        }

        assertEquals(1, inc)
        expectNoChanges()
        assertEquals(1, inc)
    }

    @Test
    fun testMemoization2() = compositionTest {
        var calculations = 0
        var compositions = 0
        var calculation = 0
        var key = 0
        val trigger = Trigger()

        compose {
            trigger.subscribe()
            compositions++
            calculation = remember(key) { 100 * ++calculations }
        }

        assertEquals(1, calculations)
        assertEquals(100, calculation)
        assertEquals(1, compositions)
        trigger.recompose()
        expectNoChanges()

        assertEquals(1, calculations)
        assertEquals(100, calculation)
        assertEquals(2, compositions)
        key++
        trigger.recompose()
        expectChanges()

        assertEquals(2, calculations)
        assertEquals(200, calculation)
        assertEquals(3, compositions)
        trigger.recompose()
        expectNoChanges()

        assertEquals(2, calculations)
        assertEquals(200, calculation)
        assertEquals(4, compositions)
        key++
        trigger.recompose()
        expectChanges()

        assertEquals(3, calculations)
        assertEquals(300, calculation)
        assertEquals(5, compositions)
    }

    @Test
    fun testState1() = compositionTest {
        var inc = 0
        var local = mutableStateOf("invalid")

        compose {
            local = remember { mutableStateOf("Hello world! ${inc++}") } // NOTYPO
            Text(local.value)
        }

        fun validate() {
            validate {
                Text(local.value)
            }
        }
        validate()

        expectNoChanges()
        validate()

        local.value = "New string"

        expectChanges()
        validate()
    }

    @Test
    fun testState2() = compositionTest {
        var local1 = mutableStateOf("invalid")
        var local2 = mutableStateOf("invalid")

        compose {
            local1 = remember { mutableStateOf("First") }
            local2 = remember { mutableStateOf("Second") }
            Text(local1.value)
            Text(local2.value)
        }

        fun validate() {
            validate {
                Text(local1.value)
                Text(local2.value)
            }
        }
        validate()

        expectNoChanges()
        validate()

        local1.value = "New First"
        expectChanges()
        validate()
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

        assertEquals(expected, myState.value, "state object after write")
        assertEquals(expected, reader, "reader after write")
        assertEquals(expected, writer, "writer after write")
    }

    @Test
    fun testCommit1() = compositionTest {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            log("Unmountable:start")
            DisposableEffect(NeverEqualObject) {
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
        }

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
        expectChanges()
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

    @Test
    fun testCommit2() = compositionTest {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            DisposableEffect(NeverEqualObject) {
                log("onCommit:a2")
                onDispose {
                    log("onDispose:a2")
                }
            }
            DisposableEffect(NeverEqualObject) {
                log("onCommit:b2")
                onDispose {
                    log("onDispose:b2")
                }
            }
        }

        compose {
            DisposableEffect(NeverEqualObject) {
                log("onCommit:a1")
                onDispose {
                    log("onDispose:a1")
                }
            }
            if (mount) {
                Unmountable()
            }
            DisposableEffect(NeverEqualObject) {
                log("onCommit:b1")
                onDispose {
                    log("onDispose:b1")
                }
            }
        }

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
        expectChanges()

        assertArrayEquals(
            listOf(
                "onCommit:a1",
                "onCommit:a2",
                "onCommit:b2",
                "onCommit:b1",
                "recompose",
                "onDispose:b1",
                "onDispose:b2",
                "onDispose:a2",
                "onDispose:a1",
                "onCommit:a1",
                "onCommit:b1"
            ),
            logHistory
        )
    }

    @Test
    fun testCommit3() = compositionTest {
        var x = 0
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            trigger.subscribe()
            DisposableEffect(NeverEqualObject) {
                val y = x++
                log("onCommit:$y")
                onDispose {
                    log("dispose:$y")
                }
            }
        }

        log("recompose")
        trigger.recompose()
        expectChanges()
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

    @Test
    fun testCommit31() = compositionTest {
        var a = 0
        var b = 0
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            trigger.subscribe()
            DisposableEffect(NeverEqualObject) {
                val y = a++
                log("onCommit a:$y")
                onDispose {
                    log("dispose a:$y")
                }
            }
            DisposableEffect(NeverEqualObject) {
                val y = b++
                log("onCommit b:$y")
                onDispose {
                    log("dispose b:$y")
                }
            }
        }

        log("recompose")
        trigger.recompose()
        expectChanges()
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

    @Test
    fun testCommit4() = compositionTest {
        var x = 0
        var key = 123
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            trigger.subscribe()
            DisposableEffect(key) {
                val y = x++
                log("onCommit:$y")
                onDispose {
                    log("dispose:$y")
                }
            }
        }

        log("recompose")
        trigger.recompose()
        expectNoChanges()

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
        expectChanges()

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

    @Test
    fun testCommit5() = compositionTest {
        var a = 0
        var b = 0
        var c = 0
        val trigger = Trigger()

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Sub() {
            trigger.subscribe()
            DisposableEffect(NeverEqualObject) {
                val y = c++
                log("onCommit c:$y")
                onDispose {
                    log("dispose c:$y")
                }
            }
        }

        compose {
            trigger.subscribe()
            DisposableEffect(NeverEqualObject) {
                val y = a++
                log("onCommit a:$y")
                onDispose {
                    log("dispose a:$y")
                }
            }

            DisposableEffect(NeverEqualObject) {
                val y = b++
                log("onCommit b:$y")
                onDispose {
                    log("dispose b:$y")
                }
            }

            Sub()
        }

        log("recompose")
        trigger.recompose()
        expectChanges()
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

    @Test
    fun testCommit6() = compositionTest {
        var readValue = 0

        @Composable
        fun UpdateStateInCommit() {
            var value by remember { mutableStateOf(1) }
            readValue = value
            SideEffect {
                value = 2
            }
        }

        compose {
            UpdateStateInCommit()
        }
        expectChanges()

        assertEquals(2, readValue)
    }

    @Test
    fun testOnDispose1() = compositionTest {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun DisposeLogger(msg: String) {
            DisposableEffect(Unit) {
                onDispose { log(msg) }
            }
        }

        compose {
            DisposeLogger(msg = "onDispose:1")
            if (mount) {
                DisposeLogger(msg = "onDispose:2")
            }
        }

        assertArrayEquals(
            emptyList(),
            logHistory
        )
        mount = false
        log("recompose")
        expectChanges()
        assertArrayEquals(
            listOf("recompose", "onDispose:2"),
            logHistory
        )
    }

    @Test
    fun testCompositionLocal1() = compositionTest {
        val Foo = compositionLocalOf<String> { error("Not provided") }
        var current by mutableStateOf("Hello World")

        @Composable
        fun Bar() {
            val foo = Foo.current
            Text(foo)
        }

        compose {
            CompositionLocalProvider(Foo provides current) {
                Bar()
            }
        }

        fun validate() {
            validate {
                Text(current)
            }
        }
        validate()

        current = "abcd"
        expectChanges()
        validate()
    }

    @Test
    fun testCompositionLocal2() = compositionTest {
        val MyCompositionLocal = compositionLocalOf<Int> { throw Exception("not set") }

        var scope: RecomposeScope? = null
        var compositionLocalValue = 1

        @Composable fun SimpleComposable2() {
            val value = MyCompositionLocal.current
            Text("$value")
        }

        @Composable fun SimpleComposable() {
            scope = currentRecomposeScope
            CompositionLocalProvider(MyCompositionLocal provides compositionLocalValue++) {
                SimpleComposable2()
                Text("Other")
            }
        }

        @Composable fun Root() {
            SimpleComposable()
        }

        compose {
            Root()
        }

        fun validate() {
            validate {
                Text("${compositionLocalValue - 1}")
                Text("Other")
            }
        }
        validate()
        scope?.invalidate()
        expectChanges()
        validate()
    }

    @Test
    fun testCompositionLocal_RecomposeScope() = compositionTest {
        val MyCompositionLocal = compositionLocalOf<Int> { throw Exception("not set") }

        var scope: RecomposeScope? = null
        var compositionLocalValue = 1

        @Composable fun SimpleComposable2() {
            val value = MyCompositionLocal.current
            Text("$value")
        }

        @Composable fun SimpleComposable() {
            scope = currentRecomposeScope
            CompositionLocalProvider(MyCompositionLocal provides compositionLocalValue++) {
                SimpleComposable2()
                Text("Other")
            }
        }

        @Composable fun Root() {
            SimpleComposable()
        }

        compose {
            Root()
        }

        fun validate() {
            validate {
                Text("${compositionLocalValue - 1}")
                Text("Other")
            }
        }
        validate()

        scope?.invalidate()
        expectChanges()
        validate()
    }

    @Test
    fun testUpdatedComposition() = compositionTest {
        var inc = 0

        compose {
            val local = remember { mutableStateOf("Hello world! ${inc++}") } // NOTYPO
            Text(local.value)
        }

        validate {
            Text("Hello world! 0")
        }
        expectNoChanges()
        validate {
            Text("Hello world! 0")
        }
    }

    @Test // regression test for 165416179
    fun recomposeNewModifiedState() = compositionTest {
        var enabler by mutableStateOf(false)
        var advance = { }
        compose {
            // "enabler" forces the test into the recompose code path
            // rather than initial composition
            if (enabler) {
                var state by remember { mutableStateOf(0) }
                DisposableEffect(Unit) {
                    state = 1
                    onDispose { }
                }
                Text("$state")
                Text("Some text")
                advance = {
                    state++
                }
            }
        }

        fun validate(state: Int) {
            validate {
                if (enabler) {
                    Text("$state")
                    Text("Some text")
                }
            }
        }
        validate(0)
        enabler = true
        expectChanges()
        validate(0)
        advance()
        expectChanges()
        validate(2)
        advance()
        expectChanges()
        validate(3)
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

class Trigger {
    val count = mutableStateOf(0)
    fun subscribe() { count.value }
    fun recompose() { count.value += 1 }
}
