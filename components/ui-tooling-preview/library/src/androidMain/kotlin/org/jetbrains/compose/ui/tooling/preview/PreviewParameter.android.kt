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
 * On Android, we just typealias to the androidx.compose.ui.tooling.preview.PreviewParameterProvider.
 */
actual typealias PreviewParameterProvider<T> = androidx.compose.ui.tooling.preview.PreviewParameterProvider<T>
