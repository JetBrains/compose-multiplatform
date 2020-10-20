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
import com.android.tools.lint.detector.api.TextFormat
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.VALUE_PARAMETER
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.kotlin.KotlinStringTemplateUPolyadicExpression
import org.jetbrains.uast.kotlin.KotlinStringULiteralExpression
import org.jetbrains.uast.kotlin.KotlinUArrayAccessExpression
import org.jetbrains.uast.kotlin.KotlinUBinaryExpression
import org.jetbrains.uast.kotlin.KotlinUBlockExpression
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression
import org.jetbrains.uast.kotlin.KotlinUIfExpression
import org.jetbrains.uast.kotlin.KotlinULambdaExpression
import org.jetbrains.uast.kotlin.KotlinUQualifiedReferenceExpression
import org.jetbrains.uast.kotlin.KotlinUSimpleReferenceExpression

private const val ModifierClass = "androidx.compose.ui.Modifier"
private const val Modifier = "Modifier"
private const val ModifierFile = "Modifier.kt"
private const val ComposedModifierFile = "ComposedModifier.kt"
private const val LambdaFunction = "kotlin.jvm.functions.Function1"
private const val InspectorInfoClass = "androidx.compose.ui.platform.InspectorInfo"
private const val InspectableValueFile = "androidx.compose.ui.platform.InspectableValueKt"
private const val UnitClass = "kotlin.Unit"
private const val DebugInspectorInfoFunction = "debugInspectorInfo"

/**
 * Lint [Detector] to ensure that we are creating debug information for the layout inspector on
 * all modifiers. For example in:
 * ```
 *   fun Modifier.preferredWidth(width: Dp) = this.then(SizeModifier(width))
 * ```
 *
 * The layout inspector will not know that the name is `preferredWidth` and the `width` member
 * may not be a field in SizeModifier that we can see via reflection.
 *
 * To supply debug information to the layout inspector include an InspectorInfo lambda like this:
 * ```
 *   fun Modifier.preferredWidth(width: Dp) = this.then(
 *      SizeModifier(
 *          width = width,
 *          inspectorInfo = debugInspectorInfo {
 *              name = "preferredWidth",
 *              value = width,
 *          )
 *      )
 *  )
 * ```
 *
 * The `debugInspectorInfo' lambda will be stripped from release builds.
 *
 * If the modifier has multiple parameters use the `properties` specifier instead:
 * ```
 *   fun Modifier.preferredSize(width: Dp, height: Dp) = this.then(
 *      SizeModifier(
 *          width = width,
 *          inspectorInfo = debugInspectorInfo {
 *              name = "preferredWidth",
 *              properties["width"] = width
 *              properties["height"] = height
 *          )
 *      )
 *  )
 * ```
 */
class ModifierInspectorInfoDetector : Detector(), SourceCodeScanner {
    override fun createUastHandler(context: JavaContext): UElementHandler = ModifierHandler(context)

    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    /**
     * This handler visits every method and determines if this is a Modifier definition with a
     * `then` construct, and reports an issue for any of the following problems:
     *
     * 1. The modifier implementation is not a function call
     * 2. The modifier implementation does not have an InspectorInfo lambda as last parameter
     * 3. The lambda is not surrounded by a call to `debugInspectorInfo`
     * 4. The modifier name is missing from the lambda or specified incorrectly
     * 5. A modifier value is specified that doesn't match an actual modifier argument
     * 6. A modifier argument is specified that doesn't match an actual modifier argument
     * 7. An actual modifier argument is missing
     */
    private class ModifierHandler(private val context: JavaContext) : UElementHandler() {

        override fun visitMethod(node: UMethod) {
            if (node.containingFile?.name == ModifierFile ||
                node.containingFile?.name == ComposedModifierFile ||
                firstParameterType(node) != ModifierClass
            ) {
                // Ignore the method if it isn't a method on Modifier,
                // or if the method is defined in Modifier.kt or ComposedModifier.kt
                return
            }
            // For now: only look at definitions with a Modifier.then(newModifier) body.
            // Extend that to other definitions in a later change.
            val then = thenBody(node) ?: return
            checkInspectorInfo(node, then)
        }

        private fun firstParameterType(method: UMethod): String? =
            (method.parameters.firstOrNull() as? KtLightParameter)?.type?.canonicalText

