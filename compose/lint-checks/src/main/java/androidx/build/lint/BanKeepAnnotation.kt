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

class BanKeepAnnotation : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UAnnotation::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return AnnotationChecker(context)
    }

    private inner class AnnotationChecker(val context: JavaContext) : UElementHandler() {
        override fun visitAnnotation(node: UAnnotation) {
            if (node.qualifiedName == "androidx.annotation.Keep" ||
                node.qualifiedName == "android.support.annotation.keep"
            ) {
                val incident = Incident(context)
                    .issue(ISSUE)
                    .location(context.getNameLocation(node))
                    .message("Uses @Keep annotation")
                    .scope(node)
                context.report(incident)
            }
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "BanKeepAnnotation",
            "Uses @Keep annotation",
            "Use of @Keep annotation is not allowed, please use a conditional " +
                "keep rule in proguard-rules.pro.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(BanKeepAnnotation::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
