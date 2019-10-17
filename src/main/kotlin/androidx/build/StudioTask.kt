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

package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.service.ServiceRegistry
import org.gradle.process.ExecSpec
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Locale
import java.util.Properties

/**
 * Task responsible for updating / installing the Studio version used in the current root project,
 * and launching it.
 */
abstract class StudioTask : DefaultTask() {

    // TODO: support -y and --update-only options? Can use @Option for this
    @TaskAction
    fun studiow() {
        StudioWrapper.create(project).run {
            update()
            launch(services)
        }
    }

    companion object {
        private const val STUDIO_TASK = "studio"

        fun Project.registerStudioTask() {
            tasks.register(STUDIO_TASK, StudioTask::class.java)
            // TODO: b/142859295 re-enable IDE plugin when we fix circular dependency
            /*val taskProvider = tasks.register(STUDIO_TASK, StudioTask::class.java)
            if (isUiProject) {
                // Need to prepare the sandbox before we can run studio
                taskProvider.dependsOn(":compose:compose-ide-plugin:prepareSandbox")
            }*/
        }
    }
}

// TODO: compose-compiler-plugin relies on logic in here to set up the environment for its Intellij
// plugin. If that is changed then this can be made to extend DefaultTask like a normal task
/**
 * Project-independent common logic for updating / launching studio.
 *
 * @param project the root project that Studio should be launched for
 */
abstract class StudioWrapper(val project: Project) {

