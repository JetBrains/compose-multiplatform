/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.build.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class ObsoleteBuildCompatUsageDetector : Detector(), Detector.UastScanner {
    private val methodsToApiLevels = mapOf(
        "isAtLeastN" to 24,
        "isAtLeastNMR1" to 25,
        "isAtLeastO" to 26,
        "isAtLeastOMR1" to 27,
        "isAtLeastP" to 28,
        "isAtLeastQ" to 29
    )

    override fun getApplicableMethodNames() = methodsToApiLevels.keys.toList()

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!context.evaluator.isMemberInClass(method, "androidx.core.os.BuildCompat")) {
            return
        }

        // A receiver indicates the class name is part of the call (as opposed to static import).
        val target = if (node.receiver != null) node.uastParent!! else node

        val apiLevel = methodsToApiLevels[node.methodName]
        val lintFix = fix().name("Use SDK_INT >= $apiLevel")
            .replace()
            .text(target.asRenderString())
            .with("Build.VERSION.SDK_INT >= $apiLevel")
            .build()
        val incident = Incident(context)
            .fix(lintFix)
            .issue(ISSUE)
            .location(context.getLocation(node))
            .message("Using deprecated BuildCompat methods")
            .scope(node)
        context.report(incident)
    }

    companion object {
        val ISSUE = Issue.create(
            "ObsoleteBuildCompat",
            "Using deprecated BuildCompat methods",
            "BuildConfig methods should only be used prior to an API level's finalization. " +
                "Once an API level number is assigned, comparing directly with SDK_INT " +
                "is preferred as it enables other lint checks to correctly work.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(ObsoleteBuildCompatUsageDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
