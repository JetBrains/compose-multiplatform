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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass

/**
 * Checks for usages of @org.junit.Ignore at the class level.
 */
class IgnoreClassLevelDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UAnnotation::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return AnnotationChecker(context)
    }

    private inner class AnnotationChecker(val context: JavaContext) : UElementHandler() {
        override fun visitAnnotation(node: UAnnotation) {
            if (node.qualifiedName == "org.junit.Ignore" && node.uastParent is UClass) {
                val incident = Incident(context)
                    .issue(ISSUE)
                    .location(context.getNameLocation(node))
                    .message("@Ignore should not be used at the class level. Move the annotation " +
                        "to each test individually.")
                    .scope(node)
                context.report(incident)
            }
        }
    }

    /**
     * Creates a LintFix which removes the @Ignore annotation from the class and adds it to each
     * individual test method.
     *
     * TODO(b/235340679): This is currently unused because of issues described in the method body.
     */
    @Suppress("unused")
    private fun createFix(testClass: UClass, context: JavaContext, annotation: String): LintFix {
        val fix = fix()
            .name("Annotate each test method and remove the class-level annotation")
            .composite()

        for (method in testClass.allMethods) {
            if (method.isTestMethod()) {
                val methodFix = fix()
                    // The replace param on annotate doesn't work: if @Ignore is already present on
                    // the method, the annotation is added again instead of being replaced.
                    .annotate("org.junit.Ignore", true)
                    .range(context.getLocation(method))
                    .build()
                fix.add(methodFix)
            }
        }

        val classFix = fix().replace()
            // This requires the exact text of the class annotation to be passed to this function.
            // This can be gotten with `node.sourcePsi?.node?.text!!`, but `text`'s doc says using
            // it should be avoided, so this isn't the best solution.
            .text(annotation)
            .with("")
            .reformat(true)
            .build()
        fix.add(classFix)

        return fix.build()
    }

    /**
     * Checks if this PsiMethod has a @org.junit.Test annotation
     */
    private fun PsiMethod.isTestMethod(): Boolean {
        for (annotation in this.annotations) {
            if (annotation.qualifiedName == "org.junit.Test") {
                return true
            }
        }
        return false
    }

    companion object {
        val ISSUE = Issue.create(
            "IgnoreClassLevelDetector",
            "@Ignore should not be used at the class level.",
            "Using @Ignore at the class level instead of annotating each individual " +
                "test causes errors in Android Test Hub.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                IgnoreClassLevelDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
