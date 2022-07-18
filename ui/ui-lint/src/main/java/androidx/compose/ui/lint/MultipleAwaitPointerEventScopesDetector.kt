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

package androidx.compose.ui.lint

import androidx.compose.lint.Names
import androidx.compose.lint.inheritsFrom
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getContainingDeclaration
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.uast.withContainingElements

class MultipleAwaitPointerEventScopesDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> =
        listOf(Names.Ui.Pointer.AwaitPointerEventScope.shortName)

    // Our approach is to go up the file tree and find all awaitPointerEventScopes under
    // a given parent. We might report the same node more than once, so we keep track of reported
    // nodes to avoid duplicated reporting.
    private val reportedNodes = mutableSetOf<UElement>()

    override fun afterCheckFile(context: Context) {
        reportedNodes.clear()
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // Not defined in UI Pointer Input
        if (!method.isInPackageName(Names.Ui.Pointer.PackageName)) return
        val containingDeclaration = node.getContainingDeclaration()

        // The element we will look inside
        val boundaryElement = node.withContainingElements.first {
            // Reached the containing function / lambda
            // or Found a modifier expression - don't look outside the scope of this modifier
            it == containingDeclaration || (it is UExpression) && it.getExpressionType()
                ?.inheritsFrom(Names.Ui.Modifier) == true
        }

        val awaitPointerEventCalls = searchAwaitPointerScopeCalls(boundaryElement)

        // loop block contains the correct amount of awaitPointerEventScope calls (1 or none)
        if (awaitPointerEventCalls <= 1) return

        // If loop contains more than one awaitPointerEventScope we should report if we haven't done
        // so before.
        if (!reportedNodes.contains(node)) {
            context.report(
                MultipleAwaitPointerEventScopes,
                node,
                context.getNameLocation(node),
                ErrorMessage
            )
            reportedNodes.add(node)
        }
    }

    private fun searchAwaitPointerScopeCalls(parent: UElement): Int {
        var awaitPointerEventCallsCount = 0
        parent.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                val method = node.tryResolve() as? PsiMethod ?: return false
                if (!method.isInPackageName(Names.Ui.Pointer.PackageName)) return false

                if (method.name == Names.Ui.Pointer.AwaitPointerEventScope.shortName) {
                    awaitPointerEventCallsCount++
                }
                return false
            }
        })

        return awaitPointerEventCallsCount
    }

    companion object {
        const val IssueId: String = "MultipleAwaitPointerEventScopes"
        const val ErrorMessage =
            "Suspicious use of multiple awaitPointerEventScope blocks. Using " +
                "multiple awaitPointerEventScope blocks may cause some input events to be dropped."
        val MultipleAwaitPointerEventScopes = Issue.create(
            IssueId,
            ErrorMessage,
            "Pointer Input events are queued inside awaitPointerEventScope. Multiple " +
                "calls to awaitPointerEventScope may exit the scope. During this time " +
                "there is no guarantee that the events will be queued and some " +
                "events may be dropped. It is recommended to use a single top-level block and " +
                "perform the pointer events processing within such block.",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                MultipleAwaitPointerEventScopesDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
