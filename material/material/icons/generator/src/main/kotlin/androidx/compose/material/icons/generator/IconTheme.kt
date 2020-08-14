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
 * Enum representing the different themes for Material icons.
 *
 * @property themePackageName the lower case name used for package names and in xml files
 * @property themeClassName the CameCase name used for the theme objects
 */
enum class IconTheme(val themePackageName: String, val themeClassName: String) {
    Filled("filled", "Filled"),
    Outlined("outlined", "Outlined"),
    Rounded("rounded", "Rounded"),
    TwoTone("twotone", "TwoTone"),
    Sharp("sharp", "Sharp")
}

/**
 * Returns the matching [IconTheme] from [this] [IconTheme.themePackageName].
 */
fun String.toIconTheme() = requireNotNull(IconTheme.values().find {
    it.themePackageName == this
}) { "No matching theme found" }

/**
 * The ClassName representing this [IconTheme] object, so we can generate extension properties on
 * the object.
 */
val IconTheme.className get() = PackageNames.MaterialIconsPackage.className("Icons", themeClassName)
