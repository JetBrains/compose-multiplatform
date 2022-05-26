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

import androidx.compose.lint.isInvokedWithinComposable
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
import com.android.tools.lint.detector.api.UastLintUtils.Companion.tryResolveUDeclaration
import java.util.EnumSet
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getContainingUClass

class LazyLayoutStateReadInCompositionDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(
            UQualifiedReferenceExpression::class.java,
            USimpleNameReferenceExpression::class.java
        )

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            // Qualified dot call (e.g. state.firstVisibleItemIndex)
            override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
                val name = node.resolvedName
                if (name !in ObservableGetterNames) {
                    return
                }

                val receiverFqName = node.receiver.getExpressionType()?.canonicalText
                if (receiverFqName !in LazyStateFqNames) {
                    return
                }

                if (node.isInvokedWithinComposable()) {
                    context.reportObservableNodeInComposition(node)
                }
            }

            // Usages with extension receiver
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                if (node.uastParent is UQualifiedReferenceExpression) {
                    // handled in the UQualifiedReferenceExpression case
                    return
                }

                val name = node.identifier
                if (name !in ObservablePropertyNames) {
                    return
                }

                val receiverClsFqName = (node.tryResolveUDeclaration() as? UMethod)
                    ?.getContainingUClass()
                    ?.qualifiedName

                if (receiverClsFqName !in LazyStateFqNames) {
                    return
                }

                if (node.isInvokedWithinComposable()) {
                    context.reportObservableNodeInComposition(node)
                }
            }

            private fun JavaContext.reportObservableNodeInComposition(node: UElement) {
                report(
                    FrequentlyChangedStateReadInComposition,
                    node,
                    getNameLocation(node),
                    "Frequently changing state should not be directly read in composable function",
                    LintFix.create()
                        .alternatives()
                        .add(derivedStateLintFix(node))
                        .add(snapshotFlowLintFix(node))
                        .build()
                )
            }
        }

    private fun JavaContext.derivedStateLintFix(node: UElement): LintFix {
        val text = node.sourcePsi?.text
        return LintFix.create()
            .replace()
            .range(getLocation(node))
            .name("Wrap with derivedStateOf")
            .text(text)
            .with(
                "androidx.compose.runtime.remember { " +
                    "androidx.compose.runtime.derivedStateOf { $text } }"
            )
            .shortenNames()
            .build()
    }

    private fun JavaContext.snapshotFlowLintFix(node: UElement): LintFix {
        val receiverText = when (node) {
            is UQualifiedReferenceExpression -> {
                node.receiver.sourcePsi?.text
            }
            else -> "this"
        }
        val expressionText = node.sourcePsi?.text
        return LintFix.create()
            .replace()
            .range(getLocation(node))
            .name("Collect with snapshotFlow")
            .text(expressionText)
            .with(
                """androidx.compose.runtime.LaunchedEffect($receiverText) {
                    androidx.compose.runtime.snapshotFlow { $expressionText }
                        .collect { TODO("Collect the state") }
                }""".trimIndent()
            )
            .shortenNames()
            .reformat(true)
            .build()
    }

    companion object {
        val LazyStateFqNames = listOf(
            FoundationNames.Lazy.LazyListState.javaFqn,
            FoundationNames.Lazy.Grid.LazyGridState.javaFqn,
        )
        val ObservableGetterNames = listOf(
            "getFirstVisibleItemIndex",
            "getFirstVisibleItemScrollOffset",
            "getLayoutInfo"
        )
        val ObservablePropertyNames = listOf(
            "firstVisibleItemIndex",
            "firstVisibleItemScrollOffset",
            "layoutInfo"
        )
        val FrequentlyChangedStateReadInComposition = Issue.create(
            id = "FrequentlyChangedStateReadInComposition",
            briefDescription =
                "Frequently changing state should not " +
                    " be directly read in composable function",
            explanation =
                "This property is observable and is updated after every scroll or remeasure. " +
                "If you use it in the composable function directly, " +
                "it will be recomposed on every change, causing potential performance issues " +
                "including infinity recomposition loops. " +
                "Prefer wrapping it with derivedStateOf to use calculation based on this " +
                "property in composition or collect changes inside LaunchedEffect instead.",
            category = Category.PERFORMANCE,
            priority = 3,
            severity = Severity.WARNING,
            implementation = Implementation(
                LazyLayoutStateReadInCompositionDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}