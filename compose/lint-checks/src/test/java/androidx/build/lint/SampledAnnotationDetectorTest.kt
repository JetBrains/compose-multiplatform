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

@file:Suppress("KDocUnresolvedReference", "UnstableApiUsage")

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */

/**
 * Test for [SampledAnnotationDetector]
 *
 * This tests the following module setup:
 *
 * Module 'foo', which lives in foo
 * Module 'foo:samples', which lives in foo/samples, and depends on 'foo'
 */
@RunWith(JUnit4::class)
class SampledAnnotationDetectorTest : LintDetectorTest() {

    override fun getDetector() = SampledAnnotationDetector()

    override fun getIssues() = mutableListOf(
        SampledAnnotationDetector.OBSOLETE_SAMPLED_ANNOTATION,
        SampledAnnotationDetector.UNRESOLVED_SAMPLE_LINK,
        SampledAnnotationDetector.MULTIPLE_FUNCTIONS_FOUND,
        SampledAnnotationDetector.INVALID_SAMPLES_LOCATION
    )

    private val fooModuleName = "foo"
    private val sampleModuleName = "samples"

    private val barFilePath = "src/foo/Bar.kt"
    private val sampleFilePath = "samples/src/foo/samples/test.kt"

    private val sampledStub = compiled(
        "libs/sampled.jar",
        kotlin(
            """
            package androidx.annotation

            @Target(AnnotationTarget.FUNCTION)
            @Retention(AnnotationRetention.SOURCE)
            annotation class Sampled
        """
        ).to("androidx/annotation/Sampled.kt"),
        0x9dd4162c,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3AJcrFnZafr1ecmFuQk1osxBaSWlzi
        XaLEoMUAAClM+YMvAAAA
        """,
        """
        androidx/annotation/Sampled.class:
        H4sIAAAAAAAAAIVSyU4CQRB9NYggbriDuGv0Jki8cXLBSKJiEL1wapmOGWlm
        CNOg3rj5Tx4M8ehHGatdgMNE+1D9qvq96qrqfv94eQWwj01CSrh203Psx7Rw
        XU8L7Xhu+krUG0raERAhfi/aIq2Ee5cu3t7Lqo4gRFjtRwd0Bz0YQZiweFbz
        tHLcQUpZNO+kzhEmhFLeg7S/Az5hK4DdT9jTRU+uL47KheIFYTlAUZJaugYx
        NdwWqiUJO39mHlQMXxWvS0d5wtpZYIOD3O1/KJeecqpPOVNmILHX0UbweV7J
        OmcqPzWkqew8Xz4tHhOmfps5l1rYQgs+tOrtED8oGcNzpxqHHh3jZRjZe4Rk
        txONWQkrZsVT0bdnK9HtZK0MHXY7hpA1Rf7xEfgKEGI/3m5NG8drNavyxFE8
        4GSpxS3X5Y3jO7dK9mfrb3N6DLF4GGZZWP+ya9jgvYow+I8hWgFJjCCGUfbG
        JMYxgclvGGc4ZeAXZxozmGXVXAWhAuYLWGCLhDHJAhaRYpaPJSxXYPlY8bH6
        CeBn7eLsAgAA
        """
    )

    private val emptySampleFile = kotlin(
        """
            package foo.samples
        """
    )

    private val unannotatedSampleFile = kotlin(
        """
            package foo.samples

            fun sampleBar() {}
        """
    )

    private val multipleMatchingSampleFile = kotlin(
        """
            package foo.samples

            import androidx.annotation.Sampled

            @Sampled
            fun sampleBar() {}

            @Sampled
            fun sampleBar(param: Boolean = false) {}
        """
    )

    private val correctlyAnnotatedSampleFile = kotlin(
        """
            package foo.samples

            import androidx.annotation.Sampled

            @Sampled
            fun sampleBar() {}
        """
    )

    private fun checkKotlin(
        fooFile: TestFile? = null,
        sampleFile: TestFile,
        expectedText: String? = null,
        requiresPartialAnalysis: Boolean = true
    ) {
        val projectDescriptions = mutableListOf<ProjectDescription>()
        val fooProject = ProjectDescription().apply {
            name = fooModuleName
            type = ProjectDescription.Type.LIBRARY
            fooFile?.let { addFile(fooFile) }
        }
        projectDescriptions += fooProject

        val sampleProject = ProjectDescription().apply {
            name = sampleModuleName
            type = ProjectDescription.Type.LIBRARY
            under = fooProject
            files = arrayOf(sampleFile, sampledStub)
            dependsOn(fooProject)
        }
        projectDescriptions += sampleProject

        lint().run {
            projects(*projectDescriptions.toTypedArray())
            if (requiresPartialAnalysis) {
                // The lint check will only work in partial analysis, so we expect non-partial
                // test modes to have no errors. This has to be called before run().
                expectIdenticalTestModeOutput(false)
            }
            run().run {
                if (expectedText == null) {
                    expectClean()
                } else {
                    if (requiresPartialAnalysis) {
                        // expectClean doesn't accept a testMode param
                        expect("No warnings.", testMode = TestMode.DEFAULT)
                        expect(expectedText, testMode = TestMode.PARTIAL)
                    } else {
                        expect(expectedText)
                    }
                }
            }
        }
    }

    @Test
    fun unresolvedSampleLink_Function() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """
        )

