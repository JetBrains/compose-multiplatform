/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.build

import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuiltArtifactsLoader
import com.android.build.api.variant.HasAndroidTest
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.getByType
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Expected to rerun every time")
abstract class FtlRunner : DefaultTask() {
    init {
        group = "Verification"
        description = "Runs devices tests in Firebase Test Lab filtered by --className"
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testFolder: DirectoryProperty

    @get:Internal
    abstract val testLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val appFolder: DirectoryProperty

    @get:Internal
    abstract val appLoader: Property<BuiltArtifactsLoader>

    @get:Optional
    @get:Input
    @get:Option(option = "className", description = "Fully qualified class name of a class to run")
    abstract val className: Property<String>

    @get:Input
    abstract val device: Property<String>

    @TaskAction
    fun execThings() {
        val testApk = testLoader.get().load(testFolder.get())
            ?: throw RuntimeException("Cannot load required APK for task: $name")
        val testApkPath = testApk.elements.single().outputFile
        val appApkPath = if (appLoader.isPresent) {
            val appApk = appLoader.get().load(appFolder.get())
                ?: throw RuntimeException("Cannot load required APK for task: $name")
            appApk.elements.single().outputFile
        } else {
            "gs://androidx-ftl-test-results/github-ci-action/placeholderApp/" +
                "aadb5e0219ce132e73236ef1e06bb50dd60217e20e803ea00d57a1cf1cea902c.apk"
        }
        try {
            execOperations.exec {
                    it.commandLine("gcloud", "--version")
            }
        } catch (exception: Exception) {
            throw Exception(
                "Missing gcloud, please follow go/androidx-dev#remote-build-cache to set it up"
            )
        }
        execOperations.exec {
            it.commandLine(
                listOfNotNull(
                "gcloud",
                "--project",
                "androidx-dev-prod",
                "firebase",
                "test",
                "android",
                "run",
                "--type",
                "instrumentation",
                "--no-performance-metrics",
                "--no-auto-google-login",
                "--device",
                "model=${device.get()},locale=en_US,orientation=portrait",
                "--app",
                appApkPath,
                "--test",
                testApkPath,
                if (className.isPresent) "--test-targets" else null,
                if (className.isPresent) "class ${className.get()}" else null,
                )
            )
        }
    }
}

private const val PIXEL2_API30_PREFIX = "ftlpixel2api30"
private const val NEXUS4_API21_PREFIX = "ftlnexus4api21"

fun Project.configureFtlRunner() {
    extensions.getByType(AndroidComponentsExtension::class.java).apply {
        onVariants { variant ->
            var name: String? = null
            var artifacts: Artifacts? = null
            when {
                variant is HasAndroidTest -> {
                    name = variant.androidTest?.name
                    artifacts = variant.androidTest?.artifacts
                }

                project.plugins.hasPlugin("com.android.test") -> {
                    name = variant.name
                    artifacts = variant.artifacts
                }
            }
            if (name == null || artifacts == null) {
                return@onVariants
            }
            tasks.register("$PIXEL2_API30_PREFIX$name", FtlRunner::class.java) { task ->

                task.device.set("Pixel2.arm,version=30")
                task.testFolder.set(artifacts.get(SingleArtifact.APK))
                task.testLoader.set(artifacts.getBuiltArtifactsLoader())
            }
            tasks.register("$NEXUS4_API21_PREFIX$name", FtlRunner::class.java) { task ->
                task.device.set("Nexus4,version=21")
                task.testFolder.set(artifacts.get(SingleArtifact.APK))
                task.testLoader.set(artifacts.getBuiltArtifactsLoader())
            }
        }
    }
}

fun Project.addAppApkToFtlRunner() {
    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        onVariants(selector().withBuildType("debug")) { appVariant ->
            tasks.named("$PIXEL2_API30_PREFIX${appVariant.name}AndroidTest") { configTask ->
                configTask as FtlRunner
                configTask.appFolder.set(appVariant.artifacts.get(SingleArtifact.APK))
                configTask.appLoader.set(appVariant.artifacts.getBuiltArtifactsLoader())
            }
            tasks.named("$NEXUS4_API21_PREFIX${appVariant.name}AndroidTest") { configTask ->
                configTask as FtlRunner
                configTask.appFolder.set(appVariant.artifacts.get(SingleArtifact.APK))
                configTask.appLoader.set(appVariant.artifacts.getBuiltArtifactsLoader())
            }
        }
    }
}