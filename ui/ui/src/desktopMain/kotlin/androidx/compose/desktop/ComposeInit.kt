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

@file:Suppress("DEPRECATION_ERROR")

package androidx.compose.desktop

/**
 * Can be called multiple times.
 *
 * Initialization will occur only on the first call. The next calls will do nothing.
 *
 * Should be called in a class that uses Jetpack Compose Api:
 *
 * class SomeClass {
 *     companion object {
 *         init {
 *             initCompose()
 *         }
 *     }
 * }
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("We don't need to init Compose explicitly now")
fun initCompose() = Unit