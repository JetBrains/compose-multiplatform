/*
 * Copyright 2018 The Android Open Source Project
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

import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ObsoleteBuildCompatUsageDetectorTest : AbstractLintDetectorTest(
    useDetector = ObsoleteBuildCompatUsageDetector(),
    useIssues = listOf(ObsoleteBuildCompatUsageDetector.ISSUE),
    stubs = arrayOf(BuildCompat),
) {

    @Test
    @Ignore("ANDROID_HOME not available on CI")
    fun isAtLeastN() {
        val input = java(
            """
            package foo;
            import androidx.core.os.BuildCompat;
            public class Example {
              public static void main(String... args) {
                if (BuildCompat.isAtLeastN()) {
                  System.out.println("Hey");
                }
              }
            }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/foo/Example.java:5: Error: Using deprecated BuildCompat methods [ObsoleteBuildCompat]
                if (BuildCompat.isAtLeastN()) {
                    ~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """

        val expectedDiff = """
            Fix for src/foo/Example.java line 5: Use SDK_INT >= 24:
            @@ -5 +5
            -     if (BuildCompat.isAtLeastN()) {
            +     if (Build.VERSION.SDK_INT >= 24) {
        """
        /* ktlint-enable max-line-length */

        check(input)
            .expect(expected.trimIndent())
            .expectFixDiffs(expectedDiff.trimIndent())
    }

    @Test
    @Ignore("ANDROID_HOME not available on CI")
    fun isAtLeastNStaticImport() {
        val input = java(
            """
            package foo;
            import static androidx.core.os.BuildCompat.isAtLeastN;
            public class Example {
              public static void main(String... args) {
                if (isAtLeastN()) {
                  System.out.println("Hey");
                }
              }
            }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/foo/Example.java:5: Error: Using deprecated BuildCompat methods [ObsoleteBuildCompat]
                if (isAtLeastN()) {
                    ~~~~~~~~~~~~
            1 errors, 0 warnings
        """

        val expectedDiff = """
            Fix for src/foo/Example.java line 5: Use SDK_INT >= 24:
            @@ -5 +5
            -     if (isAtLeastN()) {
            +     if (Build.VERSION.SDK_INT >= 24) {
        """
        /* ktlint-enable max-line-length */

        check(input)
            .expect(expected.trimIndent())
            .expectFixDiffs(expectedDiff.trimIndent())
    }

    @Test
    @Ignore("ANDROID_HOME not available on CI")
    fun isAtLeastNMR1() {
        val input = java(
            """
            package foo;
            import androidx.core.os.BuildCompat;
            public class Example {
              public static void main(String... args) {
                if (BuildCompat.isAtLeastNMR1()) {
                  System.out.println("Hey");
                }
              }
            }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/foo/Example.java:5: Error: Using deprecated BuildCompat methods [ObsoleteBuildCompat]
                if (BuildCompat.isAtLeastNMR1()) {
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """

        val expectedDiff = """
            Fix for src/foo/Example.java line 5: Use SDK_INT >= 25:
            @@ -5 +5
            -     if (BuildCompat.isAtLeastNMR1()) {
            +     if (Build.VERSION.SDK_INT >= 25) {
        """
        /* ktlint-enable max-line-length */

        check(input)
            .expect(expected.trimIndent())
            .expectFixDiffs(expectedDiff.trimIndent())
    }

    @Test
    @Ignore("ANDROID_HOME not available on CI")
    fun isAtLeastO() {
        val input = java(
            """
            package foo;
            import androidx.core.os.BuildCompat;
            public class Example {
              public static void main(String... args) {
                if (BuildCompat.isAtLeastO()) {
                  System.out.println("Hey");
                }
              }
            }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/foo/Example.java:5: Error: Using deprecated BuildCompat methods [ObsoleteBuildCompat]
                if (BuildCompat.isAtLeastO()) {
                    ~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """

        val expectedDiff = """
            Fix for src/foo/Example.java line 5: Use SDK_INT >= 26:
            @@ -5 +5
            -     if (BuildCompat.isAtLeastO()) {
            +     if (Build.VERSION.SDK_INT >= 26) {
        """
        /* ktlint-enable max-line-length */

        check(input)
            .expect(expected.trimIndent())
            .expectFixDiffs(expectedDiff.trimIndent())
    }

    @Test
    @Ignore("ANDROID_HOME not available on CI")
    fun isAtLeastOMR1() {
        val input = java(
            """
            package foo;
            import androidx.core.os.BuildCompat;
            public class Example {
              public static void main(String... args) {
                if (BuildCompat.isAtLeastOMR1()) {
                  System.out.println("Hey");
                }
              }
            }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/foo/Example.java:5: Error: Using deprecated BuildCompat methods [ObsoleteBuildCompat]
                if (BuildCompat.isAtLeastOMR1()) {
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """

        val expectedDiff = """
            Fix for src/foo/Example.java line 5: Use SDK_INT >= 27:
            @@ -5 +5
            -     if (BuildCompat.isAtLeastOMR1()) {
            +     if (Build.VERSION.SDK_INT >= 27) {
        """
        /* ktlint-enable max-line-length */

        check(input)
            .expect(expected.trimIndent())
            .expectFixDiffs(expectedDiff.trimIndent())
    }

    @Test
    @Ignore("ANDROID_HOME not available on CI")
    fun isAtLeastP() {
        val input = java(
            """
            package foo;
            import androidx.core.os.BuildCompat;
            public class Example {
              public static void main(String... args) {
                if (BuildCompat.isAtLeastP()) {
                  System.out.println("Hey");
                }
              }
            }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/foo/Example.java:5: Error: Using deprecated BuildCompat methods [ObsoleteBuildCompat]
                if (BuildCompat.isAtLeastP()) {
                    ~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """

        val expectedDiff = """
            Fix for src/foo/Example.java line 5: Use SDK_INT >= 28:
            @@ -5 +5
            -     if (BuildCompat.isAtLeastP()) {
            +     if (Build.VERSION.SDK_INT >= 28) {
        """
        /* ktlint-enable max-line-length */

        check(input)
            .expect(expected.trimIndent())
            .expectFixDiffs(expectedDiff.trimIndent())
    }

    @Test
    @Ignore("ANDROID_HOME not available on CI")
    fun isAtLeastQ() {
        val input = java(
            """
            package foo;
            import androidx.core.os.BuildCompat;
            public class Example {
              public static void main(String... args) {
                if (BuildCompat.isAtLeastQ()) {
                  System.out.println("Hey");
                }
              }
            }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/foo/Example.java:5: Error: Using deprecated BuildCompat methods [ObsoleteBuildCompat]
                if (BuildCompat.isAtLeastQ()) {
                    ~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """

        val expectedDiff = """
            Fix for src/foo/Example.java line 5: Use SDK_INT >= 29:
            @@ -5 +5
            -     if (BuildCompat.isAtLeastQ()) {
            +     if (Build.VERSION.SDK_INT >= 29) {
        """
        /* ktlint-enable max-line-length */

        check(input)
            .expect(expected.trimIndent())
            .expectFixDiffs(expectedDiff.trimIndent())
    }

    companion object {
        private val BuildCompat = java(
            """
            package androidx.core.os;
            public class BuildCompat {
              public static boolean isAtLeastN() { return false; }
              public static boolean isAtLeastNMR1() { return false; }
              public static boolean isAtLeastO() { return false; }
              public static boolean isAtLeastOMR1() { return false; }
              public static boolean isAtLeastP() { return false; }
              public static boolean isAtLeastQ() { return false; }
            }
            """.trimIndent()
        )
    }
}
