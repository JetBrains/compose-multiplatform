/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.runtime.reflect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mock.EmptyApplier
import androidx.compose.runtime.withRunningRecomposer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

@Composable
private fun composableFunction() {
}

private fun nonComposableFunction() {
}

@Suppress("UNUSED_PARAMETER")
private fun nonComposableFunctionWithComposerParam(unused: Composer) {
}

@Composable
private fun composableFunctionWithDefaults(
    s1: String,
    s2: String,
    s3: String = "a",
    s4: String = "a",
    s5: String = "a"
): String { return s1 + s2 + s3 + s4 + s5 }

@Composable
private fun overloadedComposable() {
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun overloadedComposable(s: String) {
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun overloadedComposable(
    v1: String,
    v2: String,
    v3: String,
    v4: String,
    v5: String,
    v6: String,
    v7: String,
    v8: String,
    v9: String,
    v10: String
) { }

@Suppress("UNUSED_PARAMETER")
@Composable
private fun overloadedComposable(
    v1: String,
    v2: String,
    v3: String,
    v4: String,
    v5: String,
    v6: String,
    v7: String,
    v8: String,
    v9: String,
    v10: String,
    v11: String
) { }

@Suppress("UNUSED_PARAMETER")
@Composable
private fun overloadedComposable(
    v1: String,
    v2: String,
    v3: String,
    v4: String,
    v5: String,
    v6: String,
    v7: String,
    v8: String,
    v9: String,
    v10: String,
    v11: String,
    v12: String
) { }

@Suppress("UNUSED_PARAMETER")
@Composable
private fun differentParametersTypes(
    v1: String,
    v2: Any,
    v3: Int,
    v4: Float,
    v5: Double,
    v6: Long
) { }

private class ComposablesWrapper {
    @Composable
    fun composableMethod() {
    }

    fun nonComposableMethod() {
    }

    @Suppress("UNUSED_PARAMETER")
    fun nonComposableMethodWithComposerParam(unused: Composer) {
    }

    @Composable
    fun composableMethodWithDefaults(
        s1: String,
        s2: String,
        s3: String = "a",
        s4: String = "a",
        s5: String = "a"
    ): String { return s1 + s2 + s3 + s4 + s5 }

    @Composable
    fun overloadedComposableMethod() {
    }

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun overloadedComposableMethod(s: String) {
    }

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun overloadedComposableMethod(
        v1: String,
        v2: String,
        v3: String,
        v4: String,
        v5: String,
        v6: String,
        v7: String,
        v8: String,
        v9: String,
        v10: String
    ) { }

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun overloadedComposableMethod(
        v1: String,
        v2: String,
        v3: String,
        v4: String,
        v5: String,
        v6: String,
        v7: String,
        v8: String,
        v9: String,
        v10: String,
        v11: String
    ) { }

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun overloadedComposableMethod(
        v1: String,
        v2: String,
        v3: String,
        v4: String,
        v5: String,
        v6: String,
        v7: String,
        v8: String,
        v9: String,
        v10: String,
        v11: String,
        v12: String
    ) { }

    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun differentParametersTypesMethod(
        v1: String,
        v2: Any,
        v3: Int,
        v4: Float,
        v5: Double,
        v6: Long
    ) { }
}

class ComposableMethodInvokerTest {
    private val clazz =
        Class.forName("androidx.compose.runtime.reflect.ComposableMethodInvokerTestKt")
    private val wrapperClazz =
        Class.forName("androidx.compose.runtime.reflect.ComposablesWrapper")

    private val composable = clazz.declaredMethods.find { it.name == "composableFunction" }!!
    private val nonComposable = clazz.declaredMethods.find { it.name == "nonComposableFunction" }!!
    private val nonComposableWithComposer =
        clazz.declaredMethods.find { it.name == "nonComposableFunctionWithComposerParam" }!!
    private val composableMethod =
        wrapperClazz.declaredMethods.find { it.name == "composableMethod" }!!
    private val nonComposableMethod =
        wrapperClazz.declaredMethods.find { it.name == "nonComposableMethod" }!!
    private val nonComposableMethodWithComposer =
        wrapperClazz.declaredMethods.find { it.name == "nonComposableMethodWithComposerParam" }!!

    @Test
    fun test_isComposable_correctly_checks_functions() {
        assertTrue(composable.isComposable)
        assertFalse(nonComposable.isComposable)
        assertFalse(nonComposableWithComposer.isComposable)
        assertTrue(composableMethod.isComposable)
        assertFalse(nonComposableMethod.isComposable)
        assertFalse(nonComposableMethodWithComposer.isComposable)
    }

    @Throws(NoSuchMethodException::class)
    @Test
    fun test_getDeclaredComposableMethod_differentiates_overloaded_functions() {
        val method0 = clazz.getDeclaredComposableMethod("overloadedComposable")
        val method1 = clazz.getDeclaredComposableMethod("overloadedComposable", String::class.java)
        val method10 =
            clazz.getDeclaredComposableMethod(
                "overloadedComposable",
                *Array(10) { String::class.java }
            )
        val method11 =
            clazz.getDeclaredComposableMethod(
                "overloadedComposable",
                *Array(11) { String::class.java }
            )
        val method12 =
            clazz.getDeclaredComposableMethod(
                "overloadedComposable",
                *Array(12) { String::class.java }
            )

        assertNotEquals(method0, method1)
        assertNotEquals(method0, method10)
        assertNotEquals(method0, method11)
        assertNotEquals(method0, method12)
        assertNotEquals(method1, method10)
        assertNotEquals(method1, method11)
        assertNotEquals(method1, method12)
        assertNotEquals(method10, method11)
        assertNotEquals(method10, method12)
        assertNotEquals(method11, method12)
    }

    @Throws(NoSuchMethodException::class)
    @Test
    fun test_getDeclaredComposableMethod_differentiates_overloaded_methods() {
        val method0 = wrapperClazz.getDeclaredComposableMethod("overloadedComposableMethod")
        val method1 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod",
                String::class.java
            )
        val method10 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod",
                *Array(10) { String::class.java }
            )
        val method11 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod",
                *Array(11) { String::class.java }
            )
        val method12 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod",
                *Array(12) { String::class.java }
            )

        assertNotEquals(method0, method1)
        assertNotEquals(method0, method10)
        assertNotEquals(method0, method11)
        assertNotEquals(method0, method12)
        assertNotEquals(method1, method10)
        assertNotEquals(method1, method11)
        assertNotEquals(method1, method12)
        assertNotEquals(method10, method11)
        assertNotEquals(method10, method12)
        assertNotEquals(method11, method12)
    }

    @Throws(NoSuchMethodException::class)
    @Test
    fun test_getDeclaredComposableMethod_works_with_default_params() {
        clazz.getDeclaredComposableMethod(
            "composableFunctionWithDefaults",
            *Array(5) { String::class.java }
        )

        wrapperClazz.getDeclaredComposableMethod(
            "composableMethodWithDefaults",
            *Array(5) { String::class.java }
        )
    }

    @Throws(NoSuchMethodException::class)
    @Test
    fun test_realParametersCount_returns_correct_number_of_parameters() {
        val function0 = clazz.getDeclaredComposableMethod("overloadedComposable")
        val function1 =
            clazz.getDeclaredComposableMethod("overloadedComposable", String::class.java)
        val function10 =
            clazz.getDeclaredComposableMethod(
                "overloadedComposable",
                *Array(10) { String::class.java }
            )
        val function11 =
            clazz.getDeclaredComposableMethod(
                "overloadedComposable",
                *Array(11) { String::class.java }
            )
        val function12 =
            clazz.getDeclaredComposableMethod(
                "overloadedComposable",
                *Array(12) { String::class.java }
            )

        val method0 = wrapperClazz.getDeclaredComposableMethod("overloadedComposableMethod")
        val method1 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod", String::class.java
            )
        val method10 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod",
                *Array(10) { String::class.java }
            )
        val method11 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod",
                *Array(11) { String::class.java }
            )
        val method12 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod",
                *Array(12) { String::class.java }
            )

        assertEquals(0, function0.realParametersCount)
        assertEquals(1, function1.realParametersCount)
        assertEquals(10, function10.realParametersCount)
        assertEquals(11, function11.realParametersCount)
        assertEquals(12, function12.realParametersCount)

        assertEquals(0, method0.realParametersCount)
        assertEquals(1, method1.realParametersCount)
        assertEquals(10, method10.realParametersCount)
        assertEquals(11, method11.realParametersCount)
        assertEquals(12, method12.realParametersCount)

        assertEquals(0, nonComposable.realParametersCount)
        assertEquals(1, nonComposableWithComposer.realParametersCount)
        assertEquals(0, composableMethod.realParametersCount)
        assertEquals(0, nonComposableMethod.realParametersCount)
        assertEquals(1, nonComposableMethodWithComposer.realParametersCount)
    }

    @Suppress("ClassVerificationFailure", "NewApi")
    @Throws(NoSuchMethodException::class)
    @Test
    fun test_realParameters_returns_correct_parameters() {
        val function0 = clazz.getDeclaredComposableMethod("overloadedComposable")
        val function1 =
            clazz.getDeclaredComposableMethod("overloadedComposable", String::class.java)
        val function10 =
            clazz.getDeclaredComposableMethod(
                "overloadedComposable",
                *Array(10) { String::class.java }
            )
        val function11 =
            clazz.getDeclaredComposableMethod(
                "overloadedComposable",
                *Array(11) { String::class.java }
            )
        val function12 =
            clazz.getDeclaredComposableMethod(
                "overloadedComposable",
                *Array(12) { String::class.java }
            )

        val method0 = wrapperClazz.getDeclaredComposableMethod("overloadedComposableMethod")
        val method1 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod",
                String::class.java
            )
        val method10 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod",
                *Array(10) { String::class.java }
            )
        val method11 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod",
                *Array(11) { String::class.java }
            )
        val method12 =
            wrapperClazz.getDeclaredComposableMethod(
                "overloadedComposableMethod",
                *Array(12) { String::class.java }
            )

        val diffParameters =
            clazz.getDeclaredComposableMethod(
                "differentParametersTypes",
                String::class.java,
                Any::class.java,
                Int::class.java,
                Float::class.java,
                Double::class.java,
                Long::class.java
            )

        val diffParametersMethod =
            wrapperClazz.getDeclaredComposableMethod(
                "differentParametersTypesMethod",
                String::class.java,
                Any::class.java,
                Int::class.java,
                Float::class.java,
                Double::class.java,
                Long::class.java
            )

        assertEquals(0, function0.realParameters.size)
        assertEquals(1, function1.realParameters.size)
        assertEquals(10, function10.realParameters.size)
        assertEquals(11, function11.realParameters.size)
        assertEquals(12, function12.realParameters.size)
        assertEquals(12, function12.realParameters.size)

        assertEquals(0, method0.realParameters.size)
        assertEquals(1, method1.realParameters.size)
        assertEquals(10, method10.realParameters.size)
        assertEquals(11, method11.realParameters.size)
        assertEquals(12, method12.realParameters.size)

        assertEquals(0, nonComposable.realParameters.size)
        assertEquals(1, nonComposableWithComposer.realParameters.size)
        assertEquals(0, composableMethod.realParameters.size)
        assertEquals(0, nonComposableMethod.realParameters.size)
        assertEquals(1, nonComposableMethodWithComposer.realParameters.size)

        assertEquals(6, diffParameters.realParameters.size)
        assertEquals(
            listOf(String::class.java, Any::class.java, Int::class.java, Float::class.java,
                Double::class.java, Long::class.java),
            diffParameters.realParameters.map { it.type })

        assertEquals(6, diffParametersMethod.realParameters.size)
        assertEquals(
            listOf(String::class.java, Any::class.java, Int::class.java, Float::class.java,
                Double::class.java, Long::class.java),
            diffParametersMethod.realParameters.map { it.type })
    }

    private class TestFrameClock : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
    }

    private fun <T> executeWithComposer(block: (composer: Composer) -> T): T =
        runBlocking(TestFrameClock()) {
            fun compose(
                recomposer: Recomposer,
                block: @Composable () -> Unit
            ): Composition {
                return Composition(
                    EmptyApplier(),
                    recomposer
                ).apply {
                    setContent(block)
                }
            }

            var res: T? = null
            withRunningRecomposer { r ->
                compose(r) {
                    res = block(currentComposer)
                }
            }
            res!!
        }

    @Test
    fun testInvokeComposableFunctions() {

        val composableWithDefaults =
            clazz.declaredMethods.find { it.name == "composableFunctionWithDefaults" }!!
        composableWithDefaults.isAccessible = true

        val resABAAA = executeWithComposer {
            composableWithDefaults.invokeComposable(it, null, "a", "b") as String
        }

        val resABCAA = executeWithComposer {
            composableWithDefaults.invokeComposable(it, null, "a", "b", "c") as String
        }

        val resABCDA = executeWithComposer {
            composableWithDefaults.invokeComposable(it, null, "a", "b", "c", "d") as String
        }

        val resABCDE = executeWithComposer {
            composableWithDefaults.invokeComposable(it, null, "a", "b", "c", "d", "e") as String
        }

        val resABADA = executeWithComposer {
            composableWithDefaults.invokeComposable(it, null, "a", "b", null, "d") as String
        }

        assertEquals("abaaa", resABAAA)
        assertEquals("abcaa", resABCAA)
        assertEquals("abcda", resABCDA)
        assertEquals("abcde", resABCDE)
        assertEquals("abada", resABADA)
    }

    @Test
    fun testInvokeComposableMethods() {

        val composableWithDefaults =
            wrapperClazz.declaredMethods.find { it.name == "composableMethodWithDefaults" }!!
        composableWithDefaults.isAccessible = true

        val instance = ComposablesWrapper()

        val resABAAA = executeWithComposer {
            composableWithDefaults.invokeComposable(it, instance, "a", "b") as String
        }

        val resABCAA = executeWithComposer {
            composableWithDefaults.invokeComposable(it, instance, "a", "b", "c") as String
        }

        val resABCDA = executeWithComposer {
            composableWithDefaults.invokeComposable(it, instance, "a", "b", "c", "d") as String
        }

        val resABCDE = executeWithComposer {
            composableWithDefaults
                .invokeComposable(it, instance, "a", "b", "c", "d", "e") as String
        }

        val resABADA = executeWithComposer {
            composableWithDefaults.invokeComposable(it, instance, "a", "b", null, "d") as String
        }

        assertEquals("abaaa", resABAAA)
        assertEquals("abcaa", resABCAA)
        assertEquals("abcda", resABCDA)
        assertEquals("abcde", resABCDE)
        assertEquals("abada", resABADA)
    }
}