    protected val platformUtilities by lazy {
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")) {
            PlatformUtilities.LinuxUtilities
        } else {
            PlatformUtilities.MacOsUtilities
        }
    }

    protected val projectRoot: File = project.rootDir

    private val studioVersions by lazy {
        Properties().apply {
            studioVersionsFile.inputStream().use(::load)
        }
    }

    // Properties that come from studio_versions.properties
    // Note: not all of these may be set for both projects
    protected val ideaMajorVersion get() = studioVersions["idea_major_version"].toString()
    protected val ideaMinorVersion get() = studioVersions["idea_minor_version"].toString()
    protected val studioBuildNumber get() = studioVersions["studio_build_number"].toString()
    protected val studioVersion get() = studioVersions["studio_version"].toString()

    /**
     * Directory name (not path) that Studio will be unzipped into.
     */
    private val studioDirectoryName: String
        get() {
            val osName = platformUtilities.osName
            return "android-studio-ide-$ideaMajorVersion.$studioBuildNumber-$osName"
        }

    /**
     * Filename (not path) of the Studio archive
     */
    protected val studioArchiveName: String
        get() = studioDirectoryName + platformUtilities.archiveExtension

    /**
     * The install directory containing Studio
     */
    protected val studioInstallationDir by lazy { File(projectRoot, "studio/$studioDirectoryName") }

    /**
     * Absolute path of the Studio archive
     */
    protected val studioArchivePath: String by lazy {
        File(studioInstallationDir.parentFile, studioArchiveName).absolutePath
    }

    /**
     * The idea.properties file that we want to tell Studio to use
     */
    protected abstract val ideaProperties: File

    /**
     * The studio_versions.properties file containing information about the version of Studio to use
     */
    protected abstract val studioVersionsFile: File

    /**
     * Creates the archive file and places it at [studioArchivePath], to be extracted later.
     */
    protected abstract fun createStudioArchiveFile()

    /**
     * Called after Studio has been updated / installed, and before it has been launched. An
     * optional hook for further configuration of properties / settings contained inside the
     * Studio folder.
     */
    protected open fun postInstallConfiguration() {}

    /**
     * Called before Studio is launched, for up-to-date checks or similar that need to happen
     * every time Studio is launched, as opposed to once after install.
     */
    protected open fun preLaunchConfiguration() {}

    /**
     * Updates the Studio installation and removes any old installation files if they exist.
     */
    fun update() {
        if (!studioInstallationDir.exists()) {
            // Create installation directory and any needed parent directories
            studioInstallationDir.mkdirs()
            // Attempt to remove any old installations in the parent studio/ folder
            removeOldInstallations()
            createStudioArchiveFile()
            println("Extracting archive...")
            extractStudioArchive()
            with(platformUtilities) { updateJvmHeapSize() }
            postInstallConfiguration()
        }
    }

    /**
     * Launches Studio if the user accepts / has accepted the license agreement.
     */
    fun launch(services: ServiceRegistry) {
        if (checkLicenseAgreement(services)) {
            preLaunchConfiguration()
            println("Launching studio...")
            launchStudio()
        } else {
            println("Exiting without launching studio...")
        }
    }

    private fun launchStudio() {
        val supportRootDir = SupportConfig.getSupportRoot(project)
        val vmOptions = File(supportRootDir, "development/studio/studio.vmoptions")

        ProcessBuilder().apply {
            inheritIO()
            with(platformUtilities) { command(launchCommandArguments) }

            // Some environment properties are already set in gradlew, and these by default carry
            // through here
            // TODO: idea.properties should be different for main and ui, fix
            val additionalStudioEnvironmentProperties = mapOf(
                "STUDIO_PROPERTIES" to ideaProperties.absolutePath,
                "STUDIO_VM_OPTIONS" to vmOptions.absolutePath,
                // This environment variable prevents Studio from showing IDE inspection warnings
                // for nullability issues, if the context is deprecated. This environment variable
                // is consumed by InteroperabilityDetector.kt
                "ANDROID_LINT_NULLNESS_IGNORE_DEPRECATED" to "true"
            )

            environment().putAll(additionalStudioEnvironmentProperties)
            start()
        }
    }

    private fun checkLicenseAgreement(services: ServiceRegistry): Boolean {
        val licenseAcceptedFile = File("$studioInstallationDir/STUDIOW_LICENSE_ACCEPTED")
        if (!licenseAcceptedFile.exists()) {
            val licensePath = with(platformUtilities) { licensePath }

            val userInput = services.get(UserInputHandler::class.java)
            val acceptAgreement = userInput.askYesNoQuestion(
                "Do you accept the license agreement at $licensePath?",
                /* default answer*/ false
            )
            if (!acceptAgreement) {
                return false
            }
            licenseAcceptedFile.createNewFile()
        }
        return true
    }

    private fun extractStudioArchive() {
        val fromPath = studioArchivePath
        val toPath = studioInstallationDir.absolutePath
        println("Extracting to $toPath...")
        project.exec { execSpec -> platformUtilities.extractArchive(fromPath, toPath, execSpec) }
        // Remove studio archive once done
        File(studioArchivePath).delete()
    }

    private fun removeOldInstallations() {
        val parentFile = studioInstallationDir.parentFile
        parentFile.walk().maxDepth(1)
            .filter { file ->
                // Remove any files that aren't either the directory / archive matching the
                // current version, and also ignore the parent `studio/` directory
                !file.name.contains(studioInstallationDir.name) && file != parentFile
            }
            .forEach { file ->
                println("Removing old installation file ${file.absolutePath}")
                file.deleteRecursively()
            }
    }

    companion object {
        /**
         * Creates the relevant [StudioWrapper] for the current root project.
         */
        @JvmStatic
        fun create(project: Project) = if (isUiProject) {
            UiStudioWrapper(project)
        } else {
            RootStudioWrapper(project)
        }
    }

    /**
     * Utility class containing helper functions and values that change between Linux and OSX
     */
    sealed class PlatformUtilities {
        /**
         * The file extension used for this platform's Studio archive
         */
        abstract val archiveExtension: String

        /**
         * The name of this platform - this matches Studio's naming for their downloads.
         */
        abstract val osName: String
        /**
         * The binary directory of the Studio installation.
         */
        abstract val StudioWrapper.binaryDirectory: File

        /**
         * The build.txt file of the Studio installation.
         */
        abstract val StudioWrapper.buildTxt: File

        /**
         * The directory to copy the built compose ide plugn into, where it will get automatically
         * picked up and enabled by Studio.
         */
        abstract val StudioWrapper.composeIdePluginDirectory: File

        /**
         * A list of arguments that will be executed in a shell to launch Studio.
         */
        abstract val StudioWrapper.launchCommandArguments: List<String>

        /**
         * The lib directory of the Studio installation.
         */
        abstract val StudioWrapper.libDirectory: File

        /**
         * The license path for the Studio installation.
         */
        abstract val StudioWrapper.licensePath: String

        /**
         * Extracts an archive at [fromPath] with [archiveExtension] to [toPath]
         */
        abstract fun extractArchive(fromPath: String, toPath: String, execSpec: ExecSpec)

        /**
         * Updates the Jvm heap size for this Studio installation.
         * TODO: this is temporary until b/135183535 is fixed
         */
        abstract fun StudioWrapper.updateJvmHeapSize()

        /**
         * Regex to match '-Xmx512m' or similar, so we can replace it with a larger heap size.
         */
        protected val jvmHeapRegex = "-Xmx.*".toRegex()

        object MacOsUtilities : PlatformUtilities() {
            override val archiveExtension: String get() = ".zip"

            override val osName: String get() = "mac"

            override val StudioWrapper.binaryDirectory: File
                get() {
                    val file = studioInstallationDir.walk().maxDepth(1).find { file ->
                        file.nameWithoutExtension.startsWith("Android Studio") &&
                                file.extension == "app"
                    }
                    return requireNotNull(file) { "Android Studio*.app not found!" }
                }

            override val StudioWrapper.buildTxt: File
                get() = File(binaryDirectory, "Contents/Resources/build.txt")

            override val StudioWrapper.composeIdePluginDirectory: File
                get() = File(binaryDirectory, "Contents/plugins/compose-ide-plugin")

            override val StudioWrapper.launchCommandArguments: List<String>
                get() {
                    return listOf(
                        "open",
                        "-a",
                        binaryDirectory.absolutePath,
                        projectRoot.absolutePath
                    )
                }

            override val StudioWrapper.libDirectory: File
                get() = File(binaryDirectory, "Contents/lib")

            override val StudioWrapper.licensePath: String
                get() = File(binaryDirectory, "Contents/Resources/LICENSE.txt").absolutePath

            override fun extractArchive(fromPath: String, toPath: String, execSpec: ExecSpec) {
                with(execSpec) {
                    executable("unzip")
                    args(fromPath, "-d", toPath)
                }
            }

            override fun StudioWrapper.updateJvmHeapSize() {
                val vmoptions = File(binaryDirectory, "Contents/bin/studio.vmoptions")
                val newText = vmoptions.readText().replace(jvmHeapRegex, "-Xmx8g")
                vmoptions.writeText(newText)
            }
        }

        object LinuxUtilities : PlatformUtilities() {
            override val archiveExtension: String get() = ".tar.gz"

            override val osName: String get() = "linux"

            override val StudioWrapper.binaryDirectory: File
                get() = File(studioInstallationDir, "android-studio")

            override val StudioWrapper.buildTxt: File
                get() = File(binaryDirectory, "build.txt")

            override val StudioWrapper.composeIdePluginDirectory: File
                get() = File(binaryDirectory, "plugins/compose-ide-plugin")

            override val StudioWrapper.launchCommandArguments: List<String>
                get() {
                    val studioScript = File(binaryDirectory, "bin/studio.sh")
                    return listOf(
                        "sh",
                        studioScript.absolutePath,
                        projectRoot.absolutePath
                    )
                }

            override val StudioWrapper.libDirectory: File
                get() = File(binaryDirectory, "lib")

            override val StudioWrapper.licensePath: String
                get() = File(binaryDirectory, "LICENSE.txt").absolutePath

            override fun extractArchive(fromPath: String, toPath: String, execSpec: ExecSpec) {
                with(execSpec) {
                    executable("tar")
                    args("-xf", fromPath, "-C", toPath)
                }
            }

            override fun StudioWrapper.updateJvmHeapSize() {
                val vmoptions =
                    File(binaryDirectory, "bin/studio.vmoptions")
                val newText = vmoptions.readText().replace(jvmHeapRegex, "-Xmx4g")
                vmoptions.writeText(newText)

                val vmoptions64 =
                    File(binaryDirectory, "bin/studio64.vmoptions")
                val newText64 = vmoptions64.readText().replace(jvmHeapRegex, "-Xmx8g")
                vmoptions64.writeText(newText64)
            }
        }
    }
}

