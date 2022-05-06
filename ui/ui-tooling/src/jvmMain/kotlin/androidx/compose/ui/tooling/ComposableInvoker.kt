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

package androidx.compose.ui.tooling

import androidx.compose.runtime.Composer
import androidx.compose.ui.ExperimentalComposeUiApi
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.math.ceil

/**
 * A utility object to invoke composable function by its name and containing class.
 */
@ExperimentalComposeUiApi
object ComposableInvoker {

    /**
     * Returns true if the [methodTypes] and [actualTypes] are compatible. This means that every
     * `actualTypes[n]` are assignable to `methodTypes[n]`.
     */
    private fun compatibleTypes(
        methodTypes: Array<Class<*>>,
        actualTypes: Array<Class<*>>
    ): Boolean =
        methodTypes.size == actualTypes.size &&
            methodTypes.mapIndexed { index, clazz -> clazz.isAssignableFrom(actualTypes[index]) }
                .all { it }

    /**
     * Same as [Class#getDeclaredMethod] but it accounts for compatible types so the signature does
     * not need to exactly match. This allows finding method calls that use subclasses as parameters
     * instead of the exact types.
     */
    private fun Class<*>.getDeclaredCompatibleMethod(
        methodName: String,
        vararg args: Class<*>
    ): Method {
        val actualTypes: Array<Class<*>> = arrayOf(*args)
        return declaredMethods.firstOrNull {
            methodName == it.name && compatibleTypes(it.parameterTypes, actualTypes)
        } ?: throw NoSuchMethodException("$methodName not found")
    }

    private inline fun <reified T> T.dup(count: Int): Array<T> {
        return (0..count).map { this }.toTypedArray()
    }

    /**
     * Find the given method by name. If the method has parameters, this function will try to find
     * the version that accepts default parameters.
     */
    private fun Class<*>.findComposableMethod(methodName: String, vararg args: Any?): Method {
        val method = try {
            // without defaults
            val changedParams = changedParamCount(args.size, 0)
            getDeclaredCompatibleMethod(
                methodName,
                *args.mapNotNull { it?.javaClass }.toTypedArray(),
                Composer::class.java, // composer param
                kotlin.Int::class.java, // key param
                *kotlin.Int::class.java.dup(changedParams) // changed params
            )
        } catch (e: ReflectiveOperationException) {
            try {
                declaredMethods.find { it.name == methodName }
            } catch (e: ReflectiveOperationException) {
                null
            }
        } ?: throw NoSuchMethodException("$name.$methodName")

        return method
    }

    /**
     * Returns the default value for the [Class] type. This will be 0 for numeric types, false for
     * boolean, '0' for char and null for object references.
     */
    private fun Class<*>.getDefaultValue(): Any? = when (name) {
        "int" -> 0.toInt()
        "short" -> 0.toShort()
        "byte" -> 0.toByte()
        "long" -> 0.toLong()
        "double" -> 0.toDouble()
        "float" -> 0.toFloat()
        "boolean" -> false
        "char" -> '0'
        else -> null
    }

    /**
     * Calls the method on the given [instance]. If the method accepts default values, this function
     * will call it with the correct options set.
     */
    @Suppress("BanUncheckedReflection")
    private fun Method.invokeComposableMethod(
        instance: Any?,
        composer: Composer,
        vararg args: Any?
    ): Any? {
        val composerIndex = parameterTypes.indexOfLast { it == Composer::class.java }
        val realParams = composerIndex
        val thisParams = if (instance != null) 1 else 0
        val changedParams = changedParamCount(realParams, thisParams)
        val totalParamsWithoutDefaults = realParams +
            1 + // composer
            changedParams
        val totalParams = parameterTypes.size
        val isDefault = totalParams != totalParamsWithoutDefaults
        val defaultParams = if (isDefault)
            defaultParamCount(realParams)
        else
            0

        check(
            realParams +
                1 + // composer
                changedParams +
                defaultParams ==
                totalParams
        )

        val changedStartIndex = composerIndex + 1
        val defaultStartIndex = changedStartIndex + changedParams

        val arguments = Array(totalParams) { idx ->
            when (idx) {
                // pass in "empty" value for all real parameters since we will be using defaults.
                in 0 until realParams -> args.getOrElse(idx) {
                    parameterTypes[idx].getDefaultValue()
                }
                // the composer is the first synthetic parameter
                composerIndex -> composer
                // since this is the root we don't need to be anything unique. 0 should suffice.
                // changed parameters should be 0 to indicate "uncertain"
                in changedStartIndex until defaultStartIndex -> 0
                // Default values mask, all parameters set to use defaults
                in defaultStartIndex until totalParams -> 0b111111111111111111111.toInt()
                else -> error("Unexpected index")
            }
        }
        return invoke(instance, *arguments)
    }

    private const val SLOTS_PER_INT = 15
    private const val BITS_PER_INT = 31

    private fun changedParamCount(realValueParams: Int, thisParams: Int): Int {
        if (realValueParams == 0) return 1
        val totalParams = realValueParams + thisParams
        return ceil(
            totalParams.toDouble() / SLOTS_PER_INT.toDouble()
        ).toInt()
    }

    private fun defaultParamCount(realValueParams: Int): Int {
        return ceil(
            realValueParams.toDouble() / BITS_PER_INT.toDouble()
        ).toInt()
    }

    /**
     * Invokes the given [methodName] belonging to the given [className]. The [methodName] is
     * expected to be a Composable function.
     * This method [args] will be forwarded to the Composable function.
     */
    @ExperimentalComposeUiApi
    fun invokeComposable(
        className: String,
        methodName: String,
        composer: Composer,
        vararg args: Any?
    ) {
        try {
            val composableClass = Class.forName(className)

            val method = composableClass.findComposableMethod(methodName, *args)
            method.isAccessible = true

            if (Modifier.isStatic(method.modifiers)) {
                // This is a top level or static method
                method.invokeComposableMethod(null, composer, *args)
            } else {
                // The method is part of a class. We try to instantiate the class with an empty
                // constructor.
                val instance = composableClass.getConstructor().newInstance()
                method.invokeComposableMethod(instance, composer, *args)
            }
        } catch (e: ReflectiveOperationException) {
            throw ClassNotFoundException("Composable Method '$className.$methodName' not found", e)
        }
    }
}
