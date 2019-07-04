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
 * [StableMarker] marks an annotation as indicating a type as having a stable
 * equals comparision that can be used during composition. When all types passed
 * as parameters to a [Composable] function are marked as stable then then the
 * parameter values are compared for equality based on positional memoization and
 * the call is skipped if all the values are the equal to the previous call.
 *
 * Primitive value types (such as Int, Float, etc), String and enum types are
 * considered, a priori, stable.
*
 */
@MustBeDocumented
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class StableMarker