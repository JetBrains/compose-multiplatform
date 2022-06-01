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

@file:Suppress("UnstableApiUsage")

package androix.compose.ui.test.manifest.lint

import androidx.compose.ui.test.manifest.lint.GradleDebugConfigurationDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GradleDebugConfigurationDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = GradleDebugConfigurationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(GradleDebugConfigurationDetector.ISSUE)

    @Test
    fun kotlin_manifestDependencyInBlockedConfigs_shouldRaiseIssue() {
            lint().files(
                gradle("build.gradle",
                    """
                    dependencies {
                        implementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                        api("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                        compileOnly("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                        runtimeOnly("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                        annotationProcessor("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                        lintChecks("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                        lintPublish("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    }
                """).indented()
            )
                .run()
                .expect(
                    """
                build.gradle:2: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    implementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:3: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    api("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:4: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    compileOnly("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:5: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    runtimeOnly("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:6: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    annotationProcessor("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:7: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    lintChecks("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:8: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    lintPublish("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 7 warnings
                """.trimIndent()
                )
    }

    @Test
    fun groovy_manifestDependencyInBlockedConfigs_shouldRaiseIssue() {
        lint().files(
            gradle("build.gradle",
                """
                    dependencies {
                        implementation 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                        api 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                        compileOnly 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                        runtimeOnly 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                        annotationProcessor 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                        lintChecks 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                        lintPublish 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                    }
                """).indented()
        )
            .run()
            .expect(
                """
                build.gradle:2: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    implementation 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:3: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    api 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:4: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    compileOnly 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:5: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    runtimeOnly 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:6: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    annotationProcessor 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:7: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    lintChecks 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:8: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    lintPublish 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 7 warnings
                """.trimIndent()
            )
    }

    @Test
    fun kotlin_manifestDependencyInCustomConfig_shouldNotRaiseIssue() {
        lint().files(
            gradle("build.gradle",
                """
                    dependencies {
                        customImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                        anotherConfig("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                        customApi("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    }
                """).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun groovy_manifestDependencyInCustomConfig_shouldNotRaiseIssue() {
        lint().files(
            gradle("build.gradle",
                """
                    dependencies {
                        customImplementation 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                        anotherConfig 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                        customApi 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                    }
                """).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun kotlin_manifestDependencyInDebugImplementation_shouldNotRaiseIssue() {
        lint().files(
            gradle("build.gradle",
                """
                    dependencies {
                        debugImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                        debugFlavorImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    }
                """).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun groovy_manifestDependencyInDebugImplementation_shouldNotRaiseIssue() {
        lint().files(
            gradle("build.gradle",
                """
                    dependencies {
                        debugImplementation 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                        debugFlavorImplementation 'androidx.compose.ui:ui-test-manifest:1.2.0-beta02'
                    }
                """).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun manifestDependencyInAndroidTestImplementation_shouldRaiseIssue() {
        lint().files(
            gradle("build.gradle",
                """
                    dependencies {
                        androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                        androidFlavorTestImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    }
                """).indented()
        )
            .run()
            .expect(
                """
                build.gradle:2: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:3: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    androidFlavorTestImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 2 warnings
                """.trimIndent()
            )
    }

    @Test
    fun manifestDependencyInTestImplementation_shouldRaiseIssue() {
        lint().files(
            gradle("build.gradle",
                """
                    dependencies {
                        testImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                        testFlavorImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    }
                """).indented()
        )
            .run()
            .expect(
                """
                build.gradle:2: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    testImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                build.gradle:3: Warning: Please use debugImplementation. [TestManifestGradleConfiguration]
                    testFlavorImplementation("androidx.compose.ui:ui-test-manifest:1.2.0-beta02")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 2 warnings
                """.trimIndent()
            )
    }
}