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
class PrivateConstructorForUtilityClassDetectorTest : AbstractLintDetectorTest(
    useDetector = PrivateConstructorForUtilityClassDetector(),
    useIssues = listOf(PrivateConstructorForUtilityClassDetector.ISSUE),
) {

    @Test
    fun testInnerClassVisibilityJava() {
        val input = java(
            "src/androidx/PrivateConstructorForUtilityClassJava.java",
            """
                public class PrivateConstructorForUtilityClassJava {

                    // This class has a default private constructor, which is allowed.
                    private static class PrivateInnerClass {
                        static void method() { }
                    }

                    // This class needs an explicit private constructor.
                    static class DefaultInnerClass {
                        static void method() { }
                    }

                    // This class needs an explicit private constructor.
                    protected static class ProtectedInnerClass {
                        static void method() { }
                    }

                    // This class needs an explicit private constructor.
                    public static class PublicInnerClass {
                        static void method() { }
                    }
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/PrivateConstructorForUtilityClassJava.java:9: Error: Utility class is missing private constructor [PrivateConstructorForUtilityClass]
    static class DefaultInnerClass {
                 ~~~~~~~~~~~~~~~~~
src/androidx/PrivateConstructorForUtilityClassJava.java:14: Error: Utility class is missing private constructor [PrivateConstructorForUtilityClass]
    protected static class ProtectedInnerClass {
                           ~~~~~~~~~~~~~~~~~~~
src/androidx/PrivateConstructorForUtilityClassJava.java:19: Error: Utility class is missing private constructor [PrivateConstructorForUtilityClass]
    public static class PublicInnerClass {
                        ~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(input).expect(expected)
    }
}
