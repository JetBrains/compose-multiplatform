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
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.VALUE_PARAMETER
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UExpressionList
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UObjectLiteralExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.USwitchClauseExpression
import org.jetbrains.uast.USwitchClauseExpressionWithBody
import org.jetbrains.uast.USwitchExpression
import org.jetbrains.uast.UYieldExpression
import org.jetbrains.uast.kotlin.KotlinStringTemplateUPolyadicExpression
import org.jetbrains.uast.kotlin.KotlinStringULiteralExpression
import org.jetbrains.uast.kotlin.KotlinUArrayAccessExpression
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression
import org.jetbrains.uast.kotlin.KotlinUQualifiedReferenceExpression
import org.jetbrains.uast.kotlin.KotlinUSimpleReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val ModifierClass = "androidx.compose.ui.Modifier"
private const val ModifierCompanionClass = "androidx.compose.ui.Modifier.Companion"
private const val ModifierFile = "Modifier.kt"
private const val ComposedModifierFile = "ComposedModifier.kt"
private const val InspectableValueFile = "InspectableValue.kt"
private const val LambdaFunction = "kotlin.jvm.functions.Function1"
private const val InspectorInfoClass = "androidx.compose.ui.platform.InspectorInfo"
private const val InspectorValueInfoClass = "androidx.compose.ui.platform.InspectorValueInfo"
private const val UnitClass = "kotlin.Unit"
private const val DebugInspectorInfoFunction = "debugInspectorInfo"
private const val ThenMethodName = "then"
private const val ComposedMethodName = "composed"
private const val RememberMethodName = "remember"
private const val InspectableMethodName = "inspectable"
private const val ComposedMethodPackage = "androidx.compose.ui"
private const val RememberMethodPackage = "androidx.compose.runtime"
private val DemosPackageRegEx = "androidx\\.compose\\..+\\.demos\\..+".toRegex()
private val UiPackage = FqName("androidx.compose.ui")
private val PlatformPackage = FqName("androidx.compose.ui.platform")