        private fun thenBody(method: UMethod): KotlinUFunctionCallExpression? {
            val body = method.uastBody as? UBlockExpression ?: return null
            val statement = body.expressions.singleOrNull() as? UReturnExpression ?: return null
            val expression = statement.returnExpression as? KotlinUQualifiedReferenceExpression
            val then = expression?.selector as? KotlinUFunctionCallExpression ?: return null
            val thenMethod = then.resolve() ?: return null
            val paramType = thenMethod.parameters.singleOrNull()?.type as? PsiClassReferenceType
            if (name(thenMethod) != "then" ||
                thenMethod.returnTypeElement?.type?.canonicalText != ModifierClass ||
                paramType?.canonicalText != ModifierClass
            ) {
                return null
            }
            return then
        }

        // Disregard the mangled part of a method with inline class parameters.
        // TODO: change this to an API call when a demangle function is available
        private fun name(method: PsiMethod): String =
            method.name.substringBefore('-')

        private fun wildcardType(type: PsiType?): String? =
            (type as? PsiWildcardType)?.bound?.canonicalText

        // Return true if this is a lambda expression of the type: "InspectorInfo.() -> Unit"
        private fun isInspectorInfoLambdaType(type: PsiType?): Boolean {
            val referenceType = type as? PsiClassReferenceType ?: return false
            return referenceType.rawType().canonicalText == LambdaFunction &&
                referenceType.parameterCount == 2 &&
                wildcardType(referenceType.parameters[0]) == InspectorInfoClass &&
                wildcardType(referenceType.parameters[1]) !== UnitClass
        }

        private fun asInspectorInfoLambda(expr: UExpression?): KotlinULambdaExpression? {
            val lambda = expr as? KotlinULambdaExpression ?: return null
            return if (isInspectorInfoLambdaType(lambda.getExpressionType())) lambda else null
        }

        private fun asDebugInspectorInfoCall(expr: UExpression?): KotlinUFunctionCallExpression? {
            val call = expr as? KotlinUFunctionCallExpression ?: return null
            val method = call.resolve() ?: return null
            if (name(method) != DebugInspectorInfoFunction ||
                method.containingClass?.qualifiedName != InspectableValueFile ||
                call.valueArgumentCount != 1 ||
                !isInspectorInfoLambdaType(call.valueArguments[0].getExpressionType()) ||
                !isInspectorInfoLambdaType(call.returnType)
            ) {
                return null
            }
            return call
        }

        // Check the modifier definition in the argument of the then operator,
        // Allow the definition to be an conditional 'if".
        private fun checkInspectorInfo(method: UMethod, then: KotlinUFunctionCallExpression) {
            val thenArgument = then.valueArguments.lastOrNull() ?: return wrongLambda(then)
            if (thenArgument is KotlinUIfExpression) {
                checkModifierBlock(method, thenArgument.thenExpression)
                checkModifierBlock(method, thenArgument.elseExpression)
            } else {
                checkModifierDefinition(method, thenArgument)
            }
        }

        private fun checkModifierBlock(method: UMethod, block: UExpression?) {
            if (block is KotlinUBlockExpression) {
                checkModifierDefinition(method, block.expressions.lastOrNull())
            } else {
                checkModifierDefinition(method, block)
            }
        }

