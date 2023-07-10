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
import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import java.io.FileNotFoundException

/**
 * Implementation of [LintDetectorTest] that's slightly more Kotlin-friendly.
 */
abstract class AbstractLintDetectorTest(
    val useDetector: Detector,
    val useIssues: List<Issue>,
    val stubs: Array<TestFile> = emptyArray(),
) : LintDetectorTest() {
    override fun getDetector(): Detector = useDetector

    override fun getIssues(): List<Issue> = useIssues

    /**
     * Runs lint checks for the given [projects] using (optionally) specified [testModes].
     *
     * Test mode modification is available to work around issues regarding partial analysis
     * (b/188814760, b/201086161#comment4).
     */
    fun check(
        vararg projects: ProjectDescription,
        testModes: List<TestMode> = listOf(TestMode.DEFAULT, TestMode.PARTIAL)
    ): TestLintResult {
        // If we have stubs, push those into a virtual project and pass them through the call to
        // projects(), since attempting to call files() would overwrite the call to projects().
        val projectsWithStubs = if (stubs.isNotEmpty()) {
            arrayOf(*projects, project().files(*stubs))
        } else {
            projects
        }

        return lint()
            .projects(*projectsWithStubs)
            .testModes(testModes)
            .allowDuplicates()
            .run()
    }

    fun check(
        vararg files: TestFile,
    ): TestLintResult {
        return lint()
            .files(
                *stubs,
                *files
            )
            .run()
    }
}

/**
 * Creates a new [ProjectDescription].
 */
fun project(): ProjectDescription = ProjectDescription()

/**
 * Loads a [TestFile] from `AndroidManifest.xml` included in the JAR resources.
 */
fun manifestSample(): TestFile = TestFiles.manifest(
    Stubs::class.java.getResource(
        "/AndroidManifest.xml"
    )?.readText() ?: throw FileNotFoundException(
        "Could not find AndroidManifest.xml in the integration test project"
    )
)

/**
 * Loads a [TestFile] from Java source code included in the JAR resources.
 */
fun javaSample(className: String): TestFile = TestFiles.java(
    Stubs::class.java.getResource(
        "/java/${className.replace('.', '/')}.java"
    )?.readText() ?: throw FileNotFoundException(
        "Could not find Java sources for $className in the integration test project"
    )
)

/**
 * Loads a [TestFile] from Kotlin source code included in the JAR resources.
 */
fun ktSample(className: String): TestFile = TestFiles.kotlin(
    Stubs::class.java.getResource(
        "/java/${className.replace('.', '/')}.kt"
    )?.readText() ?: throw FileNotFoundException(
        "Could not find Kotlin sources for $className in the integration test project"
    )
)
