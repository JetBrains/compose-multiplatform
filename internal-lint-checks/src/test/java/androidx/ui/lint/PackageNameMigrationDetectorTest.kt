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

package androidx.ui.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
/**
 * Test for [PackageNameMigrationDetector]
 *
 * TODO: b/160233169 remove this lint check after the migration has finished.
 */
class PackageNameMigrationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = PackageNameMigrationDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(PackageNameMigrationDetector.ISSUE)

    @Test
    fun oldPackageShouldFail() {
        lint().files(
            kotlin(
                """
                package androidx.ui.foo

                fun someApi() {}
            """
            )
        )
            .run()
            .expect(
                """
src/androidx/ui/foo/test.kt:1: Error: The package name 'androidx.ui.foo' has been migrated to 'androidx.compose.foo', please update the package name of this file accordingly. [PackageNameMigration]

^
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun newPackageShouldPass() {
        lint().files(
            kotlin(
                """
                package androidx.compose.foo

                fun someApi() {}
            """
            )
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
