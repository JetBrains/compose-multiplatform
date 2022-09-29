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
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.skipParenthesizedExprUp

class ReturnFromAwaitPointerEventScopeDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> =
        listOf(Names.Ui.Pointer.AwaitPointerEventScope.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Ui.Pointer.PackageName)) return
        val methodParent = skipParenthesizedExprUp(node.uastParent)
        val isAssignedToVariable = methodParent is ULocalVariable
        val isReturnExpression = methodParent is UReturnExpression

        if (isAssignedToVariable || isReturnExpression) {
            context.report(
                ExitAwaitPointerEventScope,
                node,
                context.getNameLocation(node),
                ErrorMessage
            )
        }
    }

    companion object {
        const val IssueId: String = "ReturnFromAwaitPointerEventScope"
        const val ErrorMessage = "Returning from awaitPointerEventScope may cause some input " +
            "events to be dropped"
        val ExitAwaitPointerEventScope = Issue.create(
            IssueId,
            ErrorMessage,
            "Pointer Input events are queued inside awaitPointerEventScope. " +
                "By using the return value of awaitPointerEventScope one might unexpectedly lose " +
                "events. If another awaitPointerEventScope is restarted " +
                "there is no guarantee that the events will persist between those calls. In this " +
                "case you should keep all events inside the awaitPointerEventScope block",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                ReturnFromAwaitPointerEventScopeDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
