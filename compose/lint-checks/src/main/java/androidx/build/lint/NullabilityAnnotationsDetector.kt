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
import com.android.tools.lint.detector.api.isJava
import org.jetbrains.uast.UAnnotation

/**
 * Checks for usages of JetBrains nullability annotations in Java code.
 */
class NullabilityAnnotationsDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes() = listOf(UAnnotation::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return AnnotationChecker(context)
    }

    private inner class AnnotationChecker(val context: JavaContext) : UElementHandler() {
        override fun visitAnnotation(node: UAnnotation) {
            if (isJava(node.sourcePsi)) {
                checkForAnnotation(node, "NotNull", "NonNull")
                checkForAnnotation(node, "Nullable", "Nullable")
            }
        }

        /**
         * Check if the node is org.jetbrains.annotations.$jetBrainsAnnotation, replace with
         * androidx.annotation.$androidxAnnotation if so.
         */
        private fun checkForAnnotation(
            node: UAnnotation,
            jetBrainsAnnotation: String,
            androidxAnnotation: String
        ) {
            val incorrectAnnotation = "org.jetbrains.annotations.$jetBrainsAnnotation"
            val replacementAnnotation = "androidx.annotation.$androidxAnnotation"
            val patternToReplace = "(?:org\\.jetbrains\\.annotations\\.)?$jetBrainsAnnotation"

            if (node.qualifiedName == incorrectAnnotation) {
                val lintFix = fix().name("Replace with `@$replacementAnnotation`")
                    .replace()
                    .pattern(patternToReplace)
                    .with(replacementAnnotation)
                    .shortenNames()
                    .autoFix(true, true)
                    .build()
                val incident = Incident(context)
                    .issue(ISSUE)
                    .fix(lintFix)
                    .location(context.getNameLocation(node))
                    .message("Use `@$replacementAnnotation` instead of `@$incorrectAnnotation`")
                    .scope(node)
                context.report(incident)
            }
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "NullabilityAnnotationsDetector",
            "Replace usages of JetBrains nullability annotations with androidx " +
                "versions in Java code",
            "The androidx nullability annotations should be used in androidx libraries " +
                "instead of JetBrains annotations.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(NullabilityAnnotationsDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
