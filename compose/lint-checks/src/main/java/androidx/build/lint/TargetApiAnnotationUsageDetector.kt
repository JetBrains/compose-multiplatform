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

import com.android.tools.lint.client.api.UElementHandler

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.namePsiElement

/**
 * Enforces policy banning use of the `@TargetApi` annotation.
 */
class TargetApiAnnotationUsageDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UAnnotation::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return AnnotationChecker(context)
    }

    private inner class AnnotationChecker(val context: JavaContext) : UElementHandler() {
        override fun visitAnnotation(node: UAnnotation) {
            if (node.qualifiedName == "android.annotation.TargetApi") {

                // To support Kotlin's type aliases, we need to check the pattern against the symbol
                // instead of a constant ("TargetApi") to pass Lint's IMPORT_ALIAS test mode. In the
                // case where namePsiElement returns null (which shouldn't happen), fall back to the
                // RegEx check.
                val searchPattern = node.namePsiElement?.text
                    ?: "(?:android\\.annotation\\.)?TargetApi"

                val lintFix = fix().name("Replace with `@RequiresApi`")
                    .replace()
                    .pattern(searchPattern)
                    .with("androidx.annotation.RequiresApi")
                    .shortenNames()
                    .autoFix(true, true)
                    .build()
                val incident = Incident(context)
                    .fix(lintFix)
                    .issue(ISSUE)
                    .location(context.getNameLocation(node))
                    .message("Use `@RequiresApi` instead of `@TargetApi`")
                    .scope(node)
                context.report(incident)
            }
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "BanTargetApiAnnotation",
            "Replace usage of `@TargetApi` with `@RequiresApi`",
            "The `@TargetApi` annotation satisfies the `NewApi` lint check, but it does " +
                "not ensure that calls to the annotated API are correctly guarded on an `SDK_INT`" +
                " (or equivalent) check. Instead, use the `@RequiresApi` annotation to ensure " +
                "that all calls are correctly guarded.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(TargetApiAnnotationUsageDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
