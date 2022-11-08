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

package androidx.build.paparazzi

import androidx.build.OperatingSystem
import androidx.build.SupportConfig.COMPILE_SDK_VERSION
import androidx.build.SupportConfig.TARGET_SDK_VERSION
import androidx.build.getLibraryByName
import androidx.build.getOperatingSystem
import androidx.build.getSdkPath
import androidx.build.getSupportRootFolder
import com.android.build.gradle.BaseExtension
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.process.JavaForkOptions

/**
 * Configures screenshot testing using Paparazzi for AndroidX projects.
 */
class AndroidXPaparazziImplPlugin @Inject constructor(
    private val fileSystemOperations: FileSystemOperations
) : Plugin<Project> {
    override fun apply(project: Project) {
        val paparazziNative = project.createUnzippedPaparazziNativeDependency()
        project.afterEvaluate { it.addTestUtilsDependency() }
        project.tasks.register("updateGolden")
        project.tasks.withType<Test>().configureEach { it.configureTestTask(paparazziNative) }
        project.tasks.withType<Test>().whenTaskAdded { project.registerUpdateGoldenTask(it) }
    }

    /**
     * Add project's golden directory and the unzipped native Paparazzi location as task inputs,
     * and set system properties for the test library to consume at runtime.
     */
    private fun Test.configureTestTask(paparazziNative: FileCollection) {
        val platformDirectory = project.getSdkPath().resolve("platforms/$COMPILE_SDK_VERSION")
        val cachedGoldenRootDirectory = project.goldenRootDirectory
        val cachedReportDirectory = reportDirectory
        val android = project.the<BaseExtension>()
        val packageName = requireNotNull(android.namespace) {
            "android.namespace must be set for Paparazzi"
        }

        // Attach unzipped Paparazzi native directory as a task input
        inputs.files(paparazziNative)
            .withPathSensitivity(PathSensitivity.NONE)
            .withPropertyName("paparazziNative")

        // Attach golden directory to task inputs to invalidate tests when updating goldens
        inputs.dir(project.goldenDirectory)
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .withPropertyName("goldenDirectory")

        // Mark report directory as an output directory
        outputs.dir(reportDirectory)
            .withPropertyName("paparazziReportDir")

        // Clean the contents of the report directory before each test run
        doFirst { fileSystemOperations.delete { it.delete(cachedReportDirectory.listFiles()) } }

        // Set non-path system properties at configuration time, so that changes invalidate caching
        prefixedSystemProperties(
            "gradlePluginApplied" to "true",
            "compileSdkVersion" to TARGET_SDK_VERSION,
            "resourcePackageNames" to packageName, // TODO: Transitive resource packages?
            "modulePath" to project.modulePath,
            "updateGoldenTask" to "${project.path}:${updateGoldenTaskName()}"
        )

        // Set the remaining system properties at execution time, after the snapshotting, so that
        // the absolute paths don't affect caching
        doFirst {
            systemProperty("paparazzi.platform.data.root", paparazziNative.singleFile.canonicalPath)
            prefixedSystemProperties(
                "platformDir" to platformDirectory.canonicalPath,
                "assetsDir" to ".", // TODO: Merged assets dirs? (needed for compose?)
                "resDir" to ".", // TODO: Merged resource dirs? (needed for compose?)
                "reportDir" to cachedReportDirectory.canonicalPath,
                "goldenRootDir" to cachedGoldenRootDirectory.canonicalPath,
            )
        }
    }

    /** Register a copy task for moving new images to the golden directory. */
    private fun Project.registerUpdateGoldenTask(testTask: Test) {
        tasks.register<Copy>(testTask.updateGoldenTaskName()) {
            dependsOn(testTask)

            from(testTask.reportDirectory) {
                include("**/*_actual.png")
                into(goldenDirectory)
                rename { it.removeSuffix("_actual.png") + "_paparazzi.png" }
            }
        }

        tasks["updateGolden"].dependsOn(testTask.updateGoldenTaskName())
    }

    /** Derive updateGolden task name from a test task name. */
    private fun Test.updateGoldenTaskName(): String {
        return "updateGolden" + name.removePrefix("test").replaceFirstChar { it.titlecase() }
    }

    /**
     * Configure [UnzipPaparazziNativeTransform] for the project, and add the platform-specific
     * Paparazzi native layoutlib dependency, using the version in `libs.versions.toml`.
     */
    private fun Project.createUnzippedPaparazziNativeDependency(): FileCollection {
        val platformSuffix = when (val os = getOperatingSystem()) {
            OperatingSystem.LINUX -> "LinuxX64"
            OperatingSystem.MAC -> {
                val arch = System.getProperty("os.arch")
                if (arch.startsWith("x86", ignoreCase = true)) "MacOsX64" else "MacOsArm64"
            }
            else -> error("Unsupported operating system $os for Paparazzi")
        }

        dependencies.registerTransform(UnzipPaparazziNativeTransform::class.java) { spec ->
            spec.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, JAR_TYPE)
            spec.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, UNZIPPED_PAPARAZZI_NATIVE)
        }

        val configuration = configurations.create("paparazziNative")
        configuration.dependencies.add(
            dependencies.create(getLibraryByName("paparazziNative$platformSuffix"))
        )

        return configuration.incoming.artifactView {
            it.attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, UNZIPPED_PAPARAZZI_NATIVE)
        }.files
    }

    /** The golden image directory for this project. */
    private val Project.goldenDirectory
        get() = goldenRootDirectory.resolve(modulePath)

    /** The root of the golden image directory in a standard AndroidX checkout. */
    private val Project.goldenRootDirectory
        get() = getSupportRootFolder().resolve("../../golden")

    /** Filesystem path for this module derived from Gradle project path. */
    private val Project.modulePath
        get() = path.replace(':', '/').trim('/')

    /** Output directory for storing reports and images. */
    private val Test.reportDirectory
        get() = project.buildDir.resolve("paparazzi").resolve(name)

    /** Add a testImplementation dependency on the wrapper test utils library. */
    private fun Project.addTestUtilsDependency() {
        configurations["testImplementation"].dependencies.add(
            dependencies.create(project(TEST_UTILS_PROJECT))
        )
    }

    private companion object {
        /** Package name of the test library, used to namespace system properties */
        const val PACKAGE_NAME = "androidx.testutils.paparazzi"

        /** Project path to the wrapper test utils project. */
        const val TEST_UTILS_PROJECT = ":internal-testutils-paparazzi"

        /** Artifact type attribute for unzipped Paparazzi layoutlib unzipped artifacts */
        const val UNZIPPED_PAPARAZZI_NATIVE = "unzipped-paparazzi-native"

        /** Set system properties with keys prefixed with [PACKAGE_NAME] */
        fun JavaForkOptions.prefixedSystemProperties(vararg properties: Pair<String, Any>) {
            properties.forEach { (name, value) -> systemProperty("$PACKAGE_NAME.$name", value) }
        }
    }
}
