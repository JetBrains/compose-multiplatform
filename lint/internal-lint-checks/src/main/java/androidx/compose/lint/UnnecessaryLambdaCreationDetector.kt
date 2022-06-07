/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.kotlin.KotlinUBlockExpression
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression
import org.jetbrains.uast.kotlin.KotlinUImplicitReturnExpression
import org.jetbrains.uast.kotlin.UnknownKotlinExpression
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.tryResolve

/**
 * Lint [Detector] to ensure that we are not creating extra lambdas just to emit already captured
 * lambdas inside Compose code. For example:
 * ```
 * val lambda = @Composable {}
 * Foo {
 *     lambda()
 * }
 * ```
 *
 * Can just be inlined to:
 * ```
 * Foo(lambda)
 * ```
 *
 * This helps avoid object allocation but more importantly helps us avoid extra code generation
 * around composable lambdas.
 */
class UnnecessaryLambdaCreationDetector : Detector(), SourceCodeScanner {
    override fun createUastHandler(context: JavaContext) = UnnecessaryLambdaCreationHandler(context)

    override fun getApplicableUastTypes() = listOf(ULambdaExpression::class.java)

    /**
     * This handler visits every lambda expression and reports an issue if the following criteria
     * (in order) hold true:
     *
     * 1. There is only one expression inside the lambda.
     * 2. The expression is a function call
     * 3. The lambda is being invoked as part of a function call, and not as a property assignment
     *    such as val foo = @Composable {}
     * 4. The receiver type of the function call is `Function0` (i.e, we are invoking something
     *    that matches `() -> Unit` - this both avoids non-lambda invocations but also makes sure
     *    that we don't warn for lambdas that have parameters, such as @Composable (Int) -> Unit
     *    - this cannot be inlined.)
     * 5. The outer function call that contains this lambda is not a call to a `LayoutNode`
     *    (because these are technically constructor invocations that we just intercept calls to
     *    there is no way to avoid using a trailing lambda for this)
     * 6. The lambda is not being passed as a parameter, for example `Foo { lambda -> lambda() }`
     */
    class UnnecessaryLambdaCreationHandler(private val context: JavaContext) : UElementHandler() {

        override fun visitLambdaExpression(node: ULambdaExpression) {
            val expressions = (node.body as? KotlinUBlockExpression)?.expressions ?: return

            if (expressions.size != 1) return

            val expression = when (val expr = expressions.first().skipParenthesizedExprDown()) {
                is KotlinUFunctionCallExpression -> expr
                is KotlinUImplicitReturnExpression ->
                    expr.returnExpression as? KotlinUFunctionCallExpression
                else -> null
            } ?: return

            // This is the parent function call that contains the lambda expression.
            // I.e in Foo { bar() } this will be the call to `Foo`.
            // We want to make sure this lambda is being invoked in the context of a function call,
            // and not as a property assignment - so we cast to KotlinUFunctionCallExpression to
            // filter out such cases.
            val parentExpression = (node.uastParent as? KotlinUFunctionCallExpression) ?: return

            // If we can't resolve the parent call, then the parent function is defined in a
            // separate module, so we don't have the right metadata - and hence the argumentType
            // below will be Function0 even if in the actual source it has a scope. Return early to
            // avoid false positives.
            parentExpression.resolve() ?: return

            // If the expression has no receiver, it is not a lambda invocation
            val functionType = expression.receiverType as? PsiClassReferenceType ?: return

            // Find the functional type of the parent argument, for example () -> Unit (Function0)
            val argumentType = node.getExpressionType() as? PsiClassReferenceType ?: return

            // Return if the receiver of the lambda argument and the lambda itself don't match. This
            // happens if the functional types are different, for example a lambda with 0 parameters
            // (Function0) and a lambda with 1 parameter (Function1). Similarly for two lambdas
            // with 0 parameters, but one that has a receiver scope (SomeScope.() -> Unit).
            if (functionType != argumentType) return

            val expectedComposable = node.isComposable

            // Try and get the UElement for the source of the lambda
            val resolvedLambdaSource = expression.sourcePsi.calleeExpression?.toUElement()
                ?.tryResolve()?.toUElement()
                // Sometimes the above will give us a method (representing the getter for a
                // property), when the actual backing element is a property. Going to the source
                // and back should give us the actual UVariable we are looking for.
                ?.sourcePsi.toUElement()

            val isComposable = when (resolvedLambdaSource) {
                is UVariable -> resolvedLambdaSource.isComposable
                // TODO: if the resolved source is a parameter in a local function, it
                //  incorrectly returns an UnknownKotlinExpression instead of a UParameter
                //  https://youtrack.jetbrains.com/issue/KTIJ-19125
                is UnknownKotlinExpression -> return
                else -> error(parentExpression.asSourceString())
            }

            if (isComposable != expectedComposable) return

            context.report(
                ISSUE,
                node,
                context.getNameLocation(expression),
                "Creating an unnecessary lambda to emit a captured lambda"
            )
        }
    }

    companion object {
        private const val Explanation =
            "Creating this extra lambda instead of just passing the already captured lambda means" +
                " that during code generation the Compose compiler will insert code around " +
                "this lambda to track invalidations. This adds some extra runtime cost so you" +
                " should instead just directly pass the lambda as a parameter to the function."

        val ISSUE = Issue.create(
            "UnnecessaryLambdaCreation",
            "Creating an unnecessary lambda to emit a captured lambda",
            Explanation,
            Category.PERFORMANCE, 5, Severity.ERROR,
            Implementation(
                UnnecessaryLambdaCreationDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
