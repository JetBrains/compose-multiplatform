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

@file:OptIn(ExperimentalComposeApi::class)
package androidx.compose.runtime

import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Create a normal (dynamic) CompositionLocal with a string value
val someTextCompositionLocal = compositionLocalOf { "Default" }

// Create a normal (dynamic) CompositionLocal with an int value
val someIntCompositionLocal = compositionLocalOf { 1 }

// Create a non-overridable CompositionLocal provider key
private val someOtherIntProvider = compositionLocalOf { 1 }

// Make public the consumer key.
val someOtherIntCompositionLocal: CompositionLocal<Int> = someOtherIntProvider

// Create a static CompositionLocal with an int value
val someStaticInt = staticCompositionLocalOf { 40 }

@MediumTest
@RunWith(AndroidJUnit4::class)
class CompositionLocalTests : BaseComposeTest() {

    @Composable
    @Suppress("Deprecation", "UNUSED_PARAMETER")
    fun Text(value: String, id: Int = 100) {}

    @Composable
    fun ReadStringCompositionLocal(compositionLocal: CompositionLocal<String>, id: Int = 100) {
        Text(value = compositionLocal.current, id = id)
    }

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun testCompositionLocalApi() {
        compose {
            assertEquals("Default", someTextCompositionLocal.current)
            assertEquals(1, someIntCompositionLocal.current)
            Providers(
                someTextCompositionLocal provides "Test1",
                someIntCompositionLocal provides 12,
                someOtherIntProvider provides 42,
                someStaticInt provides 50
            ) {
                assertEquals(
                    "Test1",
                    someTextCompositionLocal.current
                )
                assertEquals(12, someIntCompositionLocal.current)
                assertEquals(
                    42,
                    someOtherIntCompositionLocal.current
                )
                assertEquals(50, someStaticInt.current)
                Providers(
                    someTextCompositionLocal provides "Test2",
                    someStaticInt provides 60
                ) {
                    assertEquals(
                        "Test2",
                        someTextCompositionLocal.current
                    )
                    assertEquals(
                        12,
                        someIntCompositionLocal.current
                    )
                    assertEquals(
                        42,
                        someOtherIntCompositionLocal.current
                    )
                    assertEquals(60, someStaticInt.current)
                }
                assertEquals(
                    "Test1",
                    someTextCompositionLocal.current
                )
                assertEquals(12, someIntCompositionLocal.current)
                assertEquals(
                    42,
                    someOtherIntCompositionLocal.current
                )
                assertEquals(50, someStaticInt.current)
            }
            assertEquals("Default", someTextCompositionLocal.current)
            assertEquals(1, someIntCompositionLocal.current)
        }.then {
            // Force the composition to run
        }
    }

