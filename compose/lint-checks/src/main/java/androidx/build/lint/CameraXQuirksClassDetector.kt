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
import org.jetbrains.uast.UClass

/**
 * Detector to apply lint rules for CameraX quirks. The rule is to enforce a javadoc template to
 * describe the bug id, issue description and device info. This detector is disabled by default.
 * Only CameraX modules will enable the detector.
 */
class CameraXQuirksClassDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        override fun visitClass(node: UClass) {
            val isQuirk = node.implementsList?.referenceElements?.find {
                it.referenceName!!.endsWith("Quirk")
            } != null

            if (isQuirk) {
                val comments = node.comments
                val sb = StringBuilder()
                comments.forEach { sb.append(it.text) }
                val comment = sb.append("\n").toString()

                if (!comment.contains("<p>QuirkSummary") ||
                    !comment.contains("Bug Id:") ||
                    !comment.contains("Description:") ||
                    !comment.contains("Device(s):")) {
                    val implForInsertion = """
                         * <p>QuirkSummary
                         *     Bug Id:
                         *     Description:
                         *     Device(s):
                        """.trimIndent()

                    val incident = Incident(context)
                        .issue(ISSUE)
                        .message("CameraX quirks should include this template in the javadoc:" +
                            "\n\n$implForInsertion\n\n")
                        .location(context.getNameLocation(node))
                        .scope(node)
                    context.report(incident)
                }
            }
        }
    }

    companion object {
        val ISSUE = Issue.create(
            id = "CameraXQuirksClassDetector",
            briefDescription = "CameraQuirks include @QuirkSummary in the javadoc",
            explanation = "CameraX quirks should include @QuirkSummary in the javadoc.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            enabledByDefault = false,
            implementation = Implementation(
                CameraXQuirksClassDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}