/*
 * Copyright 2019 The Android Open Source Project
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

import com.android.sdklib.SdkVersionInfo.HIGHEST_KNOWN_API
import com.android.tools.lint.detector.api.ApiConstraint
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.VersionChecks.Companion.isPrecededByVersionCheckExit
import com.android.tools.lint.detector.api.VersionChecks.Companion.isWithinVersionCheckConditional
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getContainingUMethod

class BanUncheckedReflection : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf(
        METHOD_INVOKE_NAME
    )

    override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod
    ) {
        // We don't care if the invocation is correct -- there's another lint for that. We're
        // just enforcing the "all reflection on the platform SDK must be gated on SDK_INT checks"
        // policy. Also -- since we're not actually checking whether the invocation is on the
        // platform SDK -- we're discouraging reflection in general.

        // Skip if this isn't a call to `Method.invoke`.
        if (!context.evaluator.isMemberInClass(method, METHOD_REFLECTION_CLASS)) return

        // Flag if the call isn't inside or preceded by an SDK_INT check.
        if (!isWithinVersionCheckConditional(
                context,
                node,
                ApiConstraint.get(HIGHEST_KNOWN_API), false
            ) &&
            !isWithinVersionCheckConditional(context, node, ApiConstraint.get(1), true) &&
            !isPrecededByVersionCheckExit(context, node, ApiConstraint.get(HIGHEST_KNOWN_API)) &&
            !isPrecededByVersionCheckExit(context, node, ApiConstraint.get(1)) &&
            !isWithinDeprecatedSinceApiMethod(node) &&
            !isWithinDeprecatedSinceApiClass(node)
        ) {
            val incident = Incident(context)
                .issue(ISSUE)
                .location(context.getLocation(node))
                .message("Calling `Method.invoke` without an SDK check")
                .scope(node)
            context.report(incident)
        }
    }

    /**
     * Checks if the expression is within a method annotated with @DeprecatedSinceApi.
     */
    private fun isWithinDeprecatedSinceApiMethod(node: UExpression): Boolean {
        val containingMethod = node.getContainingUMethod() ?: return false
        return annotationsContainDeprecatedSinceApi(containingMethod.annotations)
    }

    /**
     * Checks if the expression is within a class annotated with @DeprecatedSinceApi.
     */
    private fun isWithinDeprecatedSinceApiClass(node: UExpression): Boolean {
        val containingClass = node.getContainingUClass() ?: return false
        return annotationsContainDeprecatedSinceApi(containingClass.annotations)
    }

    /**
     * Checks if any of the annotations are @DeprecatedSinceApi.
     */
    private fun annotationsContainDeprecatedSinceApi(annotations: Array<PsiAnnotation>): Boolean {
        for (annotation in annotations) {
            if (annotation.hasQualifiedName(DEPRECATED_SINCE_API_ANNOTATION)) {
                return true
            }
        }
        return false
    }

    companion object {
        val ISSUE = Issue.create(
            "BanUncheckedReflection",
            "Reflection that is not within an SDK check",
            "Jetpack policy discourages reflection. In cases where reflection is used on " +
                "platform SDK classes, it must be used within an `SDK_INT` check that delegates " +
                "to an equivalent public API on the latest version of the platform. If no " +
                "equivalent public API exists, reflection must not be used. For more " +
                "information, see go/androidx-api-guidelines#sdk-reflection.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(BanUncheckedReflection::class.java, Scope.JAVA_FILE_SCOPE)
        )

        const val METHOD_REFLECTION_CLASS = "java.lang.reflect.Method"
        const val METHOD_INVOKE_NAME = "invoke"
        const val DEPRECATED_SINCE_API_ANNOTATION = "androidx.annotation.DeprecatedSinceApi"
    }
}
