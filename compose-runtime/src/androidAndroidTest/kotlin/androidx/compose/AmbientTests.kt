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

package androidx.compose

import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Create a normal (dynamic) ambient with a string value
val someTextAmbient = ambientOf { "Default" }

// Create a normal (dynamic) ambient with an int value
val someIntAmbient = ambientOf { 1 }

// Create a non-overridable ambient provider key
private val someOtherIntProvider = ambientOf { 1 }

// Make public the consumer key.
val someOtherIntAmbient: Ambient<Int> = someOtherIntProvider

// Create a static ambient with an int value
val someStaticInt = staticAmbientOf { 40 }

@RunWith(AndroidJUnit4::class)
@Suppress("PLUGIN_ERROR")
class AmbientTests : BaseComposeTest() {

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun testAmbientApi() {
        compose {
            assertEquals("Default", someTextAmbient.current)
            assertEquals(1, someIntAmbient.current)
            Providers(
                someTextAmbient provides "Test1",
                someIntAmbient provides 12,
                someOtherIntProvider provides 42,
                someStaticInt provides 50
            ) {
                assertEquals("Test1", someTextAmbient.current)
                assertEquals(12, someIntAmbient.current)
                assertEquals(42, someOtherIntAmbient.current)
                assertEquals(50, someStaticInt.current)
                Providers(someTextAmbient provides "Test2", someStaticInt provides 60) {
                    assertEquals("Test2", someTextAmbient.current)
                    assertEquals(12, someIntAmbient.current)
                    assertEquals(42, someOtherIntAmbient.current)
                    assertEquals(60, someStaticInt.current)
                }
                assertEquals("Test1", someTextAmbient.current)
                assertEquals(12, someIntAmbient.current)
                assertEquals(42, someOtherIntAmbient.current)
                assertEquals(50, someStaticInt.current)
            }
            assertEquals("Default", someTextAmbient.current)
            assertEquals(1, someIntAmbient.current)
        }.then {
            // Force the composition to run
        }
    }

    @Test
    fun recompose_Dynamic() {
        val tvId = 100
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        var someText = "Unmodified"
        compose {
            invalidates.add(invalidate)
            Providers(
                someTextAmbient provides someText
            ) {
                call(107, { false }) {
                    ReadStringAmbient(ambient = someTextAmbient, id = tvId)
                }
            }
        }.then { activity ->
            assertEquals(someText, activity.findViewById<TextView>(100).text)

            someText = "Modified"
            doInvalidate()
        }.then { activity ->
            assertEquals(someText, activity.findViewById<TextView>(100).text)
        }
    }

    @Test
    fun recompose_Static() {
        val tvId = 100
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        val staticStringAmbient = staticAmbientOf { "Default" }
        var someText = "Unmodified"
        compose {
            invalidates.add(invalidate)
            Providers(
                staticStringAmbient provides someText
            ) {
                call(114, { false }) {
                    ReadStringAmbient(ambient = staticStringAmbient, id = tvId)
                }
            }
        }.then { activity ->
            assertEquals(someText, activity.findViewById<TextView>(100).text)

            someText = "Modified"
            doInvalidate()
        }.then { activity ->
            assertEquals(someText, activity.findViewById<TextView>(100).text)
        }
    }

