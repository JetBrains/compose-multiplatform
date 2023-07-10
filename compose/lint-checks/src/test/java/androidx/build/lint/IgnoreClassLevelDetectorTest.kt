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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IgnoreClassLevelDetectorTest : AbstractLintDetectorTest(
    useDetector = IgnoreClassLevelDetector(),
    useIssues = listOf(IgnoreClassLevelDetector.ISSUE),
    ) {
    @Test
    fun `Detection of class level ignore in Kotlin sources`() {
        val input = arrayOf(
            kotlin("""
                package java.androidx

                import org.junit.Ignore
                import org.junit.Test

                @Ignore("Class")
                class TestClass {
                    @Test
                    fun oneTest() {}

                    @Test
                    @Ignore
                    fun twoTest() {}
                }
            """),
            Stubs.IgnoreAnnotation,
            Stubs.TestAnnotation
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/TestClass.kt:7: Error: @Ignore should not be used at the class level. Move the annotation to each test individually. [IgnoreClassLevelDetector]
                @Ignore("Class")
                ~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of class level ignore in Java sources`() {
        val input = arrayOf(
            java(
                """
                    package java.androidx;

                    import org.junit.Ignore;
                    import org.junit.Test;

                    @Ignore
                    public class TestClass {
                        @Test
                        public void oneTest() {}

                        @Ignore
                        @Test
                        public void twoTest() {}
                    }
                """
            ),
            Stubs.IgnoreAnnotation,
            Stubs.TestAnnotation
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/TestClass.java:7: Error: @Ignore should not be used at the class level. Move the annotation to each test individually. [IgnoreClassLevelDetector]
                    @Ignore
                    ~~~~~~~
1 errors, 0 warnings
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Test level ignore allowed in Kotlin sources`() {
        val input = arrayOf(
            kotlin(
                """
                    package java.androidx

                    import org.junit.Ignore
                    import org.junit.Test

                    class IgnoreTestLevelKotlin {
                        @Test
                        @Ignore("Test")
                        fun oneTest() {}

                        @Test
                        fun twoTest() {}
                    }
                """
            ),
            Stubs.IgnoreAnnotation,
            Stubs.TestAnnotation
        )

        check(*input).expectClean()
    }

    @Test
    fun `Test level ignore allowed in Java sources`() {
        val input = arrayOf(
            java(
                """
                package java.androidx;

                import org.junit.Ignore;
                import org.junit.Test;

                public class Test {
                    @Test
                    @Ignore
                    public void oneTest() {}

                    @Test
                    public void twoTest() {}
                }
            """),
            Stubs.IgnoreAnnotation,
            Stubs.TestAnnotation
        )

        check(*input).expectClean()
    }
}
