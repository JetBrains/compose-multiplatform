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

package androidx.compose.runtime.lint

import androidx.compose.lint.Name
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
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.visitor.AbstractUastVisitor
import java.util.EnumSet

/**
 * [Detector] that checks calls to produceState, to make sure that the producer lambda writes to
 * MutableState#value.
 *
 * We also check to see if the lambda calls an external function that accepts a parameter of type
 * ProduceStateScope / MutableState to avoid false positives in case there is a utility function
 * that writes to MutableState#value.
 */
class ProduceStateDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> =
        listOf(Names.Runtime.ProduceState.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.isInPackageName(Names.Runtime.PackageName)) {
            // The ProduceStateScope lambda
            val producer = node.valueArguments.find {
                node.getParameterForArgument(it)?.name == "producer"
            } ?: return

            var referencesReceiver = false
            var callsSetValue = false

            producer.accept(object : AbstractUastVisitor() {
                val mutableStatePsiClass =
                    context.evaluator.findClass(Names.Runtime.MutableState.javaFqn)

                /**
                 * Visit function calls to see if the functions have a parameter of MutableState
                 * / ProduceStateScope. If they do, we cannot know for sure whether those
                 * functions internally call setValue, so we avoid reporting an error to avoid
                 * false positives.
                 */
                override fun visitCallExpression(
                    node: UCallExpression
                ): Boolean {
                    val resolvedMethod = node.resolve() ?: return false
                    return resolvedMethod.parameterList.parameters.any { parameter ->
                        val type = parameter.type

                        // Is the parameter type ProduceStateScope or a subclass
                        if (type.inheritsFrom(ProduceStateScopeName)) {
                            referencesReceiver = true
                        }

                        // Is the parameter type MutableState
                        if (mutableStatePsiClass != null &&
                            context.evaluator.getTypeClass(type) == mutableStatePsiClass) {
                            referencesReceiver = true
                        }

                        referencesReceiver
                    }
                }

                /**
                 * Visit any simple name reference expressions to see if there is a reference to
                 * `value` that resolves to a call to MutableState#setValue.
                 */
                override fun visitSimpleNameReferenceExpression(
                    node: USimpleNameReferenceExpression
                ): Boolean {
                    if (node.identifier != "value") return false
                    val resolvedMethod = node.tryResolve() as? PsiMethod ?: return false
                    if (resolvedMethod.name == "setValue" &&
                        resolvedMethod.containingClass?.inheritsFrom(
                            Names.Runtime.MutableState
                        ) == true
                    ) {
                        callsSetValue = true
                    }
                    return callsSetValue
                }
            })

            if (!callsSetValue && !referencesReceiver) {
                context.report(
                    ProduceStateDoesNotAssignValue,
                    node,
                    context.getNameLocation(node),
                    "produceState calls should assign `value` inside the producer lambda"
                )
            }
        }
    }

    companion object {
        val ProduceStateDoesNotAssignValue = Issue.create(
            "ProduceStateDoesNotAssignValue",
            "produceState calls should assign `value` inside the producer lambda",
            "produceState returns an observable State using values assigned inside the producer " +
                "lambda. If the lambda never assigns (i.e `value = foo`), then the State will " +
                "never change. Make sure to assign a value when the source you are producing " +
                "values from changes / emits a new value. For sample usage see the produceState " +
                "documentation.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                ProduceStateDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private val ProduceStateScopeName = Name(Names.Runtime.PackageName, "ProduceStateScope")
