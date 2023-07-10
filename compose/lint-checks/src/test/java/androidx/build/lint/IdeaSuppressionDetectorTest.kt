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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IdeaSuppressionDetectorTest : AbstractLintDetectorTest(
    useDetector = IdeaSuppressionDetector(),
    useIssues = listOf(IdeaSuppressionDetector.ISSUE),
) {

    @Test
    fun `Detection of IDEA-specific suppression in Java sources`() {
        val input = java(
            "src/androidx/IdeaSuppressionJava.java",
            """
                public class IdeaSuppressionJava {

                    // Call to a deprecated method with an inline suppression.
                    public void callDeprecatedMethod() {
                        //noinspection deprecation
                        deprecatedMethod();

                        notDeprecatedMethod();
                    }

                    @Deprecated
                    public void deprecatedMethod() {}

                    public void notDeprecatedMethod() {}
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/IdeaSuppressionJava.java:5: Error: Uses IntelliJ-specific suppression, should use @SuppressWarnings("deprecation") [IdeaSuppression]
        //noinspection deprecation
        ~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(input).expect(expected)
    }

    @Test
    fun `Detection of IDEA-specific suppression in Kotlin sources`() {
        val input = kotlin(
            "src/androidx/IdeaSuppressionKotlin.kt",
            """
                class IdeaSuppressionKotlin {

                    // Call to a deprecated method with an inline suppression.
                    fun callDeprecatedMethod() {
                        //noinspection deprecation
                        deprecatedMethod()

                        notDeprecatedMethod()
                    }

                    @Deprecated("Replaced with {@link #notDeprecatedMethod()}")
                    fun deprecatedMethod() {}

                    fun notDeprecatedMethod() {}
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/IdeaSuppressionKotlin.kt:5: Error: Uses IntelliJ-specific suppression, should use @SuppressWarnings("deprecation") [IdeaSuppression]
        //noinspection deprecation
        ~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(input).expect(expected)
    }
}
