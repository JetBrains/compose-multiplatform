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

@file:Suppress("KDocUnresolvedReference")

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

/**
 * Test for [SampledAnnotationEnforcer]
 *
 * This tests (with Parameterized) the two following module setups:
 *
 * Module 'foo', which lives in foo
 * Module 'foo:integration-tests:samples', which lives in foo/integration-tests/samples,
 * and depends on 'foo'
 *
 * Module 'foo:foo', which lives in foo/foo
 * Module 'foo:integration-tests:samples', which lives in foo/integration-tests/samples,
 * and depends on 'foo:foo'
 */
@RunWith(Parameterized::class)
class SampledAnnotationEnforcerTest {

    companion object {
        @JvmStatic
        @Parameters(name = "sourceModule={0}")
        fun moduleNames(): Array<String> {
            return arrayOf("foo", "foo:foo")
        }
    }

    // At runtime this contains one of the values listed in moduleNames()
    @Parameter lateinit var fooModuleName: String

    private val sampleModuleName = "foo:integration-tests:samples"

    // The path to Bar.kt changes depending on what module we are in
    private val barFilePath by lazy {
        val prefix = if (fooModuleName == moduleNames()[0]) "" else "foo:foo/"
        prefix + "src/foo/Bar.kt"
    }

    private val emptySampleFile = kotlin("""
            package foo.samples
        """)

    private val unannotatedSampleFile = kotlin("""
            package foo.samples

            fun sampleBar() {}
        """)

    private val multipleMatchingSampleFile = kotlin("""
            package foo.samples

            fun sampleBar() {}

            fun sampleBar() {}
        """)

    private val correctlyAnnotatedSampleFile = kotlin("""
            package foo.samples

            @Sampled
            fun sampleBar() {}
        """)

    private fun checkKotlin(
        fooFile: TestFile? = null,
        sampleFile: TestFile? = null,
        sampleModuleNameOverride: String? = null
    ): TestLintResult {
        val fooProject = ProjectDescription().apply {
            name = fooModuleName
            fooFile?.let { files = arrayOf(fooFile) }
        }
        val sampleProject = ProjectDescription().apply {
            name = sampleModuleNameOverride ?: sampleModuleName
            sampleFile?.let { files = arrayOf(sampleFile) }
            dependsOn(fooProject)
        }
        return lint()
            .projects(fooProject, sampleProject)
            .allowMissingSdk(true)
            .issues(
                SampledAnnotationEnforcer.MISSING_SAMPLED_ANNOTATION,
                SampledAnnotationEnforcer.OBSOLETE_SAMPLED_ANNOTATION,
                SampledAnnotationEnforcer.MISSING_SAMPLES_DIRECTORY,
                SampledAnnotationEnforcer.UNRESOLVED_SAMPLE_LINK,
                SampledAnnotationEnforcer.MULTIPLE_FUNCTIONS_FOUND,
                SampledAnnotationEnforcer.INVALID_SAMPLES_LOCATION
            )
            .run()
    }

    @Test
    fun orphanedSampleFunction() {
        val sampleFile = correctlyAnnotatedSampleFile

        val path = if (fooModuleName == moduleNames()[0]) { "" } else { "foo" }

        val expected =
"$path:integration-tests:samples/src/foo/samples/test.kt:5: Error: sampleBar is annotated with" +
""" @Sampled, but is not linked to from a @sample tag. [EnforceSampledAnnotation]
            fun sampleBar() {}
                ~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun invalidSampleLocation() {
        val sampleFile = kotlin("""
            package foo.wrong.location

            @Sampled
            fun sampleBar() {}
        """)

        val path = if (fooModuleName == moduleNames()[0]) { "" } else { "foo" }

        val expected =
"$path:integration-tests:wrong-location/src/foo/wrong/location/test.kt:5: Error: sampleBar in " +
"/TESTROOT/foo:integration-tests:wrong-location/src/foo/wrong/location/test.kt is annotated " +
"""with @Sampled, but is not inside a project/directory named samples. [EnforceSampledAnnotation]
            fun sampleBar() {}
                ~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(
            sampleFile = sampleFile,
            sampleModuleNameOverride = "foo:integration-tests:wrong-location"
        ).expect(expected)
    }

