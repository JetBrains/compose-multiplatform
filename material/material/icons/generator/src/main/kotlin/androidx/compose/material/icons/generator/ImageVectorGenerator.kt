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

import androidx.compose.material.icons.generator.vector.FillType
import androidx.compose.material.icons.generator.vector.Vector
import androidx.compose.material.icons.generator.vector.VectorNode
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.buildCodeBlock
import java.util.Locale

/**
 * Generator for creating a Kotlin source file with a ImageVectror property for the given [vector],
 * with name [iconName] and theme [iconTheme].
 *
 * @param iconName the name for the generated property, which is also used for the generated file.
 * I.e if the name is `Menu`, the property will be `Menu` (inside a theme receiver object) and
 * the file will be `Menu.kt` (under the theme package name).
 * @param iconTheme the theme that this vector belongs to. Used to scope the property to the
 * correct receiver object, and also for the package name of the generated file.
 * @param vector the parsed vector to generate ImageVector.Builder commands for
 */
class ImageVectorGenerator(
    private val iconName: String,
    private val iconTheme: IconTheme,
    private val vector: Vector
) {
    /**
     * @return a [FileSpec] representing a Kotlin source file containing the property for this
     * programmatic [vector] representation.
     *
     * The package name and hence file location of the generated file is:
     * [PackageNames.MaterialIconsPackage] + [IconTheme.themePackageName].
     */
    fun createFileSpec(): FileSpec {
        val iconsPackage = PackageNames.MaterialIconsPackage.packageName
        val themePackage = iconTheme.themePackageName
        val combinedPackageName = "$iconsPackage.$themePackage"
        // Use a unique property name for the private backing property. This is because (as of
        // Kotlin 1.4) each property with the same name will be considered as a possible candidate
        // for resolution, regardless of the access modifier, so by using unique names we reduce
        // the size from ~6000 to 1, and speed up compilation time for these icons.
        val backingPropertyName = "_" + iconName.decapitalize(Locale.ROOT)
        val backingProperty = backingProperty(name = backingPropertyName)
        return FileSpec.builder(
            packageName = combinedPackageName,
            fileName = iconName
        ).addProperty(
            PropertySpec.builder(name = iconName, type = ClassNames.ImageVector)
                .receiver(iconTheme.className)
                .getter(iconGetter(backingProperty, iconName, iconTheme))
                .build()
        ).addProperty(
            backingProperty
        ).setIndent().build()
    }

    /**
     * @return the body of the getter for the icon property. This getter returns the backing
     * property if it is not null, otherwise creates the icon and 'caches' it in the backing
     * property, and then returns the backing property.
     */
    private fun iconGetter(
        backingProperty: PropertySpec,
        iconName: String,
        iconTheme: IconTheme
    ): FunSpec {
        return FunSpec.getterBuilder()
            .addCode(
                buildCodeBlock {
                    beginControlFlow("if (%N != null)", backingProperty)
                    addStatement("return %N!!", backingProperty)
                    endControlFlow()
                }
            )
            .addCode(
                buildCodeBlock {
                    beginControlFlow(
                        "%N = %M(name = \"%N.%N\")",
                        backingProperty,
                        MemberNames.MaterialIcon,
                        iconTheme.name,
                        iconName
                    )
                    vector.nodes.forEach { node -> addRecursively(node) }
                    endControlFlow()
                }
            )
            .addStatement("return %N!!", backingProperty)
            .build()
    }

    /**
     * @return The private backing property that is used to cache the ImageVector for a given
     * icon once created.
     *
     * @param name the name of this property
     */
    private fun backingProperty(name: String): PropertySpec {
        val nullableImageVector = ClassNames.ImageVector.copy(nullable = true)
        return PropertySpec.builder(name = name, type = nullableImageVector)
            .mutable()
            .addModifiers(KModifier.PRIVATE)
            .initializer("null")
            .build()
    }
}

/**
 * Recursively adds function calls to construct the given [vectorNode] and its children.
 */
private fun CodeBlock.Builder.addRecursively(vectorNode: VectorNode) {
    when (vectorNode) {
        // TODO: b/147418351 - add clip-paths once they are supported
        is VectorNode.Group -> {
            beginControlFlow("%M", MemberNames.Group)
            vectorNode.paths.forEach { path ->
                addRecursively(path)
            }
            endControlFlow()
        }
        is VectorNode.Path -> {
            addPath(vectorNode) {
                vectorNode.nodes.forEach { pathNode ->
                    addStatement(pathNode.asFunctionCall())
                }
            }
        }
    }
}

/**
 * Adds a function call to create the given [path], with [pathBody] containing the commands for
 * the path.
 */
private fun CodeBlock.Builder.addPath(
    path: VectorNode.Path,
    pathBody: CodeBlock.Builder.() -> Unit
) {
    // Only set the fill type if it is EvenOdd - otherwise it will just be the default.
    val setFillType = path.fillType == FillType.EvenOdd

    val parameterList = with(path) {
        listOfNotNull(
            "fillAlpha = ${fillAlpha}f".takeIf { fillAlpha != 1f },
            "strokeAlpha = ${strokeAlpha}f".takeIf { strokeAlpha != 1f },
            "pathFillType = %M".takeIf { setFillType }
        )
    }

    val parameters = if (parameterList.isNotEmpty()) {
        parameterList.joinToString(prefix = "(", postfix = ")")
    } else {
        ""
    }

    if (setFillType) {
        beginControlFlow("%M$parameters", MemberNames.MaterialPath, MemberNames.EvenOdd)
    } else {
        beginControlFlow("%M$parameters", MemberNames.MaterialPath)
    }
    pathBody()
    endControlFlow()
}
