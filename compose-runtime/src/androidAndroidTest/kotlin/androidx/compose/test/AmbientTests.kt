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

package androidx.compose.test

import android.widget.TextView
import androidx.compose.Ambient
import androidx.compose.Composable
import androidx.compose.CompositionReference
import androidx.compose.Providers
import androidx.compose.Recomposer
import androidx.compose.StructurallyEqual
import androidx.compose.Untracked
import androidx.compose.ambientOf
import androidx.compose.compositionReference
import androidx.compose.escapeCompose
import androidx.compose.invalidate
import androidx.compose.remember
import androidx.compose.staticAmbientOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.ui.core.LayoutNode
import androidx.ui.core.subcomposeInto
import androidx.ui.node.UiComposer
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Create a normal (dynamic) ambient with a string value
val someTextAmbient = ambientOf { "Default" }

// Create a normal (dynamic) ambient with an int value
val someIntAmbient = ambientOf { 1 }

// Create a non-overridable ambient provider key
private val someOtherIntProvider = ambientOf { 1 }

// Make public the consumer key.
val someOtherIntAmbient: Ambient<Int> =
    someOtherIntProvider

// Create a static ambient with an int value
val someStaticInt = staticAmbientOf { 40 }

@MediumTest
@RunWith(AndroidJUnit4::class)
class AmbientTests : BaseComposeTest() {

    val composer: UiComposer get() = error("should not be called")

    @Composable
    fun Text(value: String, id: Int = 100) {
        TextView(id = id, text = value)
    }

    @Composable
    fun ReadStringAmbient(ambient: Ambient<String>, id: Int = 100) {
        Text(value = ambient.current, id = id)
    }

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
                assertEquals(
                    "Test1",
                    someTextAmbient.current
                )
                assertEquals(12, someIntAmbient.current)
                assertEquals(
                    42,
                    someOtherIntAmbient.current
                )
                assertEquals(50, someStaticInt.current)
                Providers(
                    someTextAmbient provides "Test2",
                    someStaticInt provides 60
                ) {
                    assertEquals(
                        "Test2",
                        someTextAmbient.current
                    )
                    assertEquals(
                        12,
                        someIntAmbient.current
                    )
                    assertEquals(
                        42,
                        someOtherIntAmbient.current
                    )
                    assertEquals(60, someStaticInt.current)
                }
                assertEquals(
                    "Test1",
                    someTextAmbient.current
                )
                assertEquals(12, someIntAmbient.current)
                assertEquals(
                    42,
                    someOtherIntAmbient.current
                )
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
                ReadStringAmbient(
                    ambient = someTextAmbient,
                    id = tvId
                )
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
                ReadStringAmbient(
                    ambient = staticStringAmbient,
                    id = tvId
                )
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
                ReadStringAmbient(ambient = someTextAmbient, id = tvId)

