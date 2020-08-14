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

package androidx.compose.runtime

private val EmptyComposable: @Composable () -> Unit = {}

/**
 * Represents empty content for a Composable function.
 *
 * See [orEmpty] for handling nullable Composable lambdas using empty content.
 */
@Stable
fun emptyContent() = EmptyComposable

/**
 * @return this Composable if not null, else [emptyContent].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun (@Composable (() -> Unit))?.orEmpty() = this ?: emptyContent()
