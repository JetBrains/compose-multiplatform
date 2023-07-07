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

package androidx.inspection.gradle

import androidx.testutils.gradle.ProjectSetupRule
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class InspectionPluginTest {
    @get:Rule
    val projectSetup = ProjectSetupRule()

    lateinit var gradleRunner: GradleRunner
    lateinit var dxExecutable: String

    @Before
    fun setUp() {
        val sdkDir = projectSetup.getSdkDirectory()
        dxExecutable = File(sdkDir, "build-tools/${projectSetup.props.buildToolsVersion}/dx")
            .absolutePath
        File("src/test/test-data", "app-project").copyRecursively(projectSetup.rootDir)

        gradleRunner = GradleRunner.create()
            .withProjectDir(projectSetup.rootDir)
            .withPluginClasspath()
    }

    @Ignore // b/193918205
    @Test
    fun applyInspection() {
        File(projectSetup.rootDir, "settings.gradle")
            .writeText("rootProject.name = \"test-inspector\"")
        projectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.library")
                    id("androidx.inspection")
                }
            """.trimIndent(),
            suffix = """
                dependencies {
                    implementation("androidx.inspection:inspection:1.0.0")
                }
                android {
                    defaultConfig {
                        targetSdkVersion 30
                    }
                }
            """
        )
        val output = gradleRunner.withArguments("dexInspectorRelease", "--stacktrace").build()
        assertEquals(output.task(":dexInspectorRelease")!!.outcome, TaskOutcome.SUCCESS)
        val artifact = File(
            projectSetup.rootDir,
            "build/androidx_inspection/dexedInspector/release/test-inspector.jar"
        )
        assertTrue { artifact.exists() }
        assertDeclaredInDex(artifact, "Ltest/inspector/TestInspector;")
        assertDeclaredInDex(artifact, "Ltest/inspector/TestInspectorProtocol;")
        assertDeclaredInDex(artifact, "Ldeps/test/inspector/com/google/protobuf/ByteString;")
    }

    // rely that classes should have a constructor and it is declared in class itself
    private fun assertDeclaredInDex(artifact: File, className: String) {
        val exec = Runtime.getRuntime().exec(
            arrayOf(dxExecutable, "--find-usages", artifact.absolutePath, className, "<init>")
        )
        exec.waitFor()
        assertEquals(exec.exitValue(), 0)

        assertTrue {
            BufferedReader(InputStreamReader(exec.inputStream)).readLines().any {
                it.contains("<init> method declared")
            }
        }
    }
}