open class RootStudioWrapper(project: Project) : StudioWrapper(project) {
    /**
     * Url to download the correct version of Studio from.
     */
    private val studioUrl by lazy {
        "https://dl.google.com/dl/android/studio/ide-zips/$studioVersion/$studioArchiveName"
    }

    override fun createStudioArchiveFile() {
        val tmpDownloadPath = File("$studioArchivePath.tmp").absolutePath

        println("Downloading $studioUrl to $tmpDownloadPath")
        project.exec { execSpec ->
            with(execSpec) {
                executable("curl")
                args(studioUrl, "--output", tmpDownloadPath)
            }
        }

        // Renames temp archive to the final archive name
        Files.move(Paths.get(tmpDownloadPath), Paths.get(studioArchivePath))
    }

    override val studioVersionsFile by lazy {
        File(projectRoot, "buildSrc/studio_versions.properties")
    }

    override val ideaProperties by lazy {
        File(projectRoot, "development/studio/idea.properties")
    }
}

open class UiStudioWrapper(project: Project) : StudioWrapper(project) {
    /**
     * Location of the prebuilt archive that we use.
     */
    private val studioPrebuiltArchive by lazy {
        val prebuiltsDir = SupportConfig.getPrebuiltsRootPath(project)
        File("$prebuiltsDir/androidx/studio/$studioArchiveName")
    }