        private fun checkModifierDefinition(method: UMethod, modifierSelector: UExpression?) {
            if (modifierSelector is KotlinUSimpleReferenceExpression &&
                modifierSelector.identifier == Modifier
            ) {
                // Accept a noop Modifier
                return
            }
            var selector = modifierSelector ?: return
            while (selector is KotlinUQualifiedReferenceExpression) {
                // Support inner class modifier definitions like:
                //    private data class SizeModifier() : Modifier.Element {
                //      inner data class WithOption(width: Int) : SizeModifier {
                //      }
                //    }
                selector = selector.selector
            }

            // The thenArgument is expected to be a modifier constructor call
            val modifier = selector as? KotlinUFunctionCallExpression
                ?: return wrongLambda(selector)

            // The last argument of the modifier is expected to be a InspectorInfo lambda
            val lastArgument = modifier.valueArguments.lastOrNull()
            val debugInfo = asDebugInspectorInfoCall(lastArgument)
                ?: return if (asInspectorInfoLambda(lastArgument) != null) {
                    // This is an InspectorInfo lambda but the debugInspectorInfo call is missing
                    missingDebugInfoCall(lastArgument!!)
                } else {
                    // This is not an InspectorInfo lambda
                    wrongLambda(modifier)
                }
            val lambda = asInspectorInfoLambda(debugInfo.valueArguments.first())
                ?: return wrongLambda(debugInfo)
            val lambdaBody = lambda.body as? KotlinULambdaExpression.Body
            val expressions = lambdaBody?.expressions ?: emptyList()
            if (expressions.isEmpty()) {
                // Empty lambda
                return wrongLambda(debugInfo)
            }
            val methodInfo = MethodInfo(method)
            expressions.forEach { expr ->
                val binaryExpr = expr as? KotlinUBinaryExpression ?: return wrongLambda(expr)
                val left = binaryExpr.leftOperand
                val right = binaryExpr.rightOperand
                val errorFound = !when {
                    left is KotlinUSimpleReferenceExpression && left.identifier == "name" ->
                        methodInfo.checkName(right)
                    left is KotlinUSimpleReferenceExpression && left.identifier == "value" ->
                        methodInfo.checkValue(right)
                    left is KotlinUArrayAccessExpression ->
                        methodInfo.checkArray(left, right)
                    else ->
                        unexpected(left)
                }
                if (errorFound) {
                    return
                }
            }
            methodInfo.checkComplete(lambda)
        }

        private fun wrongLambda(element: UElement) {
            context.report(
                ISSUE,
                element,
                context.getNameLocation(element),
                ISSUE.getBriefDescription(TextFormat.TEXT)
            )
        }

        private fun missingDebugInfoCall(element: UElement) {
            context.report(
                ISSUE,
                element,
                context.getNameLocation(element),
                "Expected debugInspectorInfo call"
            )
        }

        private fun reportMissing(missingVariables: Set<String>, element: UElement) {
            context.report(
                ISSUE,
                element,
                context.getNameLocation(element),
                "These lambda arguments are missing in the InspectorInfo: " +
                    "`${missingVariables.sorted().joinToString("`, `")}`"
            )
        }

        private fun wrongName(expectedName: String, unexpected: UElement) {
            context.report(
                ISSUE,
                unexpected,
                context.getNameLocation(unexpected),
                "Expected name of the modifier: `\"name\" = \"${expectedName}\"`"
            )
        }

