/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lint

import androidx.compose.lint.Names
import androidx.compose.lint.inheritsFrom
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
import com.intellij.psi.util.ClassUtil
import java.util.EnumSet
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * [Detector] that checks calls to Modifier.offset that use a non-lambda overload but read from
 * dynamic/state variables. It is recommended to use the lambda overload in those cases for
 * performance improvements
 */
class NonLambdaOffsetModifierDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> = listOf(
        FoundationNames.Layout.Offset.shortName,
        FoundationNames.Layout.AbsoluteOffset.shortName
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // Non Modifier Offset
        if (!method.isInPackageName(FoundationNames.Layout.PackageName)) return

        if (method.isDesiredOffsetOverload() && hasStateBackedArguments(node)) {
            context.report(
                UseOfNonLambdaOverload,
                node,
                context.getNameLocation(node),
                ReportMainMessage
            )
        }
    }

    /**
     * Has two parameters of type DP
     */
    private fun PsiMethod.isDesiredOffsetOverload(): Boolean {
        // use signature
        return ClassUtil.getAsmMethodSignature(this) == OffsetSignature
    }

    private fun hasStateBackedArguments(node: UCallExpression): Boolean {
        var dynamicArguments = false

        node.valueArguments
            .forEach { expression ->
                expression.accept(object : AbstractUastVisitor() {
                    override fun visitSimpleNameReferenceExpression(
                        node: USimpleNameReferenceExpression
                    ): Boolean {
                        val declaration = node.tryResolveUDeclaration() ?: return false
                        dynamicArguments = dynamicArguments || declaration.isCompositionAwareType()
                        return dynamicArguments
                    }
                })
            }

        return dynamicArguments
    }

    companion object {
        const val ReportMainMessage =
            "State backed values should use the lambda overload of Modifier.offset"

        const val IssueId = "UseOfNonLambdaOffsetOverload"

        val UseOfNonLambdaOverload = Issue.create(
            IssueId,
            "Modifier.offset{ } is preferred over Modifier.offset() for " +
                "`State` backed arguments.",
            "`Modifier.offset()` is recommended to be used with static arguments only to " +
                "avoid unnecessary recompositions. `Modifier.offset{ }` is " +
                "preferred in the cases where the arguments are backed by a `State`.",
            Category.PERFORMANCE, 3, Severity.WARNING,
            Implementation(
                NonLambdaOffsetModifierDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private fun UDeclaration.isCompositionAwareType(): Boolean {
    return isDelegateOfState() || isMethodFromStateOrAnimatable() || isStateOrAnimatableVariable()
}

private fun UDeclaration.isStateOrAnimatableVariable(): Boolean {
    return (this is UVariable) &&
        (type.inheritsFrom(Names.Runtime.State) ||
            type.inheritsFrom(Names.Animation.Core.Animatable))
}

/**
 * Special handling of implicit receiver types
 */
private fun UDeclaration.isMethodFromStateOrAnimatable(): Boolean {
    val argument = this as? UMethod
    val containingClass = argument?.containingClass ?: return false

    return containingClass.inheritsFrom(Names.Runtime.State) ||
        containingClass.inheritsFrom(Names.Animation.Core.Animatable)
}

private fun UDeclaration.isDelegateOfState(): Boolean {
    val localVariable = this as? ULocalVariable
    val ktProperty = localVariable?.sourcePsi as? KtProperty ?: return false
    val delegateExpression =
        ktProperty.delegate?.expression.toUElement() as? UExpression ?: return false
    val cleanCallExpression =
        (delegateExpression.skipParenthesizedExprDown() as? UCallExpression) ?: return false

    return cleanCallExpression.returnType?.inheritsFrom(Names.Runtime.State) ?: false
}

private const val OffsetSignature =
    "(Landroidx/compose/ui/Modifier;Landroidx/compose/ui/unit/Dp;Landroidx/compose/ui/unit/Dp;)" +
        "Landroidx/compose/ui/Modifier;"