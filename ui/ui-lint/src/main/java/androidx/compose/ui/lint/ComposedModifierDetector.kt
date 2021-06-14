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

package androidx.compose.ui.lint

import androidx.compose.lint.Names
import androidx.compose.lint.isComposable
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils.Companion.tryResolveUDeclaration
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor
import java.util.EnumSet

/**
 * [Detector] that checks calls to Modifier.composed to make sure they actually reference a
 * Composable function inside - otherwise there is no reason to use Modifier.composed, and since
 * the resulting Modifier is not skippable, it will cause worse performance.
 */
class ComposedModifierDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(Names.Ui.Composed.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Ui.PackageName)) return

        val factoryLambda = node.valueArguments.find {
            node.getParameterForArgument(it)?.name == "factory"
        } ?: return

        var hasComposableCall = false
        factoryLambda.accept(object : AbstractUastVisitor() {
            /**
             * Visit function calls to see if the functions are composable
             */
            override fun visitCallExpression(
                node: UCallExpression
            ): Boolean = (node.resolveToUElement() as? UMethod).hasComposableCall()

            /**
             * Visit any simple name reference expressions and see if they resolve to a
             * composable function - for example if referencing a property with a composable
             * getter, such as CompositionLocal.current.
             */
            override fun visitSimpleNameReferenceExpression(
                node: USimpleNameReferenceExpression
            ): Boolean {
                return try {
                    (node.tryResolveUDeclaration() as? UMethod).hasComposableCall()
                } catch (e: NullPointerException) {
                    // TODO: elvis expressions will throw a NPE if you try and resolve them
                    // https://youtrack.jetbrains.com/issue/KT-46795
                    false
                }
            }

            private fun UMethod?.hasComposableCall(): Boolean {
                if (this?.isComposable == true) {
                    hasComposableCall = true
                }
                return hasComposableCall
            }
        })

        if (!hasComposableCall) {
            context.report(
                UnnecessaryComposedModifier,
                node,
                context.getNameLocation(node),
                "Unnecessary use of Modifier.composed"
            )
        }
    }

    companion object {
        val UnnecessaryComposedModifier = Issue.create(
            "UnnecessaryComposedModifier",
            "Modifier.composed should only be used for modifiers that invoke @Composable functions",
            "`Modifier.composed` allows invoking @Composable functions when creating a `Modifier`" +
                " instance - for example, using `remember` to have instance-specific state, " +
                "allowing the same `Modifier` object to be safely used in multiple places. Using " +
                "`Modifier.composed` without calling any @Composable functions inside is " +
                "unnecessary, and since the Modifier is no longer skippable, this can cause a lot" +
                " of extra work inside the composed body, leading to worse performance.",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                ComposedModifierDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