                subCompose {
                    assertEquals(
                        someText,
                        someTextAmbient.current
                    )
                    assertEquals(0, someIntAmbient.current)

                    Providers(
                        someIntAmbient provides 42
                    ) {
                        assertEquals(
                            someText,
                            someTextAmbient.current
                        )
                        assertEquals(
                            42,
                            someIntAmbient.current
                        )
                    }
                }
            }
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)

            someText = "Modified"
            doInvalidate()
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)
        }.done()
    }

    @Test
    fun subCompose_Static() {
        val tvId = 100
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        val staticSomeTextAmbient =
            staticAmbientOf { "Default" }
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

                ReadStringAmbient(
                    ambient = staticSomeTextAmbient,
                    id = tvId
                )

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
        }.done()
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
                ReadStringAmbient(
                    ambient = someTextAmbient,
                    id = tvId
                )

                doSubCompose = deferredSubCompose {
                    assertEquals(
                        someText,
                        someTextAmbient.current
                    )
                    assertEquals(0, someIntAmbient.current)

                    Providers(
                        someIntAmbient provides 42
                    ) {
                        assertEquals(
                            someText,
                            someTextAmbient.current
                        )
                        assertEquals(
                            42,
                            someIntAmbient.current
                        )
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
        }.done()
    }

    @Test
    fun deferredSubCompose_Static() {
        val tvId = 100
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        var someText = "Unmodified"
        var doSubCompose: () -> Unit = { error("Sub-compose callback not set") }
        val staticSomeTextAmbient =
            staticAmbientOf { "Default" }
        val staticSomeIntAmbient = staticAmbientOf { -1 }
        compose {
            invalidates.add(invalidate)

            Providers(
                staticSomeTextAmbient provides someText,
                staticSomeIntAmbient provides 0
            ) {
                assertEquals(someText, staticSomeTextAmbient.current)
                assertEquals(0, staticSomeIntAmbient.current)

                ReadStringAmbient(
                    ambient = staticSomeTextAmbient,
                    id = tvId
                )

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
        }.done()
    }

    @Test
    fun deferredSubCompose_Nested_Static() {
        val tvId = 100
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        var someText = "Unmodified"
        var doSubCompose1: () -> Unit = { error("Sub-compose-1 callback not set") }
        var doSubCompose2: () -> Unit = { error("Sub-compose-2 callback not set") }
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

                ReadStringAmbient(ambient = staticSomeTextAmbient, id = tvId)

                doSubCompose1 = deferredSubCompose {

                    assertEquals(someText, staticSomeTextAmbient.current)
                    assertEquals(0, staticSomeIntAmbient.current)

                    doSubCompose2 = deferredSubCompose {
                        assertEquals(someText, staticSomeTextAmbient.current)
                        assertEquals(0, staticSomeIntAmbient.current)
                    }
                }
            }
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)
            doSubCompose1()
        }.then {
            doSubCompose2()
        }.then {
            someText = "Modified"
            doInvalidate()
        }.then {
            assertEquals(someText, it.findViewById<TextView>(tvId).text)

            doSubCompose1()
        }.then {
            doSubCompose2()
        }.done()
    }

    @Test
    fun insertShouldSeePreviouslyProvidedValues() {
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        val someStaticString =
            staticAmbientOf { "Default" }
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

    @Test
    fun providingANewDataClassValueShouldNotRecompose() {
        val invalidates = mutableListOf<() -> Unit>()
        fun doInvalidate() = invalidates.forEach { it() }.also { invalidates.clear() }
        val someDataAmbient =
            ambientOf(StructurallyEqual) { SomeData() }
        var composed = false

        @Composable
        fun ReadSomeDataAmbient(ambient: Ambient<SomeData>, id: Int = 100) {
            composed = true
            Text(value = ambient.current.value, id = id)
        }

        compose {
            Observe {
                invalidates.add(invalidate)
                Providers(
                    someDataAmbient provides SomeData("provided")
                ) {
                    ReadSomeDataAmbient(someDataAmbient)
                }
            }
        }.then {
            assertTrue(composed)
            composed = false

            doInvalidate()
        }.then {
            assertFalse(composed)
        }
    }

    @After
    fun ensureNoSubcomposePending() {
        activityRule.activity.uiThread {
            val hadPendingChanges = Recomposer.current().hasPendingChanges()
            assertTrue(!hadPendingChanges, "Pending changes detected after test completed")
        }
    }

    class Ref<T : Any> {
        lateinit var value: T
    }

    @Composable fun narrowInvalidateForReference(ref: Ref<CompositionReference>) {
        ref.value = compositionReference()
    }

    @Composable fun deferredSubCompose(block: @Composable () -> Unit): () -> Unit {
        val container = remember { escapeCompose { LayoutNode() } }
        val ref = Ref<CompositionReference>()
        narrowInvalidateForReference(ref = ref)
        return {
            // TODO(b/150390669): Review use of @Untracked
            subcomposeInto(
                activityRule.activity,
                container,
                Recomposer.current(),
                ref.value
            ) @Untracked {
                block()
            }
        }
    }
}

private data class SomeData(val value: String = "default")
