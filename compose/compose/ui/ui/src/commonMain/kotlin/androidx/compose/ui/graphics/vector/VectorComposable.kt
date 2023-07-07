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

package androidx.compose.ui.graphics.vector

import androidx.compose.runtime.ComposableTargetMarker

/**
 * An annotation that can be used to mark an composable function as being expected to be use in a
 * composable function that is also marked or inferred to be marked as a [VectorComposable].
 *
 * Using this annotation explicitly is rarely necessary as the Compose compiler plugin will infer
 * the necessary equivalent annotations automatically. See
 * [androidx.compose.runtime.ComposableTarget] for details.
 */
@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "Vector Composable")
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class VectorComposable()