    @Test
    @Ignore("b/179279455")
    fun recompose_Dynamic() {
        val tvId = 100
        val invalidates = mutableListOf<RecomposeScope>()
        fun doInvalidate() = invalidates.forEach { it.invalidate() }.also { invalidates.clear() }
        var someText = "Unmodified"
        compose {
            invalidates.add(currentRecomposeScope)
            Providers(
                someTextCompositionLocal provides someText
            ) {
                ReadStringCompositionLocal(
                    compositionLocal = someTextCompositionLocal,
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
    @Ignore("b/179279455")
    fun recompose_Static() {
        val tvId = 100
        val invalidates = mutableListOf<RecomposeScope>()
        fun doInvalidate() = invalidates.forEach { it.invalidate() }.also { invalidates.clear() }
        val staticStringCompositionLocal = staticCompositionLocalOf { "Default" }
        var someText = "Unmodified"
        compose {
            invalidates.add(currentRecomposeScope)
            Providers(
                staticStringCompositionLocal provides someText
            ) {
                ReadStringCompositionLocal(
                    compositionLocal = staticStringCompositionLocal,
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
    @Ignore("b/179279455")
    fun subCompose_Dynamic() {
        val tvId = 100
        val invalidates = mutableListOf<RecomposeScope>()
        fun doInvalidate() = invalidates.forEach { it.invalidate() }.also { invalidates.clear() }
        var someText = "Unmodified"
        compose {
            invalidates.add(currentRecomposeScope)

            Providers(
                someTextCompositionLocal provides someText,
                someIntCompositionLocal provides 0
            ) {
                ReadStringCompositionLocal(compositionLocal = someTextCompositionLocal, id = tvId)

                subCompose {
                    assertEquals(
                        someText,
                        someTextCompositionLocal.current
                    )
                    assertEquals(0, someIntCompositionLocal.current)

                    Providers(
                        someIntCompositionLocal provides 42
                    ) {
                        assertEquals(
                            someText,
                            someTextCompositionLocal.current
                        )
                        assertEquals(
                            42,
                            someIntCompositionLocal.current
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
    @Ignore("b/179279455")
    fun subCompose_Static() {
        val tvId = 100
        val invalidates = mutableListOf<RecomposeScope>()
        fun doInvalidate() = invalidates.forEach { it.invalidate() }.also { invalidates.clear() }
        val staticSomeTextCompositionLocal = staticCompositionLocalOf { "Default" }
        val staticSomeIntCompositionLocal = staticCompositionLocalOf { -1 }
        var someText = "Unmodified"
        compose {
            invalidates.add(currentRecomposeScope)

            Providers(
                staticSomeTextCompositionLocal provides someText,
                staticSomeIntCompositionLocal provides 0
            ) {
                assertEquals(someText, staticSomeTextCompositionLocal.current)
                assertEquals(0, staticSomeIntCompositionLocal.current)

                ReadStringCompositionLocal(
                    compositionLocal = staticSomeTextCompositionLocal,
                    id = tvId
                )

                subCompose {
                    assertEquals(someText, staticSomeTextCompositionLocal.current)
                    assertEquals(0, staticSomeIntCompositionLocal.current)

                    Providers(
                        staticSomeIntCompositionLocal provides 42
                    ) {
                        assertEquals(someText, staticSomeTextCompositionLocal.current)
                        assertEquals(42, staticSomeIntCompositionLocal.current)
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
    @Ignore("b/179279455")
    fun deferredSubCompose_Dynamic() {
        val tvId = 100
        val invalidates = mutableListOf<RecomposeScope>()
        fun doInvalidate() = invalidates.forEach { it.invalidate() }.also { invalidates.clear() }
        var someText = "Unmodified"
        var doSubCompose: () -> Unit = { error("Sub-compose callback not set") }
        compose {
            invalidates.add(currentRecomposeScope)

            Providers(
                someTextCompositionLocal provides someText,
                someIntCompositionLocal provides 0
            ) {
                ReadStringCompositionLocal(
                    compositionLocal = someTextCompositionLocal,
                    id = tvId
                )

                doSubCompose = deferredSubCompose {
                    assertEquals(
                        someText,
                        someTextCompositionLocal.current
                    )
                    assertEquals(0, someIntCompositionLocal.current)

                    Providers(
                        someIntCompositionLocal provides 42
                    ) {
                        assertEquals(
                            someText,
                            someTextCompositionLocal.current
                        )
                        assertEquals(
                            42,
                            someIntCompositionLocal.current
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
    @Ignore("b/179279455")
    fun deferredSubCompose_Static() {
        val tvId = 100
        val invalidates = mutableListOf<RecomposeScope>()
        fun doInvalidate() = invalidates.forEach { it.invalidate() }.also { invalidates.clear() }
        var someText = "Unmodified"
        var doSubCompose: () -> Unit = { error("Sub-compose callback not set") }
        val staticSomeTextCompositionLocal = staticCompositionLocalOf { "Default" }
        val staticSomeIntCompositionLocal = staticCompositionLocalOf { -1 }
        compose {
            invalidates.add(currentRecomposeScope)

            Providers(
                staticSomeTextCompositionLocal provides someText,
                staticSomeIntCompositionLocal provides 0
            ) {
                assertEquals(someText, staticSomeTextCompositionLocal.current)
                assertEquals(0, staticSomeIntCompositionLocal.current)

                ReadStringCompositionLocal(
                    compositionLocal = staticSomeTextCompositionLocal,
                    id = tvId
                )

                doSubCompose = deferredSubCompose {

                    assertEquals(someText, staticSomeTextCompositionLocal.current)
                    assertEquals(0, staticSomeIntCompositionLocal.current)

                    Providers(
                        staticSomeIntCompositionLocal provides 42
                    ) {
                        assertEquals(someText, staticSomeTextCompositionLocal.current)
                        assertEquals(42, staticSomeIntCompositionLocal.current)
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
    @Ignore("b/179279455")
    fun deferredSubCompose_Nested_Static() {
        val tvId = 100
        val invalidates = mutableListOf<RecomposeScope>()
        fun doInvalidate() = invalidates.forEach { it.invalidate() }.also { invalidates.clear() }
        var someText = "Unmodified"
        var doSubCompose1: () -> Unit = { error("Sub-compose-1 callback not set") }
        var doSubCompose2: () -> Unit = { error("Sub-compose-2 callback not set") }
        val staticSomeTextCompositionLocal = staticCompositionLocalOf { "Default" }
        val staticSomeIntCompositionLocal = staticCompositionLocalOf { -1 }
        compose {
            invalidates.add(currentRecomposeScope)

            Providers(
                staticSomeTextCompositionLocal provides someText,
                staticSomeIntCompositionLocal provides 0
            ) {
                assertEquals(someText, staticSomeTextCompositionLocal.current)
                assertEquals(0, staticSomeIntCompositionLocal.current)

                ReadStringCompositionLocal(
                    compositionLocal = staticSomeTextCompositionLocal,
                    id = tvId
                )

                doSubCompose1 = deferredSubCompose {

                    assertEquals(someText, staticSomeTextCompositionLocal.current)
                    assertEquals(0, staticSomeIntCompositionLocal.current)

                    doSubCompose2 = deferredSubCompose {
                        assertEquals(someText, staticSomeTextCompositionLocal.current)
                        assertEquals(0, staticSomeIntCompositionLocal.current)
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
    @Ignore("b/179279455")
    fun insertShouldSeePreviouslyProvidedValues() {
        val invalidates = mutableListOf<RecomposeScope>()
        fun doInvalidate() = invalidates.forEach { it.invalidate() }.also { invalidates.clear() }
        val someStaticString = staticCompositionLocalOf { "Default" }
        var shouldRead = false
        compose {
            Providers(
                someStaticString provides "Provided A"
            ) {
                Observe {
                    invalidates.add(currentRecomposeScope)
                    if (shouldRead)
                        ReadStringCompositionLocal(someStaticString)
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
    @Ignore("b/179279455")
    fun providingANewDataClassValueShouldNotRecompose() {
        val invalidates = mutableListOf<RecomposeScope>()
        fun doInvalidate() = invalidates.forEach { it.invalidate() }.also { invalidates.clear() }
        val someDataCompositionLocal = compositionLocalOf(structuralEqualityPolicy()) { SomeData() }
        var composed = false

        @Composable
        fun ReadSomeDataCompositionLocal(
            compositionLocal: CompositionLocal<SomeData>,
            id: Int = 100
        ) {
            composed = true
            Text(value = compositionLocal.current.value, id = id)
        }

        compose {
            Observe {
                invalidates.add(currentRecomposeScope)
                Providers(
                    someDataCompositionLocal provides SomeData("provided")
                ) {
                    ReadSomeDataCompositionLocal(someDataCompositionLocal)
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
            val hasInvalidations = Recomposer.runningRecomposers.value.any { it.hasPendingWork }
            assertFalse(hasInvalidations, "Pending changes detected after test completed")
        }
    }

    class Ref<T : Any> {
        lateinit var value: T
    }

    @Composable fun narrowInvalidateForReference(ref: Ref<CompositionContext>) {
        ref.value = rememberCompositionContext()
    }

    @Suppress("UNUSED_PARAMETER")
    @Composable fun deferredSubCompose(block: @Composable () -> Unit): () -> Unit {
//        val container = remember { View(activity) }
        val ref = Ref<CompositionContext>()
        narrowInvalidateForReference(ref = ref)
        return {
//            @OptIn(ExperimentalComposeApi::class)
//            Composition(
//                container,
//                UiApplier(container),
//                ref.value
//            ).apply {
//                setContent {
//                    block()
//                }
//            }
        }
    }
}

private data class SomeData(val value: String = "default")
