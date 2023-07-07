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
 * This annotation is used to mark an annotation as being equivalent using [ComposableTarget] with
 * the fully qualified name of the marked annotation as the `applier` value. See [ComposableTarget]
 * for when a [ComposableTarget] annotation is required and when it can be inferred by the Compose
 * compiler plugin.
 *
 * The [description] value can be used to supply a string that is used to describe the group of
 * composable function instead of applier parameter of the [ComposableTarget]. See UiComposable and
 * VectorComposable for examples. If no description is provided, the fully-qualified name of the
 * marked annotation is used instead.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class ComposableTargetMarker(val description: String = "")
