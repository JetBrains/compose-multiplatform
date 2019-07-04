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
 * [Children] can be applied to one parameter of a composable function, to indicate that this
 * parameter represents the composable's children.  The general expectation is that the parameter
 * this annotates is used to describe the composable's sub-nodes within the composable's view
 * hierarchy.  The type of the parameter that [Children] annotates is expected to be a function of
 * some type, and it is assumed to be [Composable] unless otherwise specified with the [composable]
 * parameter. One does not need to use both [Composable] and [Children] in conjunction with one
 * another.
 *
 * NOTE: It is expected that this annotation will eventually go away and function parameters at the
 * end of a parameter list will be treated as Children parameters, just like postfix lambda
 * arguments in normal Kotlin.
 *
 * @param composable If false, the type of the argument will be compiled as a function that is _not_
 *  [Composable]. Defaults to true.
 *
 * @see Composable
 */
@MustBeDocumented
@Target(
    // Parameters of [Composable] functions
    AnnotationTarget.VALUE_PARAMETER,
    // Properties of [Component] classes
    AnnotationTarget.PROPERTY,
    // Setter functions of [Component] classes
    AnnotationTarget.FUNCTION
)
annotation class Children(val composable: Boolean = true)
