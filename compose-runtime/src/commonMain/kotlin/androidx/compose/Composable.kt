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
 * [Composable] functions are the fundamental building blocks of an application built with Compose.
 *
 * [Composable] can be applied to a function or lambda to indicate that the function/lambda can be
 * used as part of a composition to describe a transformation from application data into a
 * tree or hierarchy.
 *
 * Annotating a function or expression with [Composable] changes the type of that function or
 * expression. For example, [Composable] functions can only ever be called from within another
 * [Composable] function. A useful mental model for [Composable] functions is that an implicit
 * "composable context" is passed into a [Composable] function, and is done so implicitly when it
 * is called from within another [Composable] function.  This "context" can be used to store
 * information from previous executions of the function that happened at the same logical point of
 * the tree.
 *
 * @see Component
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    // not currently used
    AnnotationTarget.CLASS,

    // function declarations
    // @Composable fun Foo() { ... }
    // lambda expressions
    // val foo = @Composable { ... }
    AnnotationTarget.FUNCTION,

    // type declarations
    // var foo: @Composable() () -> Unit = { ... }
    // parameter types
    // foo: @Composable() () -> Unit
    AnnotationTarget.TYPE,

    // composable types inside of type signatures
    // foo: (@Composable() () -> Unit) -> Unit
    AnnotationTarget.TYPE_PARAMETER
)
annotation class Composable