        private fun wrongArgument(args: Collection<String>, unexpected: UElement) {
            val message = when (args.size) {
                0 -> "Unexpected, modifier has no arguments"
                1 -> "Expected the variable: \"${args.first()}\""
                else -> "Expected one of the variables: ${args.joinToString(", ", "\"", "\"")}"
            }
            context.report(
                ISSUE,
                unexpected,
                context.getNameLocation(unexpected),
                message
            )
        }

        private fun wrongArgumentForIndex(indexName: String, value: UElement): Boolean {
            context.report(
                ISSUE,
                value,
                context.getNameLocation(value),
                "The value should match the index name: `$indexName`"
            )
            return false
        }

        private fun unexpected(element: UElement): Boolean {
            context.report(
                ISSUE,
                element,
                context.getNameLocation(element),
                "Unexpected element found"
            )
            return false
        }

        /**
         * Method related data for checking the inspector lambda against the modifier method.
         *
         * The inspector lambda normally holds a `properties["name"] = name` expression for each
         * of the parameters of the modifier. However if the modifier has exactly one parameter,
         * and that parameter is an instance of a data class, then the arguments of the data class
         * can be used in the lambda instead of the modifier parameters.
         *
         * See the test existingInspectorInfoWithDataClassMemberValues as an example.
         */
        private inner class MethodInfo(method: UMethod) {
            val name = name(method)
            val methodArguments = Arguments(findMethodArguments(method))
            val dataClassArguments = Arguments(findDataClassMembers(method))
            var foundName = false

            fun checkName(value: UExpression): Boolean {
                if (name == literal(value)) {
                    foundName = true
                    return true
                }
                wrongName(name, value)
                return false
            }

            fun checkValue(value: UExpression): Boolean {
                val (arguments, variable) = variable(value)
                if (variable != null && arguments.expected.contains(variable)) {
                    arguments.found.add(variable)
                    return true
                }
                wrongArgument(arguments.expected, value)
                return false
            }

            /**
             * Check an array from an Inspector lambda.
             *
             * A Inspector lambda may contain expressions of the form:
             * `  properties["name"] = value
             *
             * Check the name and value against the known parameters of the modifier.
             */
            fun checkArray(key: KotlinUArrayAccessExpression, value: UExpression): Boolean {
                val index = key.indices.singleOrNull()
                val keyName = literal(index)
                val (arguments, variable) = variable(value)
                if (keyName != null &&
                    arguments.expected.contains(keyName) &&
                    keyName == variable
                ) {
                    arguments.found.add(variable)
                    return true
                }
                if (keyName == null || !arguments.expected.contains(keyName)) {
                    wrongArgument(arguments.expected, index ?: key)
                } else {
                    wrongArgumentForIndex(keyName, value)
                }
                return false
            }

            fun checkComplete(lambda: KotlinULambdaExpression) {
                if (!foundName) {
                    wrongName(name, lambda)
                } else {
                    val arguments =
                        if (dataClassArguments.found.isNotEmpty()) dataClassArguments
                        else methodArguments
                    val absentVariables = arguments.expected.minus(arguments.found)
                    if (absentVariables.isNotEmpty()) {
                        reportMissing(absentVariables, lambda)
                    }
                }
            }

            private fun literal(expr: UExpression?): String? = when (expr) {
                is KotlinStringULiteralExpression,
                is KotlinStringTemplateUPolyadicExpression -> expr.evaluate() as? String
                else -> null
            }

            /**
             * Return the name of the variable that the [expr] represents.
             *
             * This can be a simple parameter variable from the modifier.\
             * Example: `width` from `fun Modifier.preferredWidth(width: Int)`
             *
             * Or it can be a selector from a data class parameter variable from the modifier.\
             * Example: `values.start` from `fun Modifier.border(values: Borders)`
             *
             * where `Borders` is a data class with `start` as one of the value parameters.
             */
            private fun variable(expr: UExpression?): Pair<Arguments, String?> {
                return when (expr) {
                    is KotlinUSimpleReferenceExpression ->
                        Pair(methodArguments, expr.identifier)
                    is KotlinUQualifiedReferenceExpression -> {
                        val receiver = identifier(expr.receiver)
                        if (methodArguments.expected.contains(receiver)) {
                            Pair(dataClassArguments, identifier(expr.selector))
                        } else {
                            Pair(methodArguments, null)
                        }
                    }
                    else -> Pair(methodArguments, null)
                }
            }

            private fun identifier(expr: UExpression?): String? =
                (expr as? KotlinUSimpleReferenceExpression)?.identifier

            private fun findMethodArguments(method: UMethod): Set<String> =
                method.parameters.asSequence().drop(1).mapNotNull { it.name }.toSet()

            /**
             * Return all the names of the value parameters of a data class.
             *
             * We don't actually know if a class is a data class, but we can see if all the
             * parameters of the main constructor are value parameter fields.
             */
            private fun findDataClassMembers(method: UMethod): Set<String> {
                val singleParameter = method.parameters.asSequence().drop(1).singleOrNull()
                val type = (singleParameter?.type as? PsiClassReferenceType)?.reference?.resolve()
                val klass = type as? KtLightClass ?: return emptySet()
                val mainConstructor = klass.constructors.firstOrNull() ?: return emptySet()
                if (klass.fields.size != mainConstructor.parameters.size ||
                    klass.fields.any { !isValueParameter(it) }
                ) {
                    return emptySet()
                }
                return klass.fields.asSequence().map { it.name }.toSet()
            }

            private fun isValueParameter(field: PsiField): Boolean {
                val kotlinField = field as? KtLightField
                val kotlinParameter =
                    kotlinField?.lightMemberOrigin?.originalElement as? KtParameter
                return kotlinParameter?.elementType == VALUE_PARAMETER
            }
        }

        private class Arguments(val expected: Set<String>) {
            val found = mutableSetOf<String>()
        }
    }

    companion object {
        val ISSUE = Issue.create(
            id = "ModifierInspectorInfo",
            briefDescription = "Modifiers should include inspectorInfo for the Layout Inspector",
            explanation = """
                The Layout Inspector will see an instance of the usually private modifier class
                where the modifier name is gone, and the fields may not reflect directly on what
                was specified for the modifier. Instead specify the `inspectorInfo` directly on the
                modifier. See example here:
                `androidx.compose.ui.samples.InspectorInfoInComposedModifierSample`.
            """,
            category = Category.PRODUCTIVITY,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                ModifierInspectorInfoDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
