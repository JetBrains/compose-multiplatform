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

package androidx.compose.animation.core.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Names
import androidx.compose.lint.findUnreferencedParameters
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression
import java.util.EnumSet

/**
 * [Detector] that checks `Transition` usages for correctness.
 *
 * - Transition animate functions (such as animateFloat) provide a `targetValueByState` lambda
 * that contains a `state` parameter containing the state to get the value for. It is always an
 * error to not use this parameter, so this detector ensures that the parameter is always at
 * least referenced, preventing common errors such as:
 * `transition.animateFloat { if (someOtherState) 0f else 1f }`
 */
class TransitionDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            val method = node.resolve() ?: return
            // Ignore if this isn't an extension on Transition that starts with `animate`
            val receiverType = node.receiverType as? PsiClassReferenceType ?: return
            if (!receiverType.rawType().equalsToText(Transition.javaFqn)) return
            if (node.methodName?.startsWith("animate") != true) return

            // Our heuristic looks for lambda parameters with `(S) -> *`, this should catch all
            // similar methods with (as of yet) no false positives.
            val matchingLambdaArguments = node.valueArguments
                .filterIsInstance<ULambdaExpression>()
                .filter { argument ->
                    val parameters = argument.valueParameters
                    if (parameters.size != 1) return@filter false
                    val parameter = parameters.first()
                    // If the type is a primitive, the generic type on Transition will be the
                    // Boxed version, but the type of the lambda argument is most likely the
                    // primitive type
                    val boxedType = when (val type = parameter.type) {
                        is PsiPrimitiveType -> type.getBoxedType(method)
                        else -> type
                    }
                    boxedType == receiverType.parameters[0]
                }

            // Make sure that the parameter (S) is referenced inside the lambda body.
            matchingLambdaArguments.forEach { lambda ->
                lambda.findUnreferencedParameters().forEach { unreferencedParameter ->
                    val location = unreferencedParameter.parameter
                        ?.let { context.getLocation(it) }
                        ?: context.getLocation(lambda)
                    val name = unreferencedParameter.name
                    context.report(
                        UnusedTransitionTargetStateParameter,
                        node,
                        location,
                        "Target state parameter `$name` is not used"
                    )
                }
            }
        }
    }

    companion object {
        val UnusedTransitionTargetStateParameter = Issue.create(
            "UnusedTransitionTargetStateParameter",
            "Transition.animate* calls should use the provided targetState when defining values",
            "Transition.animate* functions provide a target state parameter in the lambda that " +
                "will be used to calculate the value for a given state. This target state " +
                "parameter in the lambda may or may not be the same as the actual state, as the" +
                " animation system occasionally needs to look up target values for other states " +
                "to do proper seeking/tooling preview. Relying on other state than the provided " +
                "`targetState` could also result in unnecessary recompositions. Therefore, it is " +
                "generally considered an error if this `targetState` parameter is not used.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                TransitionDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private val Transition = Name(Names.AnimationCore.PackageName, "Transition")
