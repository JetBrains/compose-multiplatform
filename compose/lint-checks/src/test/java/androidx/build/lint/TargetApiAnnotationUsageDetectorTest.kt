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

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class TargetApiAnnotationUsageDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = TargetApiAnnotationUsageDetector()

    override fun getIssues(): List<Issue> = listOf(
        TargetApiAnnotationUsageDetector.ISSUE
    )

    private fun checkTask(testFile: TestFile): TestLintTask {
        return lint().files(
            java(annotationSource),
            testFile
        )
    }

    private val annotationSource = """
package android.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({TYPE, METHOD, CONSTRUCTOR})
@Retention(RetentionPolicy.CLASS)
public @interface TargetApi {
    int value();
}
    """.trimIndent()

    @Test
    fun testAnnotationUsageJava() {
        val input = java(
            """
package androidx.sample;

import android.annotation.TargetApi;

@TargetApi(24)
public class SampleClass {
    @TargetApi(15)
    public void method() {
        // Stub
    }
}
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/SampleClass.java:5: Error: Use @RequiresApi instead of @TargetApi [BanTargetApiAnnotation]
@TargetApi(24)
~~~~~~~~~~~~~~
src/androidx/sample/SampleClass.java:7: Error: Use @RequiresApi instead of @TargetApi [BanTargetApiAnnotation]
    @TargetApi(15)
    ~~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        /* ktlint-disable max-line-length */
        val expectFixDiffs = """
Fix for src/androidx/sample/SampleClass.java line 5: Replace with `@RequiresApi`:
@@ -5 +5
- @TargetApi(24)
+ @androidx.annotation.RequiresApi(24)
Fix for src/androidx/sample/SampleClass.java line 7: Replace with `@RequiresApi`:
@@ -7 +7
-     @TargetApi(15)
+     @androidx.annotation.RequiresApi(15)
        """.trimIndent()
        /* ktlint-enable max-line-length */

        checkTask(input)
            .run()
            .expect(expected)
            .expectFixDiffs(expectFixDiffs)
    }

    @Test
    fun testAnnotationUsageKt() {
        val input = kotlin(
            """
package androidx.sample

import android.annotation.TargetApi

@TargetApi(24)
class SampleClass {
    @TargetApi(15)
    fun method() {
        // Stub
    }
}
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/SampleClass.kt:5: Error: Use @RequiresApi instead of @TargetApi [BanTargetApiAnnotation]
@TargetApi(24)
~~~~~~~~~~~~~~
src/androidx/sample/SampleClass.kt:7: Error: Use @RequiresApi instead of @TargetApi [BanTargetApiAnnotation]
    @TargetApi(15)
    ~~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        /* ktlint-disable max-line-length */
        val expectFixDiffs = """
Fix for src/androidx/sample/SampleClass.kt line 5: Replace with `@RequiresApi`:
@@ -5 +5
- @TargetApi(24)
+ @androidx.annotation.RequiresApi(24)
Fix for src/androidx/sample/SampleClass.kt line 7: Replace with `@RequiresApi`:
@@ -7 +7
-     @TargetApi(15)
+     @androidx.annotation.RequiresApi(15)
        """.trimIndent()
        /* ktlint-enable max-line-length */

        checkTask(input)
            .run()
            .expect(expected)
            .expectFixDiffs(expectFixDiffs)
    }
}
