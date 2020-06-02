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

package androidx.compose

/**
 * This annotation can be applied to [Composable] functions and provide metadata to the compiler
 * that imply compose-specific contracts that the author of the function is guaranteeing the
 * function complies with. This metadata can be used to generate more efficient code.
 *
 * Caution: Use of this annotation means that the annotated declaration *MUST* comply with those
 * contracts, or else the resulting code's behavior will be undefined.
 *
 * @param restartable When false,this will prevent code from being generated which
 * allow this function's execution to be skipped or restarted. This may be desirable for small
 * functions which just directly call another composable function and have very little machinery
 * in them directly.
 *
 * @param readonly if false, no group will be generated around the body of the function it annotates
 * . This is not safe unless the body of the function only executes "read" operations on the
 * passed in composer..
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
annotation class ComposableContract(
    val restartable: Boolean = true,
    val readonly: Boolean = false
)