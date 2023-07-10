/*
 * Copyright (C) 2020 The Android Open Source Project
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
import com.intellij.lang.jvm.JvmModifier
import org.jetbrains.uast.UMethod

class BanSynchronizedMethods : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            if (node.hasModifier(JvmModifier.SYNCHRONIZED)) {
                val incident = Incident(context)
                    .fix(null)
                    .issue(ISSUE)
                    .location(context.getLocation(node))
                    .message("Use of synchronized methods is not recommended")
                    .scope(node)
                context.report(incident)
            }
        }
    }

    companion object {
        @SuppressWarnings("LintImplUnexpectedDomain")
        val ISSUE = Issue.create(
            "BanSynchronizedMethods",
            "Method is synchronized",
            "Use of synchronized methods is not recommended," +
                " please refer to https://android.googlesource.com/platform/frameworks/" +
                "support/+/androidx-main/docs/api_guidelines.md",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(BanSynchronizedMethods::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
