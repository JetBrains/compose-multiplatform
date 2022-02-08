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

package androidx.compose.runtime

/**
 * The [Composable] declares that it doesn't expect a particular applier. See
 * [ComposableTarget] for more details on what appliers a [Composable] function expects.
 *
 * In a [Composable] function, all the open composable appliers with the same applier index must
 * have the same name.. [CompositionLocalProvider], for example, can use [ComposableOpenTarget]
 * to declare that its content parameter must have the same applier as it receives, since it
 * calls the content parameter directly, but it could be any applier. [ComposableOpenTarget], in
 * this way, works like an open type parameter for the type of applier used by the implied
 * composer parameter.
 *
 * Th [ComposableOpenTarget] is unlikely to be required explicitly as it is inferred
 * automatically by the Compose compiler plugin. See [ComposableTarget] for more details on how
 * attributes are inferred.
 *
 * @param index The index of the open applier parameter. All open appliers with the same
 * non-negative index in the same declaration must have the same name. All negative indexes are
 * considered anonymous and can match any applier. If the [index] is only used once in a
 * declaration it can also match any applier but it is recommended to use a negative index
 * instead or just leave the annotation off as a missing annotation is equivalent to an anonymous
 * applier.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class ComposableOpenTarget(val index: Int)