/**
 * Lint [Detector] to ensure that we are creating debug information for the layout inspector on
 * all modifiers. For example in:
 * ```
 *   fun Modifier.width(width: Dp) = this.then(SizeModifier(width))
 * ```
 *
 * The layout inspector will not know that the name is `width` and the `width` member
 * may not be a field in SizeModifier that we can see via reflection.
 *
 * To supply debug information to the layout inspector include an InspectorInfo lambda like this:
 * ```
 *   fun Modifier.width(width: Dp) = this.then(
 *      SizeModifier(
 *          width = width,
 *          inspectorInfo = debugInspectorInfo {
 *              name = "width",
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
 *   fun Modifier.size(width: Dp, height: Dp) = this.then(
 *      SizeModifier(
 *          width = width,
 *          inspectorInfo = debugInspectorInfo {
 *              name = "width",
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
        private val returnVisitor = ReturnVisitor()
        private val builderVisitor = ModifierBuilderVisitor()
        private val modifierVisitor = ModifierVisitor()
        private val debugInspectorVisitor = DebugInspectorVisitor()
        private val lambdaVisitor = InspectorLambdaVisitor()
        private var methodInfo: MethodInfo? = null

        override fun visitMethod(node: UMethod) {
            if (node.isInFile(ModifierFile, UiPackage) ||
                node.isInFile(ComposedModifierFile, UiPackage) ||
                node.isInFile(InspectableValueFile, PlatformPackage) ||
                firstParameterType(node) != ModifierClass ||
                node.returnType?.canonicalText != ModifierClass ||
                DemosPackageRegEx.matches(node.containingClass?.qualifiedName ?: "")
            ) {
                // Ignore the method if it isn't a method on Modifier returning a Modifier,
                // or if the method is defined in Modifier.kt or ComposedModifier.kt
                return
            }
            methodInfo = MethodInfo(node)
            node.uastBody?.accept(returnVisitor)
            methodInfo = null
        }

        private fun UMethod.isInFile(fileName: String, packageName: FqName): Boolean {
            val file = containingFile as? KtFile ?: return false
            return file.name == fileName && file.packageFqName == packageName
        }

        private fun firstParameterType(method: UMethod): String? =
            (method.parameters.firstOrNull() as? KtLightParameter)?.type?.canonicalText

        private fun isModifierType(type: PsiType?): Boolean =
            InheritanceUtil.isInheritor(type, ModifierClass)

        // Disregard the mangled part of a method with inline class parameters.
        // TODO: change this to an API call when a demangle function is available
        private fun name(method: PsiMethod): String =
            method.name.substringBefore('-')

        private fun wildcardType(type: PsiType?): String? =
            (type as? PsiWildcardType)?.bound?.canonicalText

        private fun isThenFunctionCall(node: UQualifiedReferenceExpression): Boolean {
            if (!isModifierType(node.receiver.getExpressionType())) return false
            val then = node.selector as? KotlinUFunctionCallExpression ?: return false
            return then.methodName == ThenMethodName &&
                then.valueArguments.size == 1 &&
                isModifierType(then.valueArguments.first().getExpressionType())
        }

        private fun isComposeFunctionCall(node: UCallExpression): Boolean =
            node.methodName == ComposedMethodName &&
                node.receiverType?.canonicalText == ModifierClass &&
                node.returnType?.canonicalText == ModifierClass &&
                methodPackageName(node) == ComposedMethodPackage

        private fun isRememberFunctionCall(node: UCallExpression): Boolean =
            node.methodName == RememberMethodName &&
                node.receiver == null &&
                isModifierType(node.returnType) &&
                methodPackageName(node) == RememberMethodPackage

        private fun isInspectableModifier(node: UCallExpression): Boolean =
            node.methodName == InspectableMethodName &&
                node.receiverType?.canonicalText in listOf(ModifierClass, ModifierCompanionClass) &&
                node.returnType?.canonicalText == ModifierClass &&
                methodPackageName(node) == PlatformPackage.asString()

        // Return true if this is a lambda expression of the type: "InspectorInfo.() -> Unit"
        private fun isInspectorInfoLambdaType(type: PsiType?): Boolean {
            val referenceType = type as? PsiClassReferenceType ?: return false
            return referenceType.rawType().canonicalText == LambdaFunction &&
                referenceType.parameterCount == 2 &&
                wildcardType(referenceType.parameters[0]) == InspectorInfoClass &&
                wildcardType(referenceType.parameters[1]) !== UnitClass
        }

        private fun isDebugInspectorInfoCall(node: UCallExpression): Boolean =
            node.methodName == DebugInspectorInfoFunction &&
                node.valueArgumentCount == 1 &&
                isInspectorInfoLambdaType(node.valueArguments.first().getExpressionType()) &&
                isInspectorInfoLambdaType(node.returnType)

        private fun methodPackageName(node: UCallExpression): String? =
            (node.resolve()?.containingFile as? PsiJavaFile)?.packageName

        private fun wrongLambda(element: UElement) =
            report(element, ISSUE.getBriefDescription(TextFormat.TEXT))

        private fun missingDebugInfoCall(element: UElement) =
            report(element, message = "Expected debugInspectorInfo call")

        private fun reportMissing(missingVariables: Set<String>, element: UElement) = report(
            element = element,
            message = "These lambda arguments are missing in the InspectorInfo: " +
                "`${missingVariables.sorted().joinToString("`, `")}`"
        )

        private fun wrongName(expectedName: String, unexpected: UElement) = report(
            element = unexpected,
            message = "Expected name of the modifier: `\"name\" = \"${expectedName}\"`"
        )

        private fun wrongArgument(args: Collection<String>, unexpected: UElement) {
            val message = when (args.size) {
                0 -> "Unexpected, modifier has no arguments"
                1 -> "Expected the variable: \"${args.first()}\""
                else -> "Expected one of the variables: ${args.joinToString(", ", "\"", "\"")}"
            }
            report(unexpected, message = message)
        }

        private fun wrongArgumentForIndex(indexName: String, value: UElement) =
            report(value, message = "The value should match the index name: `$indexName`")

        private fun unexpected(element: UElement) =
            report(element, message = "Unexpected element found")

        private fun report(element: UElement, message: String) {
            context.report(
                ISSUE,
                element,
                context.getNameLocation(element),
                message
            )
            methodInfo?.foundError = true
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
            var foundError = false

            fun checkName(value: UExpression) {
                if (name == literal(value)) {
                    foundName = true
                } else if (!foundError) {
                    wrongName(name, value)
                }
            }

            fun checkValue(value: UExpression, inConditional: Boolean) {
                val (arguments, variable) = variable(value)
                if (variable != null && arguments.expected.contains(variable)) {
                    arguments.found.add(variable)
                } else if (!foundError && !(inConditional && isArgumentReceiver(value))) {
                    wrongArgument(arguments.expected, value)
                }
            }

            /**
             * Check an array from an Inspector lambda.
             *
             * A Inspector lambda may contain expressions of the form:
             * `  properties["name"] = value
             *
             * Check the name and value against the known parameters of the modifier.
             */
            fun checkArray(
                keyExpr: KotlinUArrayAccessExpression,
                value: UExpression,
                inConditional: Boolean
            ) {
                if (foundError) {
                    return
                }
                val index = keyExpr.indices.singleOrNull()
                val key = literal(index)
                val (arguments, variable) = variable(value)
                when {
                    key != null && arguments.expected.contains(key) && key == variable ->
                        arguments.found.add(variable)
                    inConditional && isArgumentReceiver(value) ->
                        {} // ignore extra information
                    key == null || !arguments.expected.contains(key) ->
                        wrongArgument(arguments.expected, index ?: keyExpr)
                    else ->
                        wrongArgumentForIndex(key, value)
                }
            }

            fun checkComplete(lambda: UExpression) {
                if (foundError) {
                    return
                } else if (!foundName) {
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

            private fun isArgumentReceiver(value: UExpression): Boolean {
                val reference = value as? KotlinUQualifiedReferenceExpression ?: return false
                val (arguments, variable) = variable(reference.receiver)
                return arguments.expected.contains(variable)
            }

            /**
             * Return the name of the variable that the [expr] represents.
             *
             * This can be a simple parameter variable from the modifier.\
             * Example: `width` from `fun Modifier.width(width: Int)`
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
                val valueParameters = klass.fields.asSequence()
                    .filter { isValueParameter(it) }.map { it.name }.toSet()
                val constructorParameters = mainConstructor.parameters.asSequence()
                    .map { it.name }.toSet()
                if (constructorParameters != valueParameters) {
                    return emptySet()
                }
                return valueParameters
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

        /**
         * Finds all return expressions, ignores all other elements.
         */
        private inner class ReturnVisitor : AbstractUastVisitor() {
            override fun visitReturnExpression(node: UReturnExpression): Boolean {
                node.returnExpression?.accept(builderVisitor)
                return true
            }

            // Ignore returns from a lambda expression
            override fun visitLambdaExpression(node: ULambdaExpression): Boolean = true
        }

        /**
         * Find and check known Modifier builder expressions.
         *
         * The expression is known to be the return expression from a return statement.
         *
         * Currently the only check the following expressions:
         * - Modifier.then(Modifier)
         * - Modifier.composed(InspectorInfoLambda,factory)
         * - everything else is ignored
         */
        private inner class ModifierBuilderVisitor : AbstractUastVisitor() {
            override fun visitQualifiedReferenceExpression(
                node: UQualifiedReferenceExpression
            ): Boolean {
                if (isThenFunctionCall(node)) {
                    node.receiver.accept(this)
                    val then = node.selector as KotlinUFunctionCallExpression
                    then.valueArguments.first().accept(modifierVisitor)
                    return true
                }
                return super.visitQualifiedReferenceExpression(node)
            }

            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (isComposeFunctionCall(node)) {
                    val inspectorInfo = node.valueArguments
                        .find { isInspectorInfoLambdaType(it.getExpressionType()) }
                    if (inspectorInfo == null) {
                        wrongLambda(node)
                    } else {
                        inspectorInfo.accept(debugInspectorVisitor)
                    }
                    return true
                }
                return super.visitCallExpression(node)
            }
        }

        /**
         * Find and check the Modifier constructor.
         *
         * The expression is known to be inside one of the Modifier builder expressions accepted
         * by [ModifierBuilderVisitor].
         *
         * Currently the only accepted expressions are of the form:
         * - SomeClassExtendingModifier(p1,p2,p3,p4,inspectorInfoLambda)
         * - SomeClass.InnerModifier(p1,p2,p3,p4,inspectorInfoLambda)
         * - object : Modifier, InspectorValueInfoClass(inspectorInfoLambda)
         * - remember { }
         * - Modifier
         * - if-then-else (with a modifier constructor in both then and else)
         * - when (with a modifier constructor in each of the when clauses)
         * All other expressions are considered errors.
         */
        private inner class ModifierVisitor : UnexpectedVisitor({ wrongLambda(it) }) {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                val info: UExpression? = node.valueArguments.firstOrNull {
                    isInspectorInfoLambdaType(it.getExpressionType())
                }
                if (info != null) {
                    info.accept(debugInspectorVisitor)
                    return true
                }
                if (isRememberFunctionCall(node)) {
                    val lambda = node.valueArguments.singleOrNull() as? ULambdaExpression
                    val body = lambda?.body as? UBlockExpression
                    val ret = body?.expressions?.firstOrNull() as? UReturnExpression
                    val definition = ret?.returnExpression ?: return super.visitCallExpression(node)
                    definition.accept(this)
                    return true
                }
                if (isModifierType(node.receiverType) && isModifierType(node.returnType)) {
                    // For now accept all other calls. Assume that the method being called
                    // will add inspector information.
                    return true
                }
                return super.visitCallExpression(node)
            }

            override fun visitQualifiedReferenceExpression(
                node: UQualifiedReferenceExpression
            ): Boolean {
                node.selector.accept(this)
                return true
            }

            override fun visitObjectLiteralExpression(node: UObjectLiteralExpression): Boolean {
                if (node.valueArgumentCount == 1 &&
                    node.declaration.uastSuperTypes.any {
                        it.getQualifiedName() == InspectorValueInfoClass
                    }
                ) {
                    node.valueArguments.first().accept(debugInspectorVisitor)
                    return true
                }
                return super.visitObjectLiteralExpression(node)
            }

            override fun visitSimpleNameReferenceExpression(
                node: USimpleNameReferenceExpression
            ): Boolean {
                // Accept any variable including Modifier
                return true
            }

            override fun visitIfExpression(node: UIfExpression): Boolean {
                node.thenExpression?.accept(this)
                node.elseExpression?.accept(this)
                return true
            }

            override fun visitSwitchExpression(node: USwitchExpression): Boolean {
                node.body.accept(this)
                return true
            }

            override fun visitSwitchClauseExpression(node: USwitchClauseExpression): Boolean {
                (node as? USwitchClauseExpressionWithBody)?.let {
                    it.body.expressions.last().accept(this)
                    return true
                }
                return super.visitSwitchClauseExpression(node)
            }

            override fun visitYieldExpression(node: UYieldExpression): Boolean {
                return false
            }

            override fun visitExpressionList(node: UExpressionList): Boolean {
                return false
            }

            override fun visitBlockExpression(node: UBlockExpression): Boolean {
                node.expressions.lastOrNull()?.accept(this)
                return true
            }
        }

        /**
         * Expect debugInspectorInfo factory method:
         * - debugInspectorInfo { inspectorInfoLambda }
         * For all other expressions: complain about the missing debugInspectorInfo call.
         */
        private inner class DebugInspectorVisitor :
            UnexpectedVisitor({ missingDebugInfoCall(it) }) {

            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (isDebugInspectorInfoCall(node)) {
                    node.valueArguments.single().accept(lambdaVisitor)
                    return true
                }
                return super.visitCallExpression(node)
            }
        }

        /**
         * Find and check the InspectorInfo lambda.
         *
         * The expression is known to be a InspectorInfo lambda found inside a debugInspectorInfo
         * call from one of the modifier constructors accepted by [ModifierVisitor].
         *
         * Check the name, value, and properties against the original modifier definition expressed
         * by [methodInfo] found by [ModifierHandler] above. After the lambda expression check that
         * all the elements of [methodInfo] was found.
         */
        private inner class InspectorLambdaVisitor : UnexpectedVisitor({ unexpected(it) }) {
            // We allow alternate values inside conditionals.
            // Example see the test: existingInspectorInfoWffithConditionals.
            var inConditional = false

            override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                val left = node.leftOperand
                val right = node.rightOperand
                when {
                    left is KotlinUSimpleReferenceExpression && left.identifier == "name" ->
                        methodInfo?.checkName(right)
                    left is KotlinUSimpleReferenceExpression && left.identifier == "value" ->
                        methodInfo?.checkValue(right, inConditional)
                    left is KotlinUArrayAccessExpression ->
                        methodInfo?.checkArray(left, right, inConditional)
                    else ->
                        unexpected(left)
                }
                return true
            }

            override fun visitLambdaExpression(node: ULambdaExpression): Boolean {
                // accept, and recurse
                return false
            }

            override fun visitIfExpression(node: UIfExpression): Boolean {
                inConditional = true
                try {
                    node.thenExpression?.accept(this)
                    node.elseExpression?.accept(this)
                } finally {
                    inConditional = false
                }
                return true
            }

            override fun visitBlockExpression(node: UBlockExpression): Boolean {
                // accept, and recurse
                return false
            }

            override fun afterVisitLambdaExpression(node: ULambdaExpression) {
                methodInfo?.checkComplete(node)
            }
        }

        private abstract inner class UnexpectedVisitor(
            private val error: (node: UElement) -> Unit
        ) : AbstractUastVisitor() {

            override fun visitElement(node: UElement): Boolean {
                error(node)
                return true
            }
        }
    }

    companion object {
        val ISSUE = Issue.create(
            id = "ModifierInspectorInfo",
            briefDescription = "Modifier missing inspectorInfo",
            explanation =
                """
                The Layout Inspector will see an instance of the usually private modifier class \
                where the modifier name is gone, and the fields may not reflect directly on what \
                was specified for the modifier. Instead specify the `inspectorInfo` directly on \
                the modifier. See example here:
                `androidx.compose.ui.samples.InspectorInfoInComposedModifierSample`.""",
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
