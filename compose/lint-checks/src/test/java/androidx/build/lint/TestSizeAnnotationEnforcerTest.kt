/*
 * Copyright 2020 The Android Open Source Project
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
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TestSizeAnnotationEnforcerTest : LintDetectorTest() {
    override fun getDetector(): Detector = TestSizeAnnotationEnforcer()

    override fun getIssues(): List<Issue> = listOf(
        TestSizeAnnotationEnforcer.MISSING_TEST_SIZE_ANNOTATION,
        TestSizeAnnotationEnforcer.UNEXPECTED_TEST_SIZE_ANNOTATION,
        TestSizeAnnotationEnforcer.UNSUPPORTED_TEST_RUNNER
    )

    @Test
    fun allowJUnit4ForHostSideTests() {
        lint().files(
            kotlin(
                """
                package androidx.foo

                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4

                @RunWith(JUnit4::class)
                class Test {
                    @Test
                    fun aTest() {}
                }
            """
            ).within("src/test"),
            *StubClasses
        )
            .run()
            .expectClean()
    }

    @Test
    fun noTestSizeAnnotationsForHostSideTests() {
        lint().files(
            kotlin(
                """
                package androidx.foo

                import androidx.test.filters.MediumTest
                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4

                @MediumTest
                @RunWith(JUnit4::class)
                class Test {
                    @MediumTest
                    fun notATest() {}

                    @MediumTest
                    @Test
                    fun aTest() {}

                    @Test
                    fun anotherTest() {}
                }
            """
            ).within("src/test"),
            *StubClasses
        )
            .run()
            .expect(
                /* ktlint-disable max-line-length */
"""
src/test/androidx/foo/Test.kt:8: Error: Unexpected test size annotation [UnexpectedTestSizeAnnotation]
                @MediumTest
                ~~~~~~~~~~~
1 errors, 0 warnings
"""
                /* ktlint-enable max-line-length */
            )
    }

    @Test
    fun failsForUnsupportedTestRunner() {
        lint().files(
            kotlin(
                """
                package androidx.foo

                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4

                @RunWith(JUnit4::class)
                class Test
            """
            ).within("src/androidTest"),
            *StubClasses
        )
            .run()
            .expect(
                /* ktlint-disable max-line-length */
                """
src/androidTest/androidx/foo/Test.kt:7: Error: Unsupported test runner. Supported runners are: [androidx.test.ext.junit.runners.AndroidJUnit4, org.junit.runners.Parameterized] [UnsupportedTestRunner]
                @RunWith(JUnit4::class)
                         ~~~~~~~~~~~~~
1 errors, 0 warnings
            """
                /* ktlint-enable max-line-length */
            )
    }

    @Test
    fun allowsAndroidJUnit4() {
        lint().files(
            kotlin(
                """
                package androidx.foo

                import androidx.test.ext.junit.runners.AndroidJUnit4
                import androidx.test.filters.MediumTest
                import org.junit.runner.RunWith

                @MediumTest
                @RunWith(AndroidJUnit4::class)
                class Test {
                    @Test
                    fun test() {}
                }
            """
            ).within("src/androidTest"),
            *StubClasses
        )
            .run()
            .expectClean()
    }

    @Test
    fun allowsParameterized() {
        lint().files(
            kotlin(
                """
                package androidx.foo

                import org.junit.runner.RunWith
                import org.junit.runners.Parameterized

                @RunWith(Parameterized::class)
                class Test {
                    @Test
                    fun test() {}
                }
            """
            ).within("src/androidTest"),
            *StubClasses
        )
            .run()
            .expectClean()
    }

    @Test
    fun ignoresMissingRunWith() {
        lint().files(
            kotlin(
                """
                package androidx.foo

                class Test {
                    @Test
                    fun test() {}
                }
            """
            ).within("src/androidTest")
        )
            .run()
            .expectClean()
    }

    @Test
    fun testSizeAnnotationOnClass() {
        lint().files(
            kotlin(
                """
                package androidx.foo

                import androidx.test.ext.junit.runners.AndroidJUnit4
                import androidx.test.filters.MediumTest
                import org.junit.runner.RunWith
                import org.junit.Test

                @MediumTest
                @RunWith(AndroidJUnit4::class)
                class Test {
                    @Test
                    fun foo() {}
                    @Test
                    fun bar() {}
                    fun notATest() {}
                }
            """
            ).within("src/androidTest"),
            *StubClasses
        )
            .run()
            .expectClean()
    }

    @Test
    fun failsForTestMethodMissingAnnotation() {
        lint().files(
            kotlin(
                """
                package androidx.foo

                import androidx.test.ext.junit.runners.AndroidJUnit4
                import androidx.test.filters.MediumTest
                import org.junit.runner.RunWith
                import org.junit.Test

                @RunWith(AndroidJUnit4::class)
                class Test {
                    @MediumTest
                    @Test
                    fun foo() {}

                    @Test
                    fun bar() {}

                    fun notATest() {}
                }
            """
            ).within("src/androidTest"),
            *StubClasses
        )
            .run()
            .expect(
                /* ktlint-disable max-line-length */
                """
src/androidTest/androidx/foo/Test.kt:16: Error: Missing test size annotation [MissingTestSizeAnnotation]
                    fun bar() {}
                        ~~~
1 errors, 0 warnings
                """
                /* ktlint-enable max-line-length */
            )
    }

    @Test
    fun failsIfNoAnnotations() {
        lint().files(
            kotlin(
                """
                package androidx.foo

                import androidx.test.ext.junit.runners.AndroidJUnit4
                import org.junit.runner.RunWith
                import org.junit.Test

                @RunWith(AndroidJUnit4::class)
                class Test {
                    @Test
                    fun foo() {}

                    @Test
                    fun bar() {}

                    fun notATest() {}
                }
            """
            ).within("src/androidTest"),
            *StubClasses
        )
            .run()
            .expect(
                /* ktlint-disable max-line-length */
                """
src/androidTest/androidx/foo/Test.kt:11: Error: Missing test size annotation [MissingTestSizeAnnotation]
                    fun foo() {}
                        ~~~
src/androidTest/androidx/foo/Test.kt:14: Error: Missing test size annotation [MissingTestSizeAnnotation]
                    fun bar() {}
                        ~~~
2 errors, 0 warnings
                """
                /* ktlint-enable max-line-length */
            )
    }

    @Test
    fun ignoresSizeAnnotationsForParameterizedTests() {
        lint().files(
            kotlin(
                """
                package androidx.foo

                import androidx.test.filters.MediumTest
                import org.junit.runner.RunWith
                import org.junit.runners.Parameterized
                import org.junit.Test

                @MediumTest
                @RunWith(Parameterized::class)
                class Test {
                    @MediumTest
                    fun notATest() {}

                    @MediumTest
                    @Test
                    fun aTest() {}

                    @Test
                    fun anotherTest() {}
                }
            """
            ).within("src/androidTest"),
            *StubClasses
        )
            .run()
            .expectClean()
    }

    private val StubClasses = arrayOf(
        Stubs.RunWith,
        Stubs.JUnit4Runner,
        Stubs.ParameterizedRunner,
        Stubs.AndroidJUnit4Runner,
        Stubs.TestSizeAnnotations,
        Stubs.TestAnnotation
    )
}
