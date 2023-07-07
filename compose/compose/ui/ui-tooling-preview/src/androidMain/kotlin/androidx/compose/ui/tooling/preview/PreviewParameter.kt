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

package androidx.compose.ui.tooling.preview

import kotlin.reflect.KClass
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * Interface to be implemented by any provider of values that you want to be injected as @[Preview]
 * parameters. This allows providing sample information for previews.
 */
@JvmDefaultWithCompatibility
interface PreviewParameterProvider<T> {
    /**
     * [Sequence] of values of type [T] to be passed as @[Preview] parameter.
     */
    val values: Sequence<T>

    /**
     * Returns the number of elements in the [values] [Sequence].
     */
    val count get() = values.count()
}

/**
 * [PreviewParameter] can be applied to any parameter of a @[Preview].
 *
 * @param provider A [PreviewParameterProvider] class to use to inject values to the annotated
 * parameter.
 * @param limit Max number of values from [provider] to inject to this parameter.
 */
annotation class PreviewParameter(
    val provider: KClass<out PreviewParameterProvider<*>>,
    val limit: Int = Int.MAX_VALUE
)
