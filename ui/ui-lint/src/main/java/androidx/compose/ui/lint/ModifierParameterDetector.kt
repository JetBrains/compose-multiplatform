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

@file:Suppress("UnstableApiUsage")

package androidx.compose.ui.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import java.util.EnumSet
import java.util.Locale

/**
 * [Detector] that checks Composable functions with Modifiers parameters for consistency with
 * guidelines.
 *
 * For functions with one / more modifier parameters, the first modifier parameter must:
 * - Be named `modifier`
 * - Have a type of `Modifier`
 * - Either have no default value, or have a default value of `Modifier`
 * - If optional, be the first optional parameter in the parameter list
 */
class ModifierParameterDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            // Ignore non-composable functions
            if (!node.isComposable) return

            // Ignore non-unit composable functions
            if (node.returnType != PsiType.VOID) return

            val modifierParameter = node.uastParameters.firstOrNull { parameter ->
                parameter.sourcePsi is KtParameter &&
                    InheritanceUtil.isInheritor(parameter.type, ModifierFqn)
            } ?: return

            // Need to strongly type this or else Kotlinc cannot resolve overloads for
            // getNameLocation
            val modifierParameterElement: UElement = modifierParameter

            val source = modifierParameter.sourcePsi as KtParameter

            if (modifierParameter.name != ModifierParameterName) {
                context.report(
                    ModifierParameter,
                    node,
                    context.getNameLocation(modifierParameterElement),
                    "$ModifierShortName parameter should be named $ModifierParameterName",
                    LintFix.create()
                        .replace()
                        .name("Change name to $ModifierParameterName")
                        .text(modifierParameter.name)
                        .with(ModifierParameterName)
                        .autoFix()
                        .build()
                )
            }

            if (modifierParameter.type.canonicalText != ModifierFqn) {
                context.report(
                    ModifierParameter,
                    node,
                    context.getNameLocation(modifierParameterElement),
                    "$ModifierShortName parameter should have a type of $ModifierShortName",
                    LintFix.create()
                        .replace()
                        .range(context.getLocation(modifierParameterElement))
                        .name("Change type to $ModifierShortName")
                        .text(source.typeReference!!.text)
                        .with(ModifierShortName)
                        .autoFix()
                        .build()
                )
            }

            if (source.hasDefaultValue()) {
                val defaultValue = source.defaultValue!!
                // If the default value is not a reference expression, then it isn't `Modifier`
                // anyway and we can just report an error
                val referenceExpression = source.defaultValue as? KtNameReferenceExpression
                if (referenceExpression?.getReferencedName() != ModifierShortName) {
                    context.report(
                        ModifierParameter,
                        node,
                        context.getNameLocation(modifierParameterElement),
                        "Optional $ModifierShortName parameter should have a default value " +
                            "of `$ModifierShortName`",
                        LintFix.create()
                            .replace()
                            .range(context.getLocation(modifierParameterElement))
                            .name("Change default value to $ModifierShortName")
                            .text(defaultValue.text)
                            .with(ModifierShortName)
                            .autoFix()
                            .build()
                    )
                }
                val index = node.uastParameters.indexOf(modifierParameter)
                val optionalParameterIndex = node.uastParameters.indexOfFirst { parameter ->
                    (parameter.sourcePsi as? KtParameter)?.hasDefaultValue() == true
                }
                if (index != optionalParameterIndex) {
                    context.report(
                        ModifierParameter,
                        node,
                        context.getNameLocation(modifierParameterElement),
                        "$ModifierShortName parameter should be the first optional parameter",
                        // Hard to make a lint fix for this and keep parameter formatting, so
                        // ignore it
                    )
                }
            }
        }
    }

    companion object {
        val ModifierParameter = Issue.create(
            "ModifierParameter",
            "Guidelines for Modifier parameters in a Composable function",
            "The first (or only) Modifier parameter in a Composable function should follow the " +
                "following rules:" +
                "- Be named `$ModifierParameterName`" +
                "- Have a type of `$ModifierShortName`" +
                "- Either have no default value, or have a default value of `$ModifierShortName`" +
                "- If optional, be the first optional parameter in the parameter list",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                ModifierParameterDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private val ModifierParameterName = ModifierShortName.decapitalize(Locale.ROOT)
