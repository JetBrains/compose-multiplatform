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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.process.JavaForkOptions

/**
 * Configures screenshot testing using Paparazzi for AndroidX projects.
 */
class AndroidXPaparazziImplPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val paparazziNative = project.createUnzippedPaparazziNativeDependency()
        project.afterEvaluate {
            project.tasks.withType<Test>().configureEach { it.configureTestTask(paparazziNative) }
        }
    }

    /**
     * Add project's golden directory and the unzipped native Paparazzi location as task inputs,
     * and set system properties for the test library to consume at runtime.
     */
    private fun Test.configureTestTask(paparazziNative: FileCollection) {
        val platformDirectory = project.getSdkPath().resolve("platforms/$COMPILE_SDK_VERSION")
        val goldenRootDirectory = project.getSupportRootFolder().resolve("../../golden")
        val modulePath = project.path.replace(':', '/').trim('/')
        val android = project.the<BaseExtension>()
        val packageName = requireNotNull(android.namespace) {
            "android.namespace must be set for Paparazzi"
        }

        // Attach unzipped Paparazzi native directory as a task input
        inputs.files(paparazziNative)
            .withPathSensitivity(PathSensitivity.NONE)
            .withPropertyName("paparazziNative")

        // Attach golden directory to task inputs to invalidate tests when updating goldens
        inputs.dir(goldenRootDirectory.resolve(modulePath))
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .withPropertyName("goldenDirectory")

        // Set non-path system properties at configuration time, so that changes invalidate caching
        prefixedSystemProperties(
            "compileSdkVersion" to TARGET_SDK_VERSION,
            "packageName" to packageName,
            "resourcePackageNames" to packageName, // TODO: Transitive resource packages?
            "modulePath" to modulePath
        )

        // Set the remaining system properties at execution time, after the snapshotting, so that
        // the absolute paths don't affect caching
        doFirst {
            prefixedSystemProperties(
                "platformDir" to platformDirectory.canonicalPath,
                "platformDataDir" to paparazziNative.singleFile.canonicalPath,
                "assetsDir" to ".", // TODO: Merged assets dirs? (needed for compose?)
                "resDir" to ".", // TODO: Merged resource dirs? (needed for compose?)
                "reportDir" to reports.junitXml.outputLocation.get().asFile.canonicalPath,
                "goldenRootDir" to goldenRootDirectory.canonicalPath,
            )
        }
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

    private companion object {
        /** Package name of the test library, used to namespace system properties */
        const val PACKAGE_NAME = "androidx.test.screenshot.paparazzi"

        /** Artifact type attribute for unzipped Paparazzi layoutlib unzipped artifacts */
        const val UNZIPPED_PAPARAZZI_NATIVE = "unzipped-paparazzi-native"

        /** Set system properties with keys prefixed with [PACKAGE_NAME] */
        fun JavaForkOptions.prefixedSystemProperties(vararg properties: Pair<String, Any>) {
            properties.forEach { (name, value) -> systemProperty("$PACKAGE_NAME.$name", value) }
        }
    }
}
