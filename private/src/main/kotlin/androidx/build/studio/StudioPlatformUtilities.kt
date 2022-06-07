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

package androidx.build.studio

import org.gradle.process.ExecSpec
import java.io.File
import java.util.Locale

/**
 * Utility class containing helper functions and values that change between Linux and OSX
 *
 * @property projectRoot the root directory of the current project
 * @property studioInstallationDir the directory where studio is installed to
 */
sealed class StudioPlatformUtilities(val projectRoot: File, val studioInstallationDir: File) {
    /**
     * The file extension used for this platform's Studio archive
     */
    abstract val archiveExtension: String

    /**
     * The binary directory of the Studio installation.
     */
    abstract val StudioTask.binaryDirectory: File

    /**
     * A list of arguments that will be executed in a shell to launch Studio.
     */
    abstract val StudioTask.launchCommandArguments: List<String>

    /**
     * The lib directory of the Studio installation.
     */
    abstract val StudioTask.libDirectory: File

    /**
     * The plugins directory of the Studio installation.
     * TODO: Consider removing after Studio has switched to Kotlin 1.4
     * b/162414740
     */
    abstract val StudioTask.pluginsDirectory: File

    /**
     * The license path for the Studio installation.
     */
    abstract val StudioTask.licensePath: String

    /**
     * Extracts an archive at [fromPath] with [archiveExtension] to [toPath]
     */
    abstract fun extractArchive(fromPath: String, toPath: String, execSpec: ExecSpec)

    /**
     * Updates the Jvm heap size for this Studio installation.
     * TODO: this is temporary until b/135183535 is fixed
     */
    abstract fun StudioTask.updateJvmHeapSize()

    /**
     * Regex to match '-Xmx512m' or similar, so we can replace it with a larger heap size.
     */
    protected val jvmHeapRegex = "-Xmx.*".toRegex()

    companion object {
        val osName = if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("linux")) {
            "linux"
        } else {
            // Only works when using native version of JDK, otherwise it will fallback to x86_64
            if (System.getProperty("os.arch") == "aarch64") {
                "mac_arm"
            } else {
                "mac"
            }
        }

        fun get(projectRoot: File, studioInstallationDir: File): StudioPlatformUtilities {
            return if (osName == "linux") {
                LinuxUtilities(projectRoot, studioInstallationDir)
            } else {
                MacOsUtilities(projectRoot, studioInstallationDir)
            }
        }
    }
}

private class MacOsUtilities(projectRoot: File, studioInstallationDir: File) :
    StudioPlatformUtilities(projectRoot, studioInstallationDir) {
    override val archiveExtension: String get() = ".zip"

    override val StudioTask.binaryDirectory: File
        get() {
            val file = studioInstallationDir.walk().maxDepth(1).find { file ->
                file.nameWithoutExtension.startsWith("Android Studio") &&
                    file.extension == "app"
            }
            return requireNotNull(file) { "Android Studio*.app not found!" }
        }

    override val StudioTask.launchCommandArguments: List<String>
        get() {
            val studioBinary = File(binaryDirectory.absolutePath, "Contents/MacOS/studio")
            return listOf(
                studioBinary.absolutePath,
                projectRoot.absolutePath
            )
        }

    override val StudioTask.libDirectory: File
        get() = File(binaryDirectory, "Contents/lib")

    override val StudioTask.pluginsDirectory: File
        get() = File(binaryDirectory, "Contents/plugins")

    override val StudioTask.licensePath: String
        get() = File(binaryDirectory, "Contents/Resources/LICENSE.txt").absolutePath

    override fun extractArchive(fromPath: String, toPath: String, execSpec: ExecSpec) {
        with(execSpec) {
            executable("unzip")
            args(fromPath, "-d", toPath)
        }
    }

    override fun StudioTask.updateJvmHeapSize() {
        val vmoptions = File(binaryDirectory, "Contents/bin/studio.vmoptions")
        val newText = vmoptions.readText().replace(jvmHeapRegex, "-Xmx8g")
        vmoptions.writeText(newText)
    }
}

private class LinuxUtilities(projectRoot: File, studioInstallationDir: File) :
    StudioPlatformUtilities(projectRoot, studioInstallationDir) {
    override val archiveExtension: String get() = ".tar.gz"

    override val StudioTask.binaryDirectory: File
        get() = File(studioInstallationDir, "android-studio")

    override val StudioTask.launchCommandArguments: List<String>
        get() {
            val studioScript = File(binaryDirectory, "bin/studio.sh")
            return listOf(
                "sh",
                studioScript.absolutePath,
                projectRoot.absolutePath
            )
        }

    override val StudioTask.pluginsDirectory: File
        get() = File(binaryDirectory, "plugins")

    override val StudioTask.libDirectory: File
        get() = File(binaryDirectory, "lib")

    override val StudioTask.licensePath: String
        get() = File(binaryDirectory, "LICENSE.txt").absolutePath

    override fun extractArchive(fromPath: String, toPath: String, execSpec: ExecSpec) {
        with(execSpec) {
            executable("tar")
            args("-xf", fromPath, "-C", toPath)
        }
    }

    override fun StudioTask.updateJvmHeapSize() {
        val vmoptions64 =
            File(binaryDirectory, "bin/studio64.vmoptions")
        val newText64 = vmoptions64.readText().replace(jvmHeapRegex, "-Xmx8g")
        vmoptions64.writeText(newText64)
    }
}
