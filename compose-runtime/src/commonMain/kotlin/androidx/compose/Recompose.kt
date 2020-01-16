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
 * [Recompose] is a component which passes a "recompose" function to its children which, when
 * invoked, will cause its children to recompose. This is useful if you are updating local state
 * and need to cause a recomposition manually.
 *
 * In most cases we recommend using [Model] classes or [state] with immutable types in order to
 * maintain local state inside of composables. For cases where this is impractical, Recompose can
 * help you.
 *
 * Example:
 *
 * @sample androidx.compose.samples.recomposeSample
 *
 * Note: The above example can be done without [Recompose] by annotating `LoginState` with [Model].
 *
 * @see Model
 * @see Observe
 * @see invalidate
 */
@Composable
fun Recompose(body: @Composable() (recompose: () -> Unit) -> Unit) {
    val composer = currentComposerNonNull
    composer.startRestartGroup(recompose)
    body(invalidate)
    composer.endRestartGroup()?.updateScope {
        Recompose(body)
    }
}

private val recompose = Any()