    /**
     * Custom build version we set in build.txt as the Intellij gradle plugin doesn't like the
     * dashes in the normal build.txt that comes from the prebuilts.
     */
    private val buildVersion: String
        get() = "AI-$ideaMajorVersion.$ideaMinorVersion.$studioBuildNumber"

    override fun createStudioArchiveFile() {
        // Copy archive from prebuilts to the parent directory of the install directory
        println("Copying prebuilt studio archive to $studioArchivePath")
        studioPrebuiltArchive.copyTo(File(studioArchivePath))
    }

    // TODO: b/142859295 re-enable IDE plugin when we fix circular dependency
    /*override fun preLaunchConfiguration() {
        // Copy the built compose plugin into the studio plugin directory every time to ensure it
        // is up to date
        val builtComposePluginDirectory = File(
            project.getRootOutDirectory(),
            "ui/compose/compose-ide-plugin/build/idea-sandbox/plugins/compose-ide-plugin"
        )
        println("Copying Compose IDE plugin to Studio directory")
        with(platformUtilities) {
            // Ensure the directory exists
            composeIdePluginDirectory.deleteRecursively()
            composeIdePluginDirectory.mkdirs()
            builtComposePluginDirectory.copyRecursively(
                target = composeIdePluginDirectory,
                overwrite = true
            )
        }
    }*/

    // TODO: temporarily deleting the directory if it exists for existing installations,
    //  until b/142859295 is fixed
    override fun preLaunchConfiguration() {
        with(platformUtilities) {
            composeIdePluginDirectory.deleteRecursively()
        }
    }

    override fun postInstallConfiguration() {
        // Sometimes the build number put in here by Studio is incompatible with the
        // Intellij gradle plugin, so we overwrite it with one that we know it will accept.
        with(platformUtilities) {
            buildTxt.createNewFile()
            buildTxt.writeText(buildVersion)
        }
    }

    override val studioVersionsFile by lazy {
        File(projectRoot, "studio_versions.properties")
    }

    override val ideaProperties by lazy {
        File(projectRoot, "idea.properties")
    }
}

private val isUiProject get() = System.getProperty("DIST_SUBDIR") == "/ui"
