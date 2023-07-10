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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UMethod

@Suppress("UnstableApiUsage")
class BanInlineOptIn : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return MethodChecker(context)
    }

    private inner class MethodChecker(val context: JavaContext) : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            val hasOptInAnnotation = context.evaluator.getAnnotation(node, "kotlin.OptIn") != null

            if (context.evaluator.isInline(node) && hasOptInAnnotation) {
                val incident = Incident(context, ISSUE)
                    .location(context.getNameLocation((node)))
                    .message("Inline functions cannot opt into experimental APIs.")
                    .scope(node)
                context.report(incident)
            }
        }
    }

    companion object {
        val ISSUE = Issue.create(
            id = "BanInlineOptIn",
            briefDescription = "Uses @OptIn annotation on an inline function",
            explanation = "Use of the @OptIn annotation is not allowed on inline functions," +
                " as libraries using this method will inline the reference to the opted-in" +
                " class. This can potentially create a compatibility issue.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                BanInlineOptIn::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}