    @Test
    fun subCompose_Dynamic() {
        val tvId = 100
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        var someText = "Unmodified"
        compose {
            invalidates.add(invalidate)

            Providers(
                someTextAmbient provides someText,
                someIntAmbient provides 0
            ) {
                call(150, { false }) {
                    ReadStringAmbient(ambient = someTextAmbient, id = tvId)
                }

                subCompose {
                    assertEquals(someText, someTextAmbient.current)
                    assertEquals(0, someIntAmbient.current)

                    Providers(
                        someIntAmbient provides 42
                    ) {
                        assertEquals(someText, someTextAmbient.current)
                        assertEquals(42, someIntAmbient.current)
                    }
                }
            }
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)

            someText = "Modified"
            doInvalidate()
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)
        }
    }

    @Test
    fun subCompose_Static() {
        val tvId = 100
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        val staticSomeTextAmbient = staticAmbientOf { "Default" }
        val staticSomeIntAmbient = staticAmbientOf { -1 }
        var someText = "Unmodified"
        compose {
            invalidates.add(invalidate)

            Providers(
                staticSomeTextAmbient provides someText,
                staticSomeIntAmbient provides 0
            ) {
                assertEquals(someText, staticSomeTextAmbient.current)
                assertEquals(0, staticSomeIntAmbient.current)

                call(150, { false }) {
                    ReadStringAmbient(ambient = staticSomeTextAmbient, id = tvId)
                }

                subCompose {
                    assertEquals(someText, staticSomeTextAmbient.current)
                    assertEquals(0, staticSomeIntAmbient.current)

                    Providers(
                        staticSomeIntAmbient provides 42
                    ) {
                        assertEquals(someText, staticSomeTextAmbient.current)
                        assertEquals(42, staticSomeIntAmbient.current)
                    }
                }
            }
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)

            someText = "Modified"
            doInvalidate()
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)
        }
    }

    @Test
    fun deferredSubCompose_Dynamic() {
        val tvId = 100
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        var someText = "Unmodified"
        var doSubCompose: () -> Unit = { error("Sub-compose callback not set") }
        compose {
            invalidates.add(invalidate)

            Providers(
                someTextAmbient provides someText,
                someIntAmbient provides 0
            ) {
                call(150, { false }) {
                    ReadStringAmbient(ambient = someTextAmbient, id = tvId)
                }

                doSubCompose = deferredSubCompose {
                    assertEquals(someText, someTextAmbient.current)
                    assertEquals(0, someIntAmbient.current)

                    Providers(
                        someIntAmbient provides 42
                    ) {
                        assertEquals(someText, someTextAmbient.current)
                        assertEquals(42, someIntAmbient.current)
                    }
                }
            }
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)
            doSubCompose()

            someText = "Modified"
            doInvalidate()
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)

            doSubCompose()
        }
    }

    @Test
    fun deferredSubCompose_Static() {
        val tvId = 100
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        var someText = "Unmodified"
        var doSubCompose: () -> Unit = { error("Sub-compose callback not set") }
        val staticSomeTextAmbient = staticAmbientOf { "Default" }
        val staticSomeIntAmbient = staticAmbientOf { -1 }
        compose {
            invalidates.add(invalidate)

            Providers(
                staticSomeTextAmbient provides someText,
                staticSomeIntAmbient provides 0
            ) {
                assertEquals(someText, staticSomeTextAmbient.current)
                assertEquals(0, staticSomeIntAmbient.current)

                call(150, { false }) {
                    ReadStringAmbient(ambient = staticSomeTextAmbient, id = tvId)
                }

                doSubCompose = deferredSubCompose {
                    assertEquals(someText, staticSomeTextAmbient.current)
                    assertEquals(0, staticSomeIntAmbient.current)

                    Providers(
                        staticSomeIntAmbient provides 42
                    ) {
                        assertEquals(someText, staticSomeTextAmbient.current)
                        assertEquals(42, staticSomeIntAmbient.current)
                    }
                }
            }
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)
            doSubCompose()

            someText = "Modified"
            doInvalidate()
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)

            doSubCompose()
        }
    }

    @Test
    fun insertShouldSeePreviouslyProvidedValues() {
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        val someStaticString = staticAmbientOf<String> { "Default" }
        var shouldRead = false
        compose {
            Providers(
                someStaticString provides "Provided A"
            ) {
                Observe {
                    invalidates.add(invalidate)
                    if (shouldRead)
                        ReadStringAmbient(someStaticString)
                }
            }
        }.then {
            assertEquals(null, it.findViewById<TextView?>(100))
            shouldRead = true
            doInvalidate()
        }.then {
            assertEquals("Provided A", it.findViewById<TextView>(100).text)
        }
    }

    @After
    fun ensureNoSubcomposePending() {
        activityRule.activity.uiThread {
            assertTrue(
                !Recomposer.hasPendingChanges(),
                "Pending changes detected after test completed"
            )
        }
    }

    fun ViewComposer.subCompose(block: ViewComposer.() -> Unit) {
        startRestartGroup(139)
        val container = remember { Container() }
        val reference = composer.buildReference()
        Compose.subcomposeInto(container, activityRule.activity, reference) {
            (currentComposerNonNull as ViewComposer).block()
        }
        endRestartGroup()?.updateScope { subCompose(block) }
    }

    fun ViewComposer.deferredSubCompose(block: ViewComposer.() -> Unit): () -> Unit {
        startRestartGroup(234)
        val container = remember { Container() }
        val reference = composer.buildReference()
        endRestartGroup()?.updateScope { deferredSubCompose(block) }
        return {
            Compose.subcomposeInto(container, activityRule.activity, reference) {
                (currentComposerNonNull as ViewComposer).block()
            }
        }
    }
}

@Suppress("PLUGIN_ERROR")
@Composable
fun ViewComposer.Text(value: String, id: Int = 100) {
    startRestartGroup(87)
    emit(
        168,
        { context -> TextView(context).apply { this.id = id } },
        { set(value) { text = it } }
    )
    endRestartGroup()?.updateScope { Text(value, id) }
}

@Suppress("PLUGIN_ERROR")
@Composable
fun ViewComposer.ReadStringAmbient(ambient: Ambient<String>, id: Int = 100) {
    startRestartGroup(80)
    Text(value = ambient.current, id = id)
    endRestartGroup()?.updateScope { ReadStringAmbient(ambient = ambient, id = id) }
}

class Container : Emittable {
    val children = mutableListOf<Emittable>()

    override fun emitInsertAt(index: Int, instance: Emittable) {
        children.add(index, instance)
    }

    override fun emitRemoveAt(index: Int, count: Int) {
        children.subList(index, index + count).clear()
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
        val toMove = children.subList(from, from + count)
        val nodes = toMove.toMutableList()
        toMove.clear()
        if (from < to) children.addAll(to - count, nodes)
        else children.addAll(to, nodes)
    }
}