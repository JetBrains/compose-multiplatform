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

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression
import org.jetbrains.uast.kotlin.KotlinULambdaExpression
import org.jetbrains.uast.kotlin.declarations.KotlinUMethod
import java.util.EnumSet

/**
 * [Detector] that checks `async` and `launch` calls to make sure they don't happen inside the
 * body of a composable function / lambda.
 */
class ComposableCoroutineCreationDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames() = listOf(AsyncShortName, LaunchShortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val packageName = (method.containingFile as? PsiJavaFile)?.packageName
        if (packageName != CoroutinePackageName) return
        val name = method.name

        var expression: UElement? = node

        // Limit the search depth in case of an error - in most cases the depth should be
        // fairly shallow unless there are many if / else / while statements.
        var depth = 0

        // Find the parent function / lambda this call expression is inside
        while (depth < 10) {
            expression = expression?.uastParent

            // TODO: this won't handle inline functions, but we also don't know if they are
            // inline when they are defined in bytecode because this information doesn't
            // exist in PSI. If this information isn't added to PSI / UAST, we would need to
            // manually parse the @Metadata annotation.
            when (expression) {
                // In the body of a lambda
                is KotlinULambdaExpression -> {
                    if (expression.isComposable) {
                        context.report(
                            CoroutineCreationDuringComposition,
                            node,
                            context.getNameLocation(node),
                            "Calls to $name should happen inside a LaunchedEffect and " +
                                "not composition"
                        )
                        return
                    }
                    val parent = expression.uastParent
                    if (parent is KotlinUFunctionCallExpression && parent.isDeclarationInline) {
                        // We are now in a non-composable lambda parameter inside an inline function
                        // For example, a scoping function such as run {} or apply {} - since the
                        // body will be inlined and this is a common case, try to see if there is
                        // a parent composable function above us, since it is still most likely
                        // an error to call these methods inside an inline function, inside a
                        // Composable function.
                        continue
                    } else {
                        return
                    }
                }
                // In the body of a function
                is KotlinUMethod -> {
                    if (expression.hasAnnotation("androidx.compose.runtime.Composable")) {
                        context.report(
                            CoroutineCreationDuringComposition,
                            node,
                            context.getNameLocation(node),
                            "Calls to $name should happen inside a LaunchedEffect and " +
                                "not composition"
                        )
                    }
                    return
                }
            }
            depth++
        }
    }

    companion object {
        val CoroutineCreationDuringComposition = Issue.create(
            "CoroutineCreationDuringComposition",
            "Calls to `async` or `launch` should happen inside a LaunchedEffect and not " +
                "composition",
            "Creating a coroutine with `async` or `launch` during composition is often incorrect " +
                "- this means that a coroutine will be created even if the composition fails / is" +
                " rolled back, and it also means that multiple coroutines could end up mutating " +
                "the same state, causing inconsistent results. Instead, use `LaunchedEffect` and " +
                "create coroutines inside the suspending block. The block will only run after a " +
                "successful composition, and will cancel existing coroutines when `key` changes, " +
                "allowing correct cleanup.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                ComposableCoroutineCreationDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
