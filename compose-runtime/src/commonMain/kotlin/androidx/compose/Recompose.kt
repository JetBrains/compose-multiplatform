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
 * In most cases we recommend using [state] with immutable types in order to
 * maintain local state inside of composables. For cases where this is impractical, Recompose can
 * help you.
 *
 * @see state
 * @see Observe
 * @see invalidate
 */
@Composable
@Deprecated(
    "The Recompose composable is no longer a useful abstraction. Most recomposition should happen" +
            " as a result of MutableState assignments. For anything beyond that, it is " +
            "recommended that you use the `invalidate` function to trigger a recomposition of the" +
            " current scope.",
    replaceWith = ReplaceWith("val recompose = invalidate")
)
fun Recompose(body: @Composable (recompose: () -> Unit) -> Unit) = body(invalidate)
