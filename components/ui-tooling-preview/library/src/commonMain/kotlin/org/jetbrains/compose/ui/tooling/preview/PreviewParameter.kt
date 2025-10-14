/*
 * Copyright 2023 The Android Open Source Project
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

package org.jetbrains.compose.ui.tooling.preview

import kotlin.reflect.KClass

/**
 * Interface to be implemented by any provider of values that you want to be injected as @[Preview]
 * parameters. This allows providing sample information for previews.
 */
@Deprecated(
    "Use androidx.compose.ui.tooling.preview.PreviewParameterProvider from " +
            "org.jetbrains.compose.ui:ui-tooling-preview module instead",
    ReplaceWith("PreviewParameterProvider", "androidx.compose.ui.tooling.preview.PreviewParameterProvider")
)
expect interface PreviewParameterProvider<T> {
    /**
     * [Sequence] of values of type [T] to be passed as @[Preview] parameter.
     */
    val values: Sequence<T>

    /**
     * Returns the number of elements in the [values] [Sequence].
     */
    open val count: Int
}

/**
 * [PreviewParameter] can be applied to any parameter of a @[Preview].
 *
 * @param provider A [PreviewParameterProvider] class to use to inject values to the annotated
 * parameter.
 * @param limit Max number of values from [provider] to inject to this parameter.
 */
@Deprecated(
    "Use androidx.compose.ui.tooling.preview.PreviewParameter from " +
            "org.jetbrains.compose.ui:ui-tooling-preview module instead",
    ReplaceWith("PreviewParameter", "androidx.compose.ui.tooling.preview.PreviewParameter")
)
annotation class PreviewParameter(
    val provider: KClass<out PreviewParameterProvider<*>>,
    val limit: Int = Int.MAX_VALUE
)