    @Test
    fun missingSampleDirectory_Function() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """)

        val expected =
    "$barFilePath:6: Error: Couldn't find a valid samples directory in this project" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile)
            .expect(expected)
    }

    @Test
    fun unresolvedSampleLink_Function() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """)

        val sampleFile = emptySampleFile

        val expected =
"$barFilePath:6: Error: Couldn't find a valid function matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun unannotatedSampleFunction_Function() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """)

        val sampleFile = unannotatedSampleFile

        val path = if (fooModuleName == moduleNames()[0]) { "" } else { "foo:foo/" }

        val expected =
"${path}src/foo/Bar.kt:6: Error: sampleBar is not annotated with @Sampled, but is linked to from" +
""" the KDoc of bar [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun multipleMatchingSampleFunctions_Function() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """)

        val sampleFile = multipleMatchingSampleFile

        val expected =
"$barFilePath:6: Error: Found multiple functions matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Function() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              fun bar() {}
            }
        """)

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectClean()
    }

    @Test
    fun missingSampleDirectory_Class() {
        val fooFile = kotlin("""
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar {}
        """)

        val expected =
"$barFilePath:5: Error: Couldn't find a valid samples directory in this project" +
""" [EnforceSampledAnnotation]
             * @sample foo.samples.sampleBar
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile)
            .expect(expected)
    }

    @Test
    fun unresolvedSampleLink_Class() {
        val fooFile = kotlin("""
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar {}
        """)

        val sampleFile = emptySampleFile

        val expected =
"$barFilePath:5: Error: Couldn't find a valid function matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
             * @sample foo.samples.sampleBar
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun unannotatedSampleFunction_Class() {
        val fooFile = kotlin("""
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar {}
        """)

        val sampleFile = unannotatedSampleFile

        val expected =
"$barFilePath:5: Error: sampleBar is not annotated with @Sampled, but is linked to from" +
""" the KDoc of Bar [EnforceSampledAnnotation]
             * @sample foo.samples.sampleBar
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun multipleMatchingSampleFunctions_Class() {
        val fooFile = kotlin("""
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar {}
        """)

        val sampleFile = multipleMatchingSampleFile

        val expected =
"$barFilePath:5: Error: Found multiple functions matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
             * @sample foo.samples.sampleBar
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Class() {
        val fooFile = kotlin("""
            package foo

            /**
             * @sample foo.samples.sampleBar
             */
            class Bar {}
        """)

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectClean()
    }

    @Test
    fun missingSampleDirectory_Field() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """)

        val expected =
"$barFilePath:6: Error: Couldn't find a valid samples directory in this project" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile)
            .expect(expected)
    }

    @Test
    fun unresolvedSampleLink_Field() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """)

        val sampleFile = emptySampleFile

        val expected =
"$barFilePath:6: Error: Couldn't find a valid function matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun unannotatedSampleFunction_Field() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """)

        val sampleFile = unannotatedSampleFile

        val expected =
"$barFilePath:6: Error: sampleBar is not annotated with @Sampled, but is linked to from" +
""" the KDoc of bar [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun multipleMatchingSampleFunctions_Field() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """)

        val sampleFile = multipleMatchingSampleFile

        val expected =
"$barFilePath:6: Error: Found multiple functions matching foo.samples.sampleBar" +
""" [EnforceSampledAnnotation]
               * @sample foo.samples.sampleBar
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expect(expected)
    }

    @Test
    fun correctlyAnnotatedSampleFunction_Field() {
        val fooFile = kotlin("""
            package foo

            class Bar {
              /**
               * @sample foo.samples.sampleBar
               */
              const val bar = 0
            }
        """)

        val sampleFile = correctlyAnnotatedSampleFile

        checkKotlin(fooFile = fooFile, sampleFile = sampleFile)
            .expectClean()
    }
}
