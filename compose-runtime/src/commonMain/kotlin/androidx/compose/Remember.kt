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
    currentComposer.cache(true, calculation)

/**
 * Remember the value returned by [calculation] if [v1] is equal to the previous composition, otherwise
 * produce and remember a new value by calling [calculation].
 */
@OptIn(ComposeCompilerApi::class)
@Composable
inline fun <T, /*reified*/ V1> remember(v1: V1, calculation: () -> T): T {
    return currentComposer.cache(!currentComposer.changed(v1), calculation)
}

/**
 * Remember the value returned by [calculation] if [v1] and [v2] are equal to the previous composition,
 * otherwise produce and remember a new value by calling [calculation].
 */
@OptIn(ComposeCompilerApi::class)
@Composable
inline fun <T, /*reified*/ V1, /*reified*/ V2> remember(
    v1: V1,
    v2: V2,
    calculation: () -> T
): T {
    var valid = !currentComposer.changed(v1)
    valid = !currentComposer.changed(v2) && valid
    return currentComposer.cache(valid, calculation)
}

/**
 * Remember the value returned by [calculation] if [v1], [v2] and [v3] are equal to the previous
 * composition, otherwise produce and remember a new value by calling [calculation].
 */
@OptIn(ComposeCompilerApi::class)
@Composable
inline fun <T, /*reified*/ V1, /*reified*/ V2, /*reified*/ V3> remember(
    v1: V1,
    v2: V2,
    v3: V3,
    calculation: () -> T
): T {
    var valid = !currentComposer.changed(v1)
    valid = !currentComposer.changed(v2) && valid
    valid = !currentComposer.changed(v3) && valid
    return currentComposer.cache(valid, calculation)
}

/**
 * Remember the value returned by [block] if all values of [inputs] are equal to the previous
 * composition, otherwise produce and remember a new value by calling [block].
 */
@OptIn(ComposeCompilerApi::class)
@Composable
inline fun <V> remember(vararg inputs: Any?, block: () -> V): V {
    var valid = true
    for (input in inputs) valid = !currentComposer.changed(input) && valid
    return currentComposer.cache(valid, block)
}
