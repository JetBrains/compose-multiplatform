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

package androidx.compose.runtime.lint

import androidx.compose.lint.isComposable
import androidx.compose.lint.returnsUnit
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
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import java.util.EnumSet

/**
 * [Detector] that checks composable lambda parameters inside composable functions for
 * consistency with guidelines.
 *
 * Composable functions that have exactly one composable lambda parameter must:
 * - name this parameter `content`
 * - place this parameter at the end, so it can be used as a trailing lambda
 */
class ComposableLambdaParameterDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            // Ignore non-composable functions
            if (!node.isComposable) return

            // Ignore non-unit composable functions
            if (!node.returnsUnit) return

            /**
             * Small class to hold information from lambda properties needed for lint checks.
             */
            class ComposableLambdaParameterInfo(
                val parameter: UParameter,
                val functionType: KtFunctionType
            )

            // Filter all parameters to only contain composable lambda parameters
            val composableLambdaParameters = node.uastParameters.mapNotNull { parameter ->
                // If it is not a KtParameter, it could be the implicit receiver 'parameter' for
                // an extension function - just ignore it.
                val ktParameter = parameter.sourcePsi as? KtParameter ?: return@mapNotNull null

                val isComposable = parameter.isComposable

                val functionType = when (val type = ktParameter.typeReference!!.typeElement) {
                    is KtFunctionType -> type
                    is KtNullableType -> type.innerType as? KtFunctionType
                    else -> null
                }

                if (functionType != null && isComposable) {
                    ComposableLambdaParameterInfo(parameter, functionType)
                } else {
                    null
                }
            }

            // Only look at functions with exactly 1 composable lambda parameter. This detector
            // does not apply to functions with no composable lambda parameters, and there isn't
            // an easily lintable rule for functions with multiple.
            if (composableLambdaParameters.size != 1) return

            val parameterInfo = composableLambdaParameters.first()

            val parameter = parameterInfo.parameter

            val name = parameter.name

            // Need to strongly type this or else Kotlinc cannot resolve overloads for
            // getNameLocation
            val uElement: UElement = parameter

            // Ignore composable lambda parameters with parameters, such as
            // itemContent: @Composable (item: T) -> Unit - in this case content is not required
            // as a name and more semantically meaningful names such as `itemContent` are preferred.
            if (name != "content" && parameterInfo.functionType.parameters.isEmpty()) {
                context.report(
                    ComposableLambdaParameterNaming,
                    node,
                    context.getNameLocation(uElement),
                    "Composable lambda parameter should be named `content`",
                    LintFix.create()
                        .replace()
                        .name("Rename $name to content")
                        .text(name)
                        .with("content")
                        .autoFix()
                        .build()
                )
            }

            if (parameter !== node.uastParameters.last()) {
                context.report(
                    ComposableLambdaParameterPosition,
                    node,
                    context.getNameLocation(uElement),
                    "Composable lambda parameter should be the last parameter so it can be used " +
                        "as a trailing lambda"
                    // Hard to make a lint fix for this and keep parameter formatting, so ignore it
                )
            }
        }
    }

    companion object {
        val ComposableLambdaParameterNaming = Issue.create(
            id = "ComposableLambdaParameterNaming",
            briefDescription = "Primary composable lambda parameter not named `content`",
            explanation = "Composable functions with only one composable lambda parameter should " +
                "use the name `content` for the parameter.",
            category = Category.CORRECTNESS,
            priority = 3,
            severity = Severity.WARNING,
            enabledByDefault = false,
            implementation = Implementation(
                ComposableLambdaParameterDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )

        val ComposableLambdaParameterPosition = Issue.create(
            id = "ComposableLambdaParameterPosition",
            briefDescription = "Non-trailing primary composable lambda parameter",
            explanation = "Composable functions with only one composable lambda parameter should " +
                "place the parameter at the end of the parameter list, so it can be used as a " +
                "trailing lambda.",
            category = Category.CORRECTNESS,
            priority = 3,
            severity = Severity.WARNING,
            enabledByDefault = false,
            implementation = Implementation(
                ComposableLambdaParameterDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
