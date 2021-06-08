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
 * This annotation can be applied to [Composable] functions so that no group will be generated
 * around the body of the function it annotates. This is not safe unless the body of the function
 * and any functions that it calls only executes "read" operations on the passed in composer.
 * This will result in slightly more efficient code.
 *
 * A common use case for this are for functions that only need to be composable in order to read
 * [CompositionLocal] values, but don't call any other composables.
 *
 * Caution: Use of this annotation means that the annotated declaration *MUST* comply with this
 * contract, or else the resulting code's behavior will be undefined.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER
)
annotation class ReadOnlyComposable