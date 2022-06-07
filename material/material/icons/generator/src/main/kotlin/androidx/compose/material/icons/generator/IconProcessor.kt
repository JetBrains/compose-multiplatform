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

import com.google.common.base.CaseFormat
import java.io.File
import java.util.Locale

/**
 * Processes vector drawables in [iconDirectory] into a list of icons, removing any unwanted
 * attributes (such as android: attributes that reference the theme) from the XML source.
 *
 * Drawables in [iconDirectory] should match the following structure, see download_material_icons.py
 * to update icons, using this structure.
 *
 * // Top level
 * [iconDirectory]
 * // Theme name
 * ├── filled
 *     // Icon name
 *     ├── menu.xml
 *     └── zoom_out_map.xml
 * ├── outlined
 * ├── rounded
 * ├── twotone
 * └── sharp
 *
 * @param iconDirectory root directory containing the directory structure mentioned above
 * @param expectedApiFile location of the checked-in API file that contains the current list of
 * all icons processed and generated
 * @param generatedApiFile location of the to-be-generated API file in the build directory,
 * that we will write to and compare with [expectedApiFile]. This way the generated file can be
 * copied to overwrite the expected file, 'confirming' any API changes as a result of changing
 * icons in [iconDirectory].
 */
class IconProcessor(
    private val iconDirectories: List<File>,
    private val expectedApiFile: File,
    private val generatedApiFile: File,
    private val verifyApi: Boolean = true
) {
    /**
     * @return a list of processed [Icon]s, from the given [iconDirectory].
     */
    fun process(): List<Icon> {
        val icons = loadIcons()

        if (verifyApi) {
            ensureIconsExistInAllThemes(icons)
            writeApiFile(icons, generatedApiFile)
            checkApi(expectedApiFile, generatedApiFile)
        }

        return icons
    }

    private fun loadIcons(): List<Icon> {
        val themeDirs = iconDirectories

        return themeDirs.flatMap { dir ->
            val theme = dir.name.toIconTheme()
            val icons = dir.walk().filter { !it.isDirectory }.toList()

            val transformedIcons = icons.map { file ->
                val filename = file.nameWithoutExtension
                val kotlinName = filename.toKotlinPropertyName()

                // Prefix the icon name with a theme so we can ensure they will be unique when
                // copied to res/drawable.
                val xmlName = "${theme.themePackageName}_$filename"

                Icon(
                    kotlinName = kotlinName,
                    xmlFileName = xmlName,
                    theme = theme,
                    fileContent = processXmlFile(file.readText())
                )
            }

            // Ensure icon names are unique when accounting for case insensitive filesystems -
            // workaround for b/216295020
            transformedIcons
                .groupBy { it.kotlinName.lowercase(Locale.ROOT) }
                .filter { it.value.size > 1 }
                .filterNot { entry ->
                    entry.value.map { it.kotlinName }.containsAll(AllowedDuplicateIconNames)
                }
                .forEach { entry ->
                    throw IllegalStateException(
                        """Found multiple icons with the same case-insensitive filename:
                                | ${entry.value.joinToString()}. Generating icons with the same
                                | case-insensitive filename will cause issues on devices without
                                | a case sensitive filesystem (OSX / Windows).""".trimMargin()
                    )
                }

            transformedIcons
        }
    }
}

/**
 * Processes the given [fileContent] by removing android theme attributes and values.
 */
private fun processXmlFile(fileContent: String): String {
    // Remove any defined tint for paths that use theme attributes
    val tintAttribute = Regex.escape("""android:tint="?attr/colorControlNormal">""")
    val tintRegex = """\n.*?$tintAttribute""".toRegex(RegexOption.MULTILINE)

    return fileContent
        .replace(tintRegex, ">")
        // The imported icons have white as the default path color, so let's change it to be
        // black as is typical on Android.
        .replace("@android:color/white", "@android:color/black")
}

/**
 * Ensures that each icon in each theme is available in every other theme
 */
private fun ensureIconsExistInAllThemes(icons: List<Icon>) {
    val groupedIcons = icons.groupBy { it.theme }

    check(groupedIcons.keys.containsAll(IconTheme.values().toList())) {
        "Some themes were missing from the generated icons"
    }

    val expectedIconNames = groupedIcons.values.map { themeIcons ->
        themeIcons.map { icon -> icon.kotlinName }.sorted()
    }

    expectedIconNames.first().let { expected ->
        expectedIconNames.forEach { actual ->
            check(actual == expected) {
                "Not all icons were found in all themes $actual $expected"
            }
        }
    }
}

/**
 * Writes an API representation of [icons] to [file].
 */
private fun writeApiFile(icons: List<Icon>, file: File) {
    val apiText = icons
        .groupBy { it.theme }
        .map { (theme, themeIcons) ->
            themeIcons
                .map { icon ->
                    theme.themeClassName + "." + icon.kotlinName
                }
                .sorted()
                .joinToString(separator = "\n")
        }
        .sorted()
        .joinToString(separator = "\n")

    file.writeText(apiText)
}

/**
 * Ensures that [generatedFile] matches the checked-in API surface in [expectedFile].
 */
private fun checkApi(expectedFile: File, generatedFile: File) {
    check(expectedFile.exists()) {
        "API file at ${expectedFile.canonicalPath} does not exist!"
    }

    check(expectedFile.readText() == generatedFile.readText()) {
        """Found differences when comparing API files!
                |Please check the difference and copy over the changes if intended.
                |expected file: ${expectedFile.canonicalPath}
                |generated file: ${generatedFile.canonicalPath}
            """.trimMargin()
    }
}

/**
 * Converts a snake_case name to a KotlinProperty name.
 *
 * If the first character of [this] is a digit, the resulting name will be prefixed with an `_`
 */
private fun String.toKotlinPropertyName(): String {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this).let { name ->
        if (name.first().isDigit()) "_$name" else name
    }
}

// These icons have already shipped in a stable release, so it is too late to rename / remove one to
// fix the clash.
private val AllowedDuplicateIconNames = listOf("AddChart", "Addchart")