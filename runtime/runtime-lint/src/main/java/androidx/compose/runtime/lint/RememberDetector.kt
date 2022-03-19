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

import androidx.compose.lint.Names
import androidx.compose.lint.isInPackageName
import androidx.compose.lint.isVoidOrUnit
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
import java.util.EnumSet

/**
 * [Detector] that checks `remember` calls to make sure they are not returning [Unit].
 */
class RememberDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(Names.Runtime.Remember.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.isInPackageName(Names.Runtime.PackageName)) {
            if (node.getExpressionType().isVoidOrUnit) {
                context.report(
                    RememberReturnType,
                    node,
                    context.getNameLocation(node),
                    "`remember` calls must not return `Unit`"
                )
            }
        }
    }

    companion object {
        val RememberReturnType = Issue.create(
            "RememberReturnType",
            "`remember` calls must not return `Unit`",
            "A call to `remember` that returns `Unit` is always an error. This typically happens " +
                "when using `remember` to mutate variables on an object. `remember` is executed " +
                "during the composition, which means that if the composition fails or is " +
                "happening on a separate thread, the mutated variables may not reflect the true " +
                "state of the composition. Instead, use `SideEffect` to make deferred changes " +
                "once the composition succeeds, or mutate `MutableState` backed variables " +
                "directly, as these will handle composition failure for you.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                RememberDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
