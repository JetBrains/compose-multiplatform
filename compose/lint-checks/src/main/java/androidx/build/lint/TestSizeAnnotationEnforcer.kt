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
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import java.util.Collections
import java.util.EnumSet

/**
 * Lint check to enforce that every device side test (tests in the androidTest dir) has correct
 * size annotations, and a test runner that supports timeouts, so that we can correctly split up
 * test runs and enforce timeouts.
 *
 * Also enforces that test runners that do not support timeouts and host side tests do not
 * include test size annotations, as these are not used and can be misleading.
 */
class TestSizeAnnotationEnforcer : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>>? =
        Collections.singletonList(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return TestSizeAnnotationHandler(context)
    }

    class TestSizeAnnotationHandler(private val context: JavaContext) : UElementHandler() {
        override fun visitClass(node: UClass) {
            // Enforce no size annotations for host side tests
            val testPath = context.file.absolutePath
            if (ANDROID_TEST_DIRS.none { testPath.contains(it) }) {
                enforceHasNoSizeAnnotations(node)
                return
            }

            // Ignore any non-test classes and classes without an explicit runner specified.
            // (missing a @RunWith() annotation)
            val runWithAnnotation = node.findAnnotation(RUN_WITH) ?: return

            val testRunner = runWithAnnotation.findAttributeValue(RUN_WITH_VALUE)

            val testRunnerClassName = (testRunner as? UClassLiteralExpression)
                ?.type?.canonicalText ?: return

            if (testRunnerClassName !in ALLOWED_TEST_RUNNERS) {
                val incident = Incident(context)
                    .issue(UNSUPPORTED_TEST_RUNNER)
                    .location(context.getNameLocation(testRunner))
                    .message("Unsupported test runner." +
                        " Supported runners are: $ALLOWED_TEST_RUNNERS")
                    .scope(testRunner)
                context.report(incident)
                return
            }

            if (testRunnerClassName in TIMEOUT_ENFORCED_TEST_RUNNERS) {
                enforceHasSizeAnnotations(node)
            }
        }

        /**
         * Enforces that either [node] has a valid size annotation and/or every method has a valid
         * size annotation.
         */
        private fun enforceHasSizeAnnotations(node: UClass) {
            node.methods.filter {
                it.hasAnnotation(TEST_ANNOTATION)
            }.forEach { method ->
                val combinedAnnotations = node.uAnnotations + method.uAnnotations
                // Report an issue if neither the test method nor the surrounding class have a
                // valid test size annotation
                if (combinedAnnotations.none { it.qualifiedName in TEST_SIZE_ANNOTATIONS }) {
                    val incident = Incident(context)
                        .issue(MISSING_TEST_SIZE_ANNOTATION)
                        .location(context.getNameLocation(method))
                        .message("Missing test size annotation")
                        .scope(method)
                    context.report(incident)
                }
            }
        }

        /**
         * Enforces that [node] has no size annotations either on the class, or any test methods.
         */
        private fun enforceHasNoSizeAnnotations(node: UClass) {
            // Report an issue if the class has a size annotation
            node.uAnnotations
                .find { it.qualifiedName in TEST_SIZE_ANNOTATIONS }
                ?.let { annotation ->
                    val incident = Incident(context)
                        .issue(UNEXPECTED_TEST_SIZE_ANNOTATION)
                        .location(context.getNameLocation(annotation))
                        .message("Unexpected test size annotation")
                        .scope(annotation)
                    context.report(incident)
                }

            node.methods.filter {
                it.hasAnnotation(TEST_ANNOTATION)
            }.forEach { method ->
                // Report an issue if the method has a size annotation
                method.uAnnotations
                    .find { it.qualifiedName in TEST_SIZE_ANNOTATIONS }
                    ?.let { annotation ->
                        val incident = Incident(context)
                            .issue(UNEXPECTED_TEST_SIZE_ANNOTATION)
                            .location(context.getNameLocation(annotation))
                            .message("Unexpected test size annotation")
                            .scope(annotation)
                        context.report(incident)
                    }
            }
        }
    }

    companion object {
        /**
         * List of test runners that support timeouts. This is a subset of [ALLOWED_TEST_RUNNERS].
         */
        private val TIMEOUT_ENFORCED_TEST_RUNNERS = listOf(
            "androidx.test.ext.junit.runners.AndroidJUnit4"
        )

        /**
         * Only AndroidJUnit4 enforces timeouts, so it should be used over JUnit4 / other such
         * runners. Parameterized does not enforce timeouts, but there is no equivalent that
         * does, so it is still fine to use.
         */
        private val ALLOWED_TEST_RUNNERS = listOf(
            "androidx.test.ext.junit.runners.AndroidJUnit4",
            "org.junit.runners.Parameterized"
        )
        private const val RUN_WITH = "org.junit.runner.RunWith"
        private const val RUN_WITH_VALUE = "value"
        /**
         * TODO: b/170214947
         * Directories that contain device test source. Unfortunately we don't currently have a
         * better way of figuring out what test we are analyzing, as [Scope.TEST_SOURCES]
         * includes both host and device side tests.
         */
        private val ANDROID_TEST_DIRS = listOf(
            "androidTest",
            "androidAndroidTest",
            "androidDeviceTest",
            "androidDeviceTestDebug",
            "androidDeviceTestRelease"
        )
        private const val TEST_ANNOTATION = "org.junit.Test"
        private val TEST_SIZE_ANNOTATIONS = listOf(
            "androidx.test.filters.SmallTest",
            "androidx.test.filters.MediumTest",
            "androidx.test.filters.LargeTest"
        )

        val UNSUPPORTED_TEST_RUNNER = Issue.create(
            "UnsupportedTestRunner",
            "Unsupported test runner",
            "Only AndroidJUnit4 supports setting a timeout for tests using the test size " +
                "annotation, so this test runner should be used instead. There is no " +
                "equivalent parameterized runner that also sets timeouts, so Parameterized is " +
                "also allowed.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                TestSizeAnnotationEnforcer::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )

        val MISSING_TEST_SIZE_ANNOTATION = Issue.create(
            "MissingTestSizeAnnotation",
            "Missing test size annotation",
            "All tests require a valid test size annotation, on the class or per method." +
                "\nYou must use at least one of: @SmallTest, @MediumTest or @LargeTest." +
                "\nUse @SmallTest for tests that run in under 200ms, @MediumTest for tests " +
                "that run in under 1000ms, and @LargeTest for tests that run for more " +
                "than a second.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                TestSizeAnnotationEnforcer::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )

        val UNEXPECTED_TEST_SIZE_ANNOTATION = Issue.create(
            "UnexpectedTestSizeAnnotation",
            "Unexpected test size annotation",
            "Host side tests and device tests with runners that do not support timeouts " +
                "should not have any test size annotations. Host side tests all run together," +
                " and device tests with runners that do not support timeouts will be placed in" +
                " the 'large' test bucket, since we cannot enforce that they will be fast enough" +
                " to run in the 'small' / 'medium' buckets.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                TestSizeAnnotationEnforcer::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
