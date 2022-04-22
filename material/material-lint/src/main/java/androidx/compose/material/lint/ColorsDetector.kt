/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.compose.material.lint

import androidx.compose.lint.Name
import androidx.compose.lint.isInPackageName
import androidx.compose.material.lint.MaterialNames.Material
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.util.isConstructorCall
import java.util.EnumSet

/**
 * [Detector] that checks `Colors` definitions for correctness.
 *
 * Background colors that share the same color (such as `surface` and `background`) should also
 * share the same 'on' color (`onSurface` and `onBackground`) - otherwise we can't know which
 * color to use for a given background color by value.
 */
class ColorsDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            val method = node.resolve() ?: return
            if (!method.isInPackageName(Material.PackageName)) return

            if (node.isConstructorCall()) {
                if (method.containingClass?.name != Colors.shortName) return
            } else {
                if (
                    node.methodName != null &&
                    node.methodName != LightColors.shortName &&
                    node.methodName != DarkColors.shortName
                ) return
            }

            val parameters = method.parameterList.parameters.mapIndexed { index, parameter ->
                val argumentForParameter = node.getArgumentForParameter(index)
                ParameterWithArgument(
                    parameter,
                    argumentForParameter
                )
            }

            // Filter to only background colors, and group by their value
            val backgroundColorGroups = parameters.filter {
                it.parameter.name in OnColorMap.keys
            }.filter {
                // Filter out any parameters that have unknown defaults / arguments, we can't do
                // anything here about them
                it.sourceText != null
            }.groupBy {
                it.sourceText
            }.values

            // For each grouped pair of colors, make sure that all corresponding 'on' colors have
            // the same color
            backgroundColorGroups.forEach { colors ->
                // Find all corresponding onColors for these colors and group them by value
                val onColorGroups = colors.map { parameter ->
                    val background = parameter.parameter.name
                    val onColor = OnColorMap[background]
                    parameters.first {
                        it.parameter.name == onColor
                    }
                }
                    // If multiple background colors have the same color (such as `primary` /
                    // `primaryVariant`) then filter the duplicates out so we don't report the same
                    // 'on' color multiple times.
                    .distinctBy { it.parameter.name }
                    .groupBy {
                        it.sourceText
                    }

                // Report if there are multiple groups (i.e different values between 'on' colors)
                if (onColorGroups.size > 1) {
                    onColorGroups.values.forEach { group ->
                        group.forEach { parameter ->
                            val argument = parameter.argument
                            // If the conflicting color comes from the default value of a function,
                            // there is nothing to report - just report the clashing colors that
                            // the user explicitly provides.
                            if (argument != null) {
                                context.report(
                                    ConflictingOnColor,
                                    argument,
                                    context.getNameLocation(argument),
                                    "Conflicting 'on' color for a given background"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        val ConflictingOnColor = Issue.create(
            "ConflictingOnColor",
            "Background colors with the same value should have the same 'on' color",
            "In the Material color system background colors have a corresponding 'on' " +
                "color which is used for the content color inside a component. For example, a " +
                "button colored `primary` will have `onPrimary` text. Because of this, it is " +
                "important that there is only one possible `onColor` for a given color value, " +
                "otherwise there is no way to know which 'on' color should be used inside a " +
                "component. To fix this either use the same 'on' color for identical background " +
                "colors, or use a different background color for each 'on' color.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                ColorsDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

/**
 * Represents a [parameter] with the corresponding [argument] provided as the value for the
 * parameter. If [parameter] has a default value, [argument] may be null.
 */
class ParameterWithArgument(
    val parameter: PsiParameter,
    val argument: UExpression?
) {
    /**
     * String representing the text passed as the argument / provided within the default.
     *
     * Note: this will fail in rare cases where the same underlying value is referenced to in a
     * different way, such as:
     *
     * ```
     * val white = Color.White
     * ...
     * onPrimary = white,
     * onSecondary = Color.White
     * ```
     *
     * Theoretically we can resolve declarations, but this would require a lot of work to handle
     * different types of references, such as parameters and properties, and still will miss some
     * cases such as when this is defined inside a function with an external parameter that we
     * can't resolve.
     *
     * This string is `null` if no argument was provided, and a default value exists in a class
     * file - so we can't resolve what it is.
     */
    val sourceText: String? by lazy {
        val sourceExpression: UElement? = when {
            // An argument was provided
            argument != null -> argument
            // A default value exists (so !! is safe), and we are browsing Kotlin source
            // Note: this should be is KtLightParameter, but this was changed from an interface
            // to a class, so we get an IncompatibleClassChangeError.
            parameter is KtLightParameter -> {
                parameter.kotlinOrigin!!.defaultValue.toUElement()
            }
            // A default value exists, but it is in a class file so we can't access it anymore
            else -> null
        }

        sourceExpression?.resolveToDeclarationText()
    }

    /**
     * Returns a string that matches the original declaration for this UElement. If this is a
     * literal or a reference to something out of scope (such as parameter), this will just be
     * that text. If this is a reference to a variable, this will try and find the text of the
     * variable, the last time it was assigned.
     */
    private fun UElement.resolveToDeclarationText(): String? {
        // Get the source psi and go back to a UElement since if the declaration is a property, it
        // will actually be represented as a method (since in Kotlin properties are just getter
        // methods by default). Going to the source then back again gives us the actual UField.
        // This might be fixed in later versions of UAST, but not on the current version we run
        // tests against
        val resolved = tryResolve()?.toUElement()?.sourcePsi.toUElement()
        return if (resolved is PsiVariable) {
            val declaration = UastLintUtils.findLastAssignment(resolved, this)
            declaration?.resolveToDeclarationText() ?: sourcePsi?.text
        } else {
            sourcePsi?.text
        }
    }
}

/**
 * Map of background colors to corresponding 'on' colors.
 */
private val OnColorMap = mapOf(
    "primary" to "onPrimary",
    "primaryVariant" to "onPrimary",
    "secondary" to "onSecondary",
    "secondaryVariant" to "onSecondary",
    "background" to "onBackground",
    "surface" to "onSurface",
    "error" to "onError"
)

private val LightColors = Name(Material.PackageName, "lightColors")
private val DarkColors = Name(Material.PackageName, "darkColors")
private val Colors = Name(Material.PackageName, "Colors")
