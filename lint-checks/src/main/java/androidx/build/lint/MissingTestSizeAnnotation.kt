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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import java.util.Collections
import java.util.EnumSet

class MissingTestSizeAnnotation : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>>? =
        Collections.singletonList(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return TestSizeAnnotationHandler(context)
    }

    class TestSizeAnnotationHandler(private val context: JavaContext) : UElementHandler() {
        override fun visitClass(node: UClass) {
            // Ignore any non-test class (missing a @RunWith() annotation)
            val runWith = node.findAnnotation(RUN_WITH) ?: return

            // Ignore host side tests as test size is ignored in this case
            val runner = runWith.attributeValues[0].expression.asRenderString()
            if (HOST_SIDE_TEST_RUNNERS.any { runner == it }) {
                return
            }

            node.methods.filter {
                it.hasAnnotation(TEST_ANNOTATION)
            }.forEach { method ->
                val combinedAnnotations = node.annotations + method.annotations
                // Report an issue if neither the test method nor the surrounding class have a
                // valid test size annotation
                if (combinedAnnotations.none { it.qualifiedName in TEST_SIZE_ANNOTATIONS }) {
                    context.report(
                        ISSUE,
                        method,
                        context.getNameLocation(method),
                        "Missing test size annotation"
                    )
                }
            }
        }
    }

    companion object {
        const val RUN_WITH = "org.junit.runner.RunWith"
        val HOST_SIDE_TEST_RUNNERS = listOf(
            "org.junit.runners.JUnit4",
            "org.robolectric.RobolectricTestRunner",
            "JUnit4",
            "RobolectricTestRunner"
        )
        const val TEST_ANNOTATION = "org.junit.Test"
        val TEST_SIZE_ANNOTATIONS = listOf(
            "androidx.test.filters.SmallTest",
            "androidx.test.filters.MediumTest",
            "androidx.test.filters.LargeTest",
            "SmallTest",
            "MediumTest",
            "LargeTest"
        )

        val ISSUE = Issue.create(
            "MissingTestSizeAnnotation",
            "Missing test size annotation",
            "All tests require a valid test size annotation, on the class or per method." +
                    "\nYou must use at least one of: @SmallTest, @MediumTest or @LargeTest." +
                    "\nUse @SmallTest for tests that run in under 200ms, @MediumTest for tests " +
                    "that run in under 1000ms, and @LargeTest for tests that run for more " +
                    "than a second.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                MissingTestSizeAnnotation::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
