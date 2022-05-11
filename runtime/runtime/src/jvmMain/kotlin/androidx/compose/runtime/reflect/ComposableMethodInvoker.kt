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

import androidx.compose.runtime.Composer
import androidx.compose.runtime.internal.SLOTS_PER_INT
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import kotlin.math.ceil

private inline fun <reified T> T.dup(count: Int): Array<T> {
    return (0 until count).map { this }.toTypedArray()
}

/**
 * Find the given @Composable method by name.
 */
@Throws(NoSuchMethodException::class)
fun Class<*>.getDeclaredComposableMethod(methodName: String, vararg args: Class<*>): Method {
    val changedParams = changedParamCount(args.size, 0)
    val method = try {
        // without defaults
        getDeclaredMethod(
            methodName,
            *args,
            Composer::class.java, // composer param
            *Int::class.java.dup(changedParams) // changed params
        )
    } catch (e: ReflectiveOperationException) {
        val defaultParams = defaultParamCount(args.size)
        try {
            getDeclaredMethod(
                methodName,
                *args,
                Composer::class.java, // composer param
                *Int::class.java.dup(changedParams), // changed param
                *Int::class.java.dup(defaultParams) // default param
            )
        } catch (e2: ReflectiveOperationException) {
            null
        }
    } ?: throw NoSuchMethodException("$name.$methodName")

    return method
}

/**
 * Returns the default value for the [Class] type. This will be 0 for numeric types, false for
 * boolean and null for object references.
 */
private fun Class<*>.getDefaultValue(): Any? = when (name) {
    "int" -> 0.toInt()
    "short" -> 0.toShort()
    "byte" -> 0.toByte()
    "long" -> 0.toLong()
    "double" -> 0.toDouble()
    "float" -> 0.toFloat()
    "boolean" -> false
    "char" -> 0.toChar()
    else -> null
}

/**
 * Structure intended to be used exclusively by [getComposableInfo].
 */
private data class ComposableInfo(
    val isComposable: Boolean,
    val realParamsCount: Int,
    val changedParams: Int,
    val defaultParams: Int
)

/**
 * Checks whether the method is Composable function and returns result along with the real
 * parameters count and changed parameter count (if composable) and packed in a structure.
 */
private fun Method.getComposableInfo(): ComposableInfo {
    val realParamsCount = parameterTypes.indexOfLast { it == Composer::class.java }
    if (realParamsCount == -1) {
        return ComposableInfo(false, parameterTypes.size, 0, 0)
    }
    val thisParams = if (Modifier.isStatic(this.modifiers)) 0 else 1
    val changedParams = changedParamCount(realParamsCount, thisParams)
    val totalParamsWithoutDefaults = realParamsCount +
        1 + // composer
        changedParams
    val totalParams = parameterTypes.size
    val isDefault = totalParams != totalParamsWithoutDefaults
    val defaultParams = if (isDefault)
        defaultParamCount(realParamsCount)
    else
        0
    return ComposableInfo(
        totalParamsWithoutDefaults + defaultParams == totalParams,
        realParamsCount,
        changedParams,
        defaultParams
    )
}

/**
 * Calls the Composable method on the given [instance]. If the method accepts default values, this
 * function will call it with the correct options set.
 */
@Suppress("BanUncheckedReflection", "ListIterator")
fun Method.invokeComposable(
    composer: Composer,
    instance: Any?,
    vararg args: Any?
): Any? {
    val (isComposable, realParamsCount, changedParams, defaultParams) = getComposableInfo()

    check(isComposable)

    val totalParams = parameterTypes.size
    val changedStartIndex = realParamsCount + 1
    val defaultStartIndex = changedStartIndex + changedParams

    val defaultsMasks = Array(defaultParams) { index ->
        val start = index * BITS_PER_INT
        val end = minOf(start + BITS_PER_INT, realParamsCount)
        val useDefault =
            (start until end).map { if (it >= args.size || args[it] == null) 1 else 0 }
        val mask = useDefault.foldIndexed(0) { i, mask, default -> mask or (default shl i) }
        mask
    }

    val arguments = Array(totalParams) { idx ->
        when (idx) {
            // pass in "empty" value for all real parameters since we will be using defaults.
            in 0 until realParamsCount -> args.getOrElse(idx) {
                parameterTypes[idx].getDefaultValue()
            }
            // the composer is the first synthetic parameter
            realParamsCount -> composer
            // since this is the root we don't need to be anything unique. 0 should suffice.
            // changed parameters should be 0 to indicate "uncertain"
            changedStartIndex -> 1
            in changedStartIndex + 1 until defaultStartIndex -> 0
            // Default values mask, all parameters set to use defaults
            in defaultStartIndex until totalParams -> defaultsMasks[idx - defaultStartIndex]
            else -> error("Unexpected index")
        }
    }
    return invoke(instance, *arguments)
}

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
 * Returns true if the method is a Composable function and false otherwise.
 */
val Method.isComposable: Boolean
    get() = getComposableInfo().isComposable

/**
 * Returns real parameters count for the method, it returns the actual parameters count for the
 * usual methods and for Composable functions it excludes the utility Compose-specific parameters
 * from counting.
 */
val Method.realParametersCount: Int
    get() {
        val (isComposable, realParametersCount, _, _) = getComposableInfo()
        if (isComposable) {
            return realParametersCount
        }
        return parameterTypes.size
    }

/**
 * Returns real parameters for the method, it returns the actual parameters for the usual methods
 * and for Composable functions it excludes the utility Compose-specific parameters.
 */
val Method.realParameters: Array<out Parameter>
    @Suppress("ClassVerificationFailure", "NewApi")
    get() {
        val (isComposable, realParametersCount, _, _) = getComposableInfo()
        if (isComposable) {
            return parameters.copyOfRange(0, realParametersCount)
        }
        return parameters
    }
