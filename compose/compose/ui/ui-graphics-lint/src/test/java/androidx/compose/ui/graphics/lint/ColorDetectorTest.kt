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

package androidx.compose.ui.graphics.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */

/**
 * Test for [ColorDetector].
 */
@RunWith(JUnit4::class)
class ColorDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ColorDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            ColorDetector.MissingColorAlphaChannel,
            ColorDetector.InvalidColorHexValue
        )

    @Test
    fun MissingColorAlphaChannel() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.graphics.*

                val color = Color(0x000000)
                // upper case
                val color2 = Color(0xEEEEEE)
                // lower case
                val color3 = Color(0xeeeeee)
                // mixed case (?!)
                val color3 = Color(0xeEeeEE)
                // separators and L suffix
                val color4 = Color(0x00_00_00L)
            """
            ),
            Stubs.Color
        )
            .run()
            .expect(
                """
src/test/test.kt:6: Warning: Missing Color alpha channel [MissingColorAlphaChannel]
                val color = Color(0x000000)
                                  ~~~~~~~~
src/test/test.kt:8: Warning: Missing Color alpha channel [MissingColorAlphaChannel]
                val color2 = Color(0xEEEEEE)
                                   ~~~~~~~~
src/test/test.kt:10: Warning: Missing Color alpha channel [MissingColorAlphaChannel]
                val color3 = Color(0xeeeeee)
                                   ~~~~~~~~
src/test/test.kt:12: Warning: Missing Color alpha channel [MissingColorAlphaChannel]
                val color3 = Color(0xeEeeEE)
                                   ~~~~~~~~
src/test/test.kt:14: Warning: Missing Color alpha channel [MissingColorAlphaChannel]
                val color4 = Color(0x00_00_00L)
                                   ~~~~~~~~~~~
0 errors, 5 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 6: Add `FF` alpha channel:
@@ -6 +6
-                 val color = Color(0x000000)
+                 val color = Color(0xFF000000)
Fix for src/test/test.kt line 8: Add `FF` alpha channel:
@@ -8 +8
-                 val color2 = Color(0xEEEEEE)
+                 val color2 = Color(0xFFEEEEEE)
Fix for src/test/test.kt line 10: Add `ff` alpha channel:
@@ -10 +10
-                 val color3 = Color(0xeeeeee)
+                 val color3 = Color(0xffeeeeee)
Fix for src/test/test.kt line 12: Add `ff` alpha channel:
@@ -12 +12
-                 val color3 = Color(0xeEeeEE)
+                 val color3 = Color(0xffeEeeEE)
Fix for src/test/test.kt line 14: Add `FF` alpha channel:
@@ -14 +14
-                 val color4 = Color(0x00_00_00L)
+                 val color4 = Color(0xFF00_00_00L)
            """
            )
    }

    @Test
    fun incorrectChannels() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.graphics.*

                val color = Color(0x00000)
                val color2 = Color(0xEEEEE)
                val color3 = Color(0x00_0_0_0L)
            """
            ),
            Stubs.Color
        )
            .run()
            .expect(
                """
src/test/test.kt:6: Warning: Invalid Color hex value [InvalidColorHexValue]
                val color = Color(0x00000)
                                  ~~~~~~~
src/test/test.kt:7: Warning: Invalid Color hex value [InvalidColorHexValue]
                val color2 = Color(0xEEEEE)
                                   ~~~~~~~
src/test/test.kt:8: Warning: Invalid Color hex value [InvalidColorHexValue]
                val color3 = Color(0x00_0_0_0L)
                                   ~~~~~~~~~~~
0 errors, 3 warnings
            """
            )
            // No fix possible for this sort of error
            .expectFixDiffs("")
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package test

                import kotlin.random.Random
                import androidx.compose.ui.graphics.*

                // Correctly formatted hex values
                val color = Color(0xFF000000)
                val color2 = Color(0xFFEEEEEE)
                val color3 = Color(0xffeeeeee)
                val color4 = Color(0xFF000000L)
                val color5 = Color(0xFF_00_00_00)
                val color6 = Color(0xFF_00_00_00L)
                // Directly pass value without hex format
                val color7 = Color(4278190080)
                // Using the inline constructor
                val color8 = Color(4278190080.toULong())
                // Explicitly passing each channel
                val color9 = Color(1f, 1f, 1f, 1f)
                val color10 = Color(0xFF, 0xFF, 0xFF, 0xFF)
                // Non-literal expressions
                val color11 = Color(0xFFEEEEEE.toInt())
                val color12 = Color(Random.nextInt())
                val color13 = Color(0xFFEEEEEE.toLong())
                val color14 = Color(Random.nextLong())
            """
            ),
            Stubs.Color
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
