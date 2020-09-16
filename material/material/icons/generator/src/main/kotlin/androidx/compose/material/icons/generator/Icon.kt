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

package androidx.compose.material.icons.generator

/**
 * Represents a icon's Kotlin name, processed XML file name, theme, and XML file content.
 *
 * The [kotlinName] is typically the PascalCase equivalent of the original icon name, with the
 * caveat that icons starting with a number are prefixed with an underscore.
 *
 * @property kotlinName the name of the generated Kotlin property, for example `ZoomOutMap`.
 * @property xmlFileName the name of the processed XML file
 * @property theme the theme of this icon
 * @property fileContent the content of the source XML file that will be parsed.
 */
data class Icon(
    val kotlinName: String,
    val xmlFileName: String,
    val theme: IconTheme,
    val fileContent: String
)
