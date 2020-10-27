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

package androidx.compose.runtime

/**
 * Remember the value produced by [calculation]. [calculation] will only be evaluated during the composition.
 * Recomposition will always return the value produced by composition.
 */
@OptIn(ComposeCompilerApi::class)
@Composable
inline fun <T> remember(calculation: @ComposableContract(preventCapture = true) () -> T): T =
    currentComposer.cache(false, calculation)

/**
 * Remember the value returned by [calculation] if [v1] is equal to the previous composition, otherwise
 * produce and remember a new value by calling [calculation].
 */
@OptIn(ComposeCompilerApi::class)
@Composable
inline fun <T, /*reified*/ V1> remember(
    v1: V1,
    calculation: @ComposableContract(preventCapture = true) () -> T
): T {
    return currentComposer.cache(currentComposer.changed(v1), calculation)
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
    calculation: @ComposableContract(preventCapture = true) () -> T
): T {
    return currentComposer.cache(
        currentComposer.changed(v1) or currentComposer.changed(v2),
        calculation
    )
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
    calculation: @ComposableContract(preventCapture = true) () -> T
): T {
    return currentComposer.cache(
        currentComposer.changed(v1) or
            currentComposer.changed(v2) or
            currentComposer.changed(v3),
        calculation
    )
}

/**
 * Remember the value returned by [calculation] if all values of [inputs] are equal to the previous
 * composition, otherwise produce and remember a new value by calling [calculation].
 */
@OptIn(ComposeCompilerApi::class)
@Composable
inline fun <V> remember(
    vararg inputs: Any?,
    calculation: @ComposableContract(preventCapture = true) () -> V
): V {
    var invalid = false
    for (input in inputs) invalid = invalid or currentComposer.changed(input)
    return currentComposer.cache(invalid, calculation)
}

/**
 * [remember] a [mutableStateOf] [newValue] and update its value to [newValue] on each
 * recomposition of the [rememberUpdatedState] call.
 *
 * [rememberUpdatedState] should be used when parameters or values computed during composition
 * are referenced by a long-lived lambda or object expression. Recomposition will update the
 * resulting [State] without recreating the long-lived lambda or object, allowing that object to
 * persist without cancelling and resubscribing, or relaunching a long-lived operation that may
 * be expensive or prohibitive to recreate and restart.
 * This may be common when working with [DisposableEffect] or [LaunchedEffect], for example:
 *
 * @sample androidx.compose.runtime.samples.rememberUpdatedStateSampleWithDisposableEffect
 *
 * [LaunchedEffect]s often describe state machines that should not be reset and restarted if a
 * parameter or event callback changes, but they should have the current value available when
 * needed. For example:
 *
 * @sample androidx.compose.runtime.samples.rememberUpdatedStateSampleWithLaunchedTask
 *
 * By using [rememberUpdatedState] a composable function can update these operations in progress.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun <T> rememberUpdatedState(newValue: T): State<T> = remember {
    mutableStateOf(newValue)
}.apply { value = newValue }
