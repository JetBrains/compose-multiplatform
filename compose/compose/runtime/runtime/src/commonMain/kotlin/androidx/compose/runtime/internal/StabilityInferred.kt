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
 * This annotation is added on classes by the compiler when their stability is inferred. It
 * indicates that there will be a synthetic static final int `$stable` added to the class which
 * can be used by the compose compiler plugin to generate expressions to determine the stability
 * of a realized type at runtime.
 *
 * @param parameters A bitmask, with one bit per type parameter of the annotated class. A 1 bit
 * indicates that the stability of the annotated class should be calculated as a combination of
 * the stability of the class itself and the stability of that type parameter.
 */
@ComposeCompilerApi
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class StabilityInferred(val parameters: Int)
