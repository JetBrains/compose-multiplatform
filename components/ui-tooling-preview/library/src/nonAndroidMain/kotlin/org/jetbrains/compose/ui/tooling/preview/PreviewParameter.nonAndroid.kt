/*
 * Copyright 2023 The Android Open Source Project
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

package org.jetbrains.compose.ui.tooling.preview

/**
 * Interface to be implemented by any provider of values that you want to be injected as @[Preview]
 * parameters. This allows providing sample information for previews.
 */
actual interface PreviewParameterProvider<T> {
    /**
     * [Sequence] of values of type [T] to be passed as @[Preview] parameter.
     */
    actual val values: Sequence<T>

    /**
     * Returns the number of elements in the [values] [Sequence].
     */
    actual val count: Int
        get() = values.count()
}
