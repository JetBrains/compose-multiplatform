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
import org.jetbrains.uast.UMethod
import java.util.EnumSet
import java.util.Locale

/**
 * [Detector] that checks the naming of @Composable functions for consistency with guidelines.
 *
 * - @Composable functions that return Unit should follow typical class naming (PascalCase)
 * - @Composable functions with a return type should follow typical function naming (camelCase)
 */
class ComposableNamingDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            // Ignore non-composable functions
            if (!node.isComposable) return

            // Ignore operator functions and any override of an operator function, as their name
            // is case sensitive and cannot be changed
            if (context.evaluator.isOperator(node)) return
            if (node.findSuperMethods().any { context.evaluator.isOperator(it) }) return

            val name = node.name

            val capitalizedFunctionName = name.first().isUpperCase()

            if (node.returnsUnit) {
                if (!capitalizedFunctionName) {
                    val capitalizedName = name.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    }
                    context.report(
                        ComposableNaming,
                        node,
                        context.getNameLocation(node),
                        "Composable functions that return Unit should start with an " +
                            "uppercase letter",
                        LintFix.create()
                            .replace()
                            .name("Change to $capitalizedName")
                            .text(name)
                            .with(capitalizedName)
                            .autoFix()
                            .build()
                    )
                }
            } else {
                if (capitalizedFunctionName) {
                    val lowercaseName = name.replaceFirstChar { it.lowercase(Locale.getDefault()) }
                    context.report(
                        ComposableNaming,
                        node,
                        context.getNameLocation(node),
                        "Composable functions with a return type should start with a " +
                            "lowercase letter",
                        LintFix.create()
                            .replace()
                            .name("Change to $lowercaseName")
                            .text(name)
                            .with(lowercaseName)
                            .autoFix()
                            .build()
                    )
                }
            }
        }
    }

    companion object {
        val ComposableNaming = Issue.create(
            "ComposableNaming",
            "Incorrect naming for @Composable functions",
            "@Composable functions without a return type should use similar naming to " +
                "classes, starting with an uppercase letter and ending with a noun. @Composable " +
                "functions with a return type should be treated as normal Kotlin functions, " +
                "starting with a lowercase letter.",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                ComposableNamingDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
