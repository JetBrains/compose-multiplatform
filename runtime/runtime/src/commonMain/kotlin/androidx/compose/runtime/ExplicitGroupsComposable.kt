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
 * This annotation can be applied to [Composable] functions so that no groups will be generated
 * in the body of the function it annotates. The expectation is that the groups the function
 * should produce will be created manually using methods on [currentComposer].
 *
 * Unlike [ReadOnlyComposable], the expectation with this annotation is that the marked
 * [Composable] does in fact make writes to the [Composer] and produces a single group.
 *
 * Caution: Use of this annotation removes all guarantees provided by the Compose Compiler and
 * usage of it should be considered at your own risk.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER
)
annotation class ExplicitGroupsComposable