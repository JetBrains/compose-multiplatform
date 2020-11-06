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
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.uast.UMethod

/**
 * [Detector] that checks functions returning Modifiers for consistency with guidelines.
 *
 * - Modifier factory functions must return Modifier as their type, and not a subclass of Modifier
 */
class ModifierDeclarationDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            // Ignore functions that do not return
            val returnType = node.returnType ?: return

            // Ignore functions that do not return Modifier or something implementing Modifier
            if (!InheritanceUtil.isInheritor(returnType, Modifier)) return

            fun report(lintFix: LintFix? = null) {
                context.report(
                    ModifierFactoryReturnType,
                    node,
                    context.getNameLocation(node),
                    "Modifier factory functions must have a return type of Modifier",
                    lintFix
                )
            }

            if (returnType.canonicalText != Modifier) {
                val source = node.sourcePsi
                // If this node is a property that is a constructor parameter, ignore it.
                if (source is KtParameter) return
                // If this node is a var, then this isn't a Modifier factory API, so just
                // ignore it.
                if ((source as? KtProperty)?.isVar == true) return
                if (source is KtCallableDeclaration && source.returnTypeString != null) {
                    // Function declaration with an explicit return type, such as
                    // `fun foo(): Modifier.element = Bar`. Replace the type with `Modifier`.
                    report(
                        LintFix.create()
                            .replace()
                            .name("Change return type to Modifier")
                            .range(context.getLocation(node))
                            .text(source.returnTypeString)
                            .with(Modifier.split(".").last())
                            .autoFix()
                            .build()
                    )
                    return
                }
                if (source is KtPropertyAccessor) {
                    // If the getter is on a var, then this isn't a Modifier factory API, so just
                    // ignore it.
                    if (source.property.isVar) return

                    // Getter declaration with an explicit return type on the getter, such as
                    // `val foo get(): Modifier.Element = Bar`. Replace the type with `Modifier`.
                    val getterReturnType = source.returnTypeReference?.text

                    if (getterReturnType != null) {
                        report(
                            LintFix.create()
                                .replace()
                                .name("Change return type to Modifier")
                                .range(context.getLocation(node))
                                .text(getterReturnType)
                                .with(Modifier.split(".").last())
                                .autoFix()
                                .build()
                        )
                        return
                    }
                    // Getter declaration with an implicit return type from the property, such as
                    // `val foo: Modifier.Element get() = Bar`. Replace the type with `Modifier`.
                    val propertyType = source.property.returnTypeString

                    if (propertyType != null) {
                        report(
                            LintFix.create()
                                .replace()
                                .name("Change return type to Modifier")
                                .range(context.getLocation(source.property))
                                .text(propertyType)
                                .with(Modifier.split(".").last())
                                .autoFix()
                                .build()
                        )
                        return
                    }
                }
                if (source is KtDeclarationWithBody) {
                    // Declaration without an explicit return type, such as `fun foo() = Bar`
                    // or val foo get() = Bar
                    // Replace the `=` with `: Modifier =`
                    report(
                        LintFix.create()
                            .replace()
                            .name("Add explicit Modifier return type")
                            .range(context.getLocation(node))
                            .pattern("[ \\t\\n]+=")
                            .with(": " + Modifier.split(".").last() + " =")
                            .autoFix()
                            .build()
                    )
                    return
                }
            }
        }
    }

    companion object {
        val ModifierFactoryReturnType = Issue.create(
            "ModifierFactoryReturnType",
            "Modifier factory functions must return Modifier",
            "Modifier factory functions must return Modifier as their type, and not a " +
                "subtype of Modifier (such as Modifier.Element).",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                ModifierDeclarationDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}

private const val Modifier = "androidx.compose.ui.Modifier"

/**
 * TODO: UMethod.returnTypeReference is not available in LINT_API_MIN, so instead use this with a
 * [KtCallableDeclaration].
 * See [org.jetbrains.uast.kotlin.declarations.KotlinUMethod.returnTypeReference] on newer UAST
 * versions.
 */
private val KtCallableDeclaration.returnTypeString: String?
    get() {
        return typeReference?.text
    }