        val sampleFile = emptySampleFile

        val expected =
            """$barFilePath:6: Error: Couldn't find a valid @Sampled function matching foo.samples.sampleBar [UnresolvedSampleLink]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = expected)
    }

    @Test
    fun unannotatedSampleFunction_Function() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """
        )

        val sampleFile = unannotatedSampleFile

        val expected =
            """$barFilePath:6: Error: Couldn't find a valid @Sampled function matching foo.samples.sampleBar [UnresolvedSampleLink]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = expected)
    }

    @Test
    fun multipleMatchingSampleFunctions_Function() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """
        )

        val sampleFile = multipleMatchingSampleFile

        val expected =
            """$sampleFilePath:10: Error: Found multiple functions matching foo.samples.sampleBar [MultipleSampledFunctions]
            fun sampleBar(param: Boolean = false) {}
                ~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(
            fooFile = fooFile,
            sampleFile = sampleFile,
            expectedText = expected,
            // Since this particular is all done inside the same module, partial analysis isn't
            // required so it will still work in global analysis
            requiresPartialAnalysis = false
        )
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Function() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """
        )

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = null)
    }

    @Test
    fun unresolvedSampleLink_Class() {
        val fooFile = kotlin(
            """
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar
        """
        )

        val sampleFile = emptySampleFile

        val expected =
            """$barFilePath:5: Error: Couldn't find a valid @Sampled function matching foo.samples.sampleBar [UnresolvedSampleLink]
             * @sample foo.samples.sampleBar
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = expected)
    }

    @Test
    fun unannotatedSampleFunction_Class() {
        val fooFile = kotlin(
            """
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar
        """
        )

        val sampleFile = unannotatedSampleFile

        val expected =
            """$barFilePath:5: Error: Couldn't find a valid @Sampled function matching foo.samples.sampleBar [UnresolvedSampleLink]
             * @sample foo.samples.sampleBar
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = expected)
    }

    @Test
    fun multipleMatchingSampleFunctions_Class() {
        val fooFile = kotlin(
            """
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar
        """
        )

        val sampleFile = multipleMatchingSampleFile

        val expected =
            """$sampleFilePath:10: Error: Found multiple functions matching foo.samples.sampleBar [MultipleSampledFunctions]
            fun sampleBar(param: Boolean = false) {}
                ~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(
            fooFile = fooFile,
            sampleFile = sampleFile,
            expectedText = expected,
            // Since this particular is all done inside the same module, partial analysis isn't
            // required so it will still work in global analysis
            requiresPartialAnalysis = false
        )
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Class() {
        val fooFile = kotlin(
            """
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar
        """
        )

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = null)
    }

    @Test
    fun unresolvedSampleLink_Field() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """
        )

        val sampleFile = emptySampleFile

        val expected =
            """$barFilePath:6: Error: Couldn't find a valid @Sampled function matching foo.samples.sampleBar [UnresolvedSampleLink]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = expected)
    }

    @Test
    fun unannotatedSampleFunction_Field() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """
        )

        val sampleFile = unannotatedSampleFile

        val expected =
            """$barFilePath:6: Error: Couldn't find a valid @Sampled function matching foo.samples.sampleBar [UnresolvedSampleLink]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = expected)
    }

    @Test
    fun multipleMatchingSampleFunctions_Field() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """
        )

        val sampleFile = multipleMatchingSampleFile

        val expected =
            """$sampleFilePath:10: Error: Found multiple functions matching foo.samples.sampleBar [MultipleSampledFunctions]
            fun sampleBar(param: Boolean = false) {}
                ~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(
            fooFile = fooFile,
            sampleFile = sampleFile,
            expectedText = expected,
            // Since this particular is all done inside the same module, partial analysis isn't
            // required so it will still work in global analysis
            requiresPartialAnalysis = false
        )
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Field() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """
        )

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = null)
    }

    @Test
    fun unresolvedSampleLink_PropertyWithGetter() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              val bar get() = 0
            }
        """
        )

        val sampleFile = emptySampleFile

        val expected =
            """$barFilePath:6: Error: Couldn't find a valid @Sampled function matching foo.samples.sampleBar [UnresolvedSampleLink]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = expected)
    }

    @Test
    fun unannotatedSampleFunction_PropertyWithGetter() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              val bar get() = 0
            }
        """
        )

        val sampleFile = unannotatedSampleFile

        val expected =
            """$barFilePath:6: Error: Couldn't find a valid @Sampled function matching foo.samples.sampleBar [UnresolvedSampleLink]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = expected)
    }

    @Test
    fun multipleMatchingSampleFunctions_PropertyWithGetter() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              val bar get() = 0
            }
        """
        )

        val sampleFile = multipleMatchingSampleFile

        val expected =
            """$sampleFilePath:10: Error: Found multiple functions matching foo.samples.sampleBar [MultipleSampledFunctions]
            fun sampleBar(param: Boolean = false) {}
                ~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(
            fooFile = fooFile,
            sampleFile = sampleFile,
            expectedText = expected,
            // Since this particular is all done inside the same module, partial analysis isn't
            // required so it will still work in global analysis
            requiresPartialAnalysis = false
        )
    }

    @Test
    fun correctlyAnnotatedSampleFunction_PropertyWithGetter() {
        val fooFile = kotlin(
            """
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              val bar get() = 0
            }
        """
        )

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile, expectedText = null)
    }

    @Test
    fun obsoleteSampledFunction() {
        val sampleFile = correctlyAnnotatedSampleFile

        val expected =
            """$sampleFilePath:7: Error: foo.samples.sampleBar is annotated with @Sampled, but is not linked to from a @sample tag. [ObsoleteSampledAnnotation]
            fun sampleBar() {}
                ~~~~~~~~~
1 errors, 0 warnings

        """

        checkKotlin(sampleFile = sampleFile, expectedText = expected)
    }

    @Test
    fun invalidSampleLocation() {
        val sampleFile = kotlin(
            """
            package foo.wrong.location

            import androidx.annotation.Sampled

            @Sampled
            fun sampleBar() {}
        """
        )

        val expected =
            """samples/src/foo/wrong/location/test.kt:7: Error: foo.wrong.location.sampleBar is annotated with @Sampled, but is not linked to from a @sample tag. [ObsoleteSampledAnnotation]
            fun sampleBar() {}
                ~~~~~~~~~
1 errors, 0 warnings

        """

        checkKotlin(sampleFile = sampleFile, expectedText = expected)
    }
}
/* ktlint-enable max-line-length */
