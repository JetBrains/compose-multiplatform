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

internal expect val currentComposerNonNull: Composer<*>
internal expect var currentComposer: Composer<*>?

internal expect fun createComposer(root: Any, context: Context, recomposer: Recomposer): Composer<*>
expect fun <T> Composer<*>.runWithCurrent(block: () -> T): T

@PublishedApi
internal val invocation = Any()

@PublishedApi
internal val provider = Any()

@PublishedApi
internal val consumer = Any()

@PublishedApi
internal val reference = Any()