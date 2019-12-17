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

/**
 * Remember the value produced by [calculation]. [calculation] will only be evaluated during the composition.
 * Recomposition will always return the value produced by composition.
 */

@Composable
inline fun <T> remember(calculation: () -> T): T =
    currentComposerNonNull.cache(true, calculation)

/**
 * Remember the value returned by [calculation] if [v1] is equal to the previous composition, otherwise
 * produce and remember a new value by calling [calculation].
 */
@Composable
inline fun <T, /*reified*/ V1> remember(v1: V1, calculation: () -> T) = currentComposerNonNull
    .let {
        it.cache(!it.changed(v1), calculation)
    }

/**
 * Remember the value returned by [calculation] if [v1] and [v2] are equal to the previous composition,
 * otherwise produce and remember a new value by calling [calculation].
 */
@Composable
inline fun <T, /*reified*/ V1, /*reified*/ V2> remember(
    v1: V1,
    v2: V2,
    calculation: () -> T
): T {
    return currentComposerNonNull.let {
        var valid = !it.changed(v1)
        valid = !it.changed(v2) && valid
        it.cache(valid, calculation)
    }
}

/**
 * Remember the value returned by [calculation] if [v1], [v2] and [v3] are equal to the previous
 * composition, otherwise produce and remember a new value by calling [calculation].
 */
@Composable
inline fun <T, /*reified*/ V1, /*reified*/ V2, /*reified*/ V3> remember(
    v1: V1,
    v2: V2,
    v3: V3,
    calculation: () -> T
): T {
    return currentComposerNonNull.let {
        var valid = !it.changed(v1)
        valid = !it.changed(v2) && valid
        valid = !it.changed(v3) && valid
        it.cache(valid, calculation)
    }
}

/**
 * Remember the value returned by [block] if all values of [inputs] are equal to the previous
 * composition, otherwise produce and remember a new value by calling [block].
 */
@Composable
inline fun <V> remember(vararg inputs: Any?, block: () -> V): V {
    return currentComposerNonNull.let {
        var valid = true
        for (input in inputs) valid = !it.changed(input) && valid
        it.cache(valid, block)
    }
}

/**
 * An Effect that positionally memoizes the result of a computation.
 *
 * @param calculation A function to produce the result
 * @return The result of the calculation, or the cached value from the composition
 */
@Deprecated(
    "memo has been renamed to remember",
    ReplaceWith("remember", "androidx.compose.remember"),
    DeprecationLevel.ERROR
)
@Composable
/*inline*/ fun <T> memo(calculation: () -> T) = remember { calculation() }

/**
 * An Effect that positionally memoizes the result of a computation.
 *
 * @param v1 An input to the memoization. If this value changes, the calculation will be re-executed.
 * @param calculation A function to produce the result
 * @return The result of the calculation, or the cached value from the composition
 */
@Deprecated(
    "memo has been renamed to remember",
    ReplaceWith("remember", "androidx.compose.remember"),
    DeprecationLevel.ERROR
)
@Composable
/*inline*/ fun <T, /*reified*/ V1> memo(
    v1: V1,
    calculation: () -> T
) = remember(v1) { calculation() }

/**
 * An Effect that positionally memoizes the result of a computation.
 *
 * @param v1 An input to the memoization. If this value changes, the calculation will be re-executed.
 * @param v2 An input to the memoization. If this value changes, the calculation will be re-executed.
 * @param calculation A function to produce the result
 * @return The result of the calculation, or the cached value from the composition
 */
@Deprecated(
    "memo has been renamed to remember",
    ReplaceWith("remember", "androidx.compose.remember"),
    DeprecationLevel.ERROR
)
@Composable
/*inline*/ fun <T, /*reified*/ V1, /*reified*/ V2> memo(
    v1: V1,
    v2: V2,
    calculation: () -> T
) = remember(v1, v2) { calculation() }

/**
 * An Effect that positionally memoizes the result of a computation.
 *
 * @param inputs The inputs to the memoization. If any of these values change, the calculation will be re-executed.
 * @param calculation A function to produce the result
 * @return The result of the calculation, or the cached value from the composition
 */
@Deprecated(
    "memo has been renamed to remember",
    ReplaceWith("remember", "androidx.compose.remember"),
    DeprecationLevel.ERROR
)
@Composable
fun <T> memo(vararg inputs: Any?, calculation: () -> T) = remember(*inputs) { calculation() }
