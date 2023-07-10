/*
 * Copyright 2021 The Android Open Source Project
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
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UVariable

/**
 * Detects usages of IntelliJ's per-line suppression, which is only valid within IntelliJ-based
 * tools, and suggests replacement with the Java-compatible `@SuppressWarnings` annotation.
 *
 * Adapted from Android Studio's `TerminologyDetector` lint check.
 */
class IdeaSuppressionDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement?>> {
        // Everything that we'd expect to see a suppression on.
        return listOf(
            UFile::class.java,
            UVariable::class.java,
            UMethod::class.java,
            UClass::class.java,
            ULiteralExpression::class.java
        )
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        // We're using a UAST visitor here instead of just visiting the file
        // as raw text since we'd like to only visit declarations, comments and strings,
        // not for example class, method and field *references* to APIs outside of
        // our control
        return object : UElementHandler() {
            // There's some duplication in comments between UFile#allCommentsInFile
            // and the comments returned for each declaration, but unfortunately each
            // one is missing some from the other so we need to check both and just
            // keep track of the ones we've checked so we don't report errors multiple
            // times
            private val checkedComments = mutableSetOf<String>()

            override fun visitFile(node: UFile) {
                checkedComments.clear()
                for (comment in node.allCommentsInFile) {
                    if (comment.uastParent is UDeclaration) { // handled in checkDeclaration
                        continue
                    }
                    val contents = comment.text
                    checkedComments.add(contents)
                    visitComment(context, comment, contents)
                }
            }

            override fun visitVariable(node: UVariable) {
                checkDeclaration(node, node.name)
            }

            override fun visitMethod(node: UMethod) {
                checkDeclaration(node, node.name)
            }

            override fun visitClass(node: UClass) {
                checkDeclaration(node, node.name)
            }

            private fun checkDeclaration(node: UDeclaration, name: String?) {
                name ?: return
                visitComment(context, node, name)
                for (comment in node.comments) {
                    val contents = comment.text
                    if (checkedComments.add(contents)) {
                        visitComment(context, comment, contents)
                    }
                }
            }

            override fun visitLiteralExpression(node: ULiteralExpression) {
                if (node.isString) {
                    val string = node.value as? String ?: return
                    visitComment(context, node, string)
                }
            }
        }
    }

    /**
     * Checks the text in [source].
     *
     * If it finds matches in the string, it will report errors into the
     * given context. The associated AST [element] is used to look look
     * up suppress annotations and to find the right error range.
     */
    private fun visitComment(
        context: JavaContext,
        element: UElement,
        source: CharSequence,
    ) {
        if (source.startsWith("//noinspection ")) {
            val warnings = source.split(" ").drop(1).filter { JAVA_WARNINGS.contains(it) }
            if (warnings.isNotEmpty()) {
                val args = warnings.joinToString(", ") { "\"$it\"" }
                val incident = Incident(context)
                    .issue(ISSUE)
                    .location(context.getNameLocation(element))
                    .message("Uses IntelliJ-specific suppression, should use" +
                        " `@SuppressWarnings($args)`")
                    .scope(element)
                context.report(incident)
            }
        }
    }

    companion object {
        // Warnings that the Java compiler cares about and should not be suppressed inline.
        private val JAVA_WARNINGS = listOf(
            "deprecation"
        )

        val ISSUE = Issue.create(
            "IdeaSuppression",
            "Suppression using `//noinspection` is not supported by the Java compiler",
            "Per-line suppression using `//noinspection` is not supported by the Java compiler " +
                "and will not suppress build-time warnings. Instead, use the `@SuppressWarnings` " +
                "annotation on the containing method or class.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(IdeaSuppressionDetector::class.java, Scope.JAVA_FILE_SCOPE),
        )
    }
}