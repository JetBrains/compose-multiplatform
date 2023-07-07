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

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import java.io.File
import kotlin.reflect.KProperty0

/**
 * Generates two lists, named `CoreIcons` and `ExtendedIcons` that contains pairs mapping a
 * [KProperty0] of the generated icon to the name of the corresponding XML drawable. This is used so
 * we can run tests comparing the generated icon against the original source drawable.
 *
 * @property icons the list of [Icon]s to generate the manifest from. This icons list holds all
 * known icons, and the generator will split them to Core and Extended on [generateTo]
 */
class IconTestingManifestGenerator(private val icons: List<Icon>) {
    /**
     * Generates the list and writes it to [outputSrcDirectory].
     */
    fun generateTo(outputSrcDirectory: File) {
        // Split the icons to Core and Extended lists, and generate output for each.
        val (coreIcons, extendedIcons) = icons.partition { CoreIcons.contains(it.kotlinName) }
        generateTo(outputSrcDirectory, coreIcons, "Core")
        generateTo(outputSrcDirectory, extendedIcons, "Extended")
    }

    private fun generateTo(outputSrcDirectory: File, icons: List<Icon>, prefix: String) {
        val propertyNames: MutableList<String> = mutableListOf()
        // Further split each list by themes, otherwise we get a Method too large exception.
        // We will then generate another file that returns the result of concatenating the list
        // for each theme.
        icons
            .groupBy { it.theme }
            .map { (theme, icons) ->
                val propertyName = "$prefix${theme.themeClassName}Icons"
                propertyNames += propertyName
                theme to generateListOfIconsForTheme(propertyName, theme, icons)
            }
            .forEach { (theme, fileSpec) ->
                // KotlinPoet bans wildcard imports, and we run into class compilation errors
                // (too large a file?) if we add all the imports individually, so let's just add
                // the imports to each file manually.
                val wildcardImport =
                    "import androidx.compose.material.icons.${theme.themePackageName}.*"

                fileSpec.writeToWithCopyright(outputSrcDirectory) { fileContent ->
                    fileContent.replace(
                        "import androidx.compose.ui.graphics.vector.ImageVector",
                        "$wildcardImport\n" +
                            "import androidx.compose.ui.graphics.vector.ImageVector"
                    )
                }
            }

        val mainGetter = FunSpec.getterBuilder()
            .addStatement("return " + propertyNames.joinToString(" + "))
            .build()

        FileSpec.builder(PackageNames.MaterialIconsPackage.packageName, "All${prefix}Icons")
            .addProperty(
                PropertySpec.builder("All${prefix}Icons", type = listOfIconsType)
                    .getter(mainGetter)
                    .build()
            ).setIndent().build().writeToWithCopyright(outputSrcDirectory)
    }
}

/**
 * Generates a Kotlin file with a list containing all icons of the given [theme].
 *
 * @param propertyName the name of the top level property that we should generate the list under
 * @param theme the theme that we are generating the file for
 * @param allIcons a list containing all icons that we will filter to match [theme]
 */
private fun generateListOfIconsForTheme(
    propertyName: String,
    theme: IconTheme,
    allIcons: List<Icon>
): FileSpec {
    val icons = allIcons.filter { it.theme == theme }

    val iconStatements = icons.toStatements()

    return FileSpec.builder(PackageNames.MaterialIconsPackage.packageName, propertyName)
        .addProperty(
            PropertySpec.builder(propertyName, type = listOfIconsType)
                .initializer(
                    buildCodeBlock {
                        addStatement("listOf(")
                        indent()
                        iconStatements.forEach { add(it) }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        ).setIndent().build()
}

/**
 * @return a list of [CodeBlock] representing all the statements for the body of the list.
 * For example, one statement would look like `(Icons.Filled::Menu) to menu`.
 */
private fun List<Icon>.toStatements(): List<CodeBlock> {
    return mapIndexed { index, icon ->
        buildCodeBlock {
            val iconFunctionReference = "(%T.${icon.theme.themeClassName}::${icon.kotlinName})"
            val text = "$iconFunctionReference to \"${icon.xmlFileName}\""
            addStatement(if (index != size - 1) "$text," else text, ClassNames.Icons)
        }
    }
}

private val kPropertyType =
    (KProperty0::class).asClassName().parameterizedBy(ClassNames.ImageVector)
private val pairType = (Pair::class).asClassName().parameterizedBy(
    kPropertyType,
    (String::class).asTypeName()
)
private val listOfIconsType = (List::class).asClassName().parameterizedBy(pairType)
