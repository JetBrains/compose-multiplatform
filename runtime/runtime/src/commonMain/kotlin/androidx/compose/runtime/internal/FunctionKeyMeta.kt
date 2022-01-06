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

package androidx.compose.runtime.internal

import androidx.compose.runtime.ComposeCompilerApi

/**
 * This annotation is applied to the FunctionKeyMeta classes created by the Compose
 * Compiler. These classes will have multiple of these annotations, each one corresponding to a
 * single composable function. The annotation holds some metadata about the function itself and is
 * intended to be used to provide information useful to tooling.
 *
 * @param key The key used for the function's group.
 * @param startOffset The startOffset of the function in the source file at the time of compilation.
 * @param endOffset The startOffset of the function in the source file at the time of compilation.
 */
@ComposeCompilerApi
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class FunctionKeyMeta(
    val key: Int,
    val startOffset: Int,
    val endOffset: Int
)

/**
 * This annotation is applied to the FunctionKeyMeta classes created by the Compose
 * Compiler. This is intended to be used to provide information useful to tooling.
 *
 * @param file The file path of the file the associated class was produced for
 */
@ComposeCompilerApi
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FunctionKeyMetaClass(
    val file: String
)
