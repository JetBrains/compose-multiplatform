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

import androidx.build.SupportConfig
import androidx.build.getRootOutDirectory
import org.gradle.api.Project
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.internal.service.ServiceRegistry
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Base class with common logic for updating and launching studio in both the frameworks/support
 * project and the frameworks/support/ui project. Project-specific configuration is provided by
 * [RootStudioWrapper] and [UiStudioWrapper].
 *
 * @param project the root project that Studio should be launched for
 */
abstract class StudioWrapper(val project: Project) {

    // TODO: compose-compiler-plugin relies on logic in this class to set up the environment for its
    // Intellij plugin. If that is changed then this class can be made to extend DefaultTask
    // like a normal task.

    protected val platformUtilities by lazy {
        StudioPlatformUtilities.get(projectRoot, studioInstallationDir)
    }

    protected val projectRoot: File = project.rootDir

    protected val studioVersions by lazy { StudioVersions.get() }

    /**
     * Directory name (not path) that Studio will be unzipped into.
     */
    private val studioDirectoryName: String
        get() {
            val osName = StudioPlatformUtilities.osName
            with(studioVersions) {
                return "android-studio-ide-$ideaMajorVersion.$studioBuildNumber-$osName"
            }
        }

    /**
     * Filename (not path) of the Studio archive
     */
    protected val studioArchiveName: String
        get() = studioDirectoryName + platformUtilities.archiveExtension

    /**
     * The install directory containing Studio
     */
    private val studioInstallationDir by lazy { File(projectRoot, "studio/$studioDirectoryName") }

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
        fun create(project: Project) = if (SupportConfig.isUiProject()) {
            UiStudioWrapper(project)
        } else {
            RootStudioWrapper(project)
        }
    }
}

open class RootStudioWrapper(project: Project) : StudioWrapper(project) {
    /**
     * Url to download the correct version of Studio from.
     */
    private val studioUrl by lazy {
        with(studioVersions) {
            "https://dl.google.com/dl/android/studio/ide-zips/$studioVersion/$studioArchiveName"
        }
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

    override fun createStudioArchiveFile() {
        // Copy archive from prebuilts to the parent directory of the install directory
        println("Copying prebuilt studio archive to $studioArchivePath")
        studioPrebuiltArchive.copyTo(File(studioArchivePath))
    }

    override fun preLaunchConfiguration() {
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
    }

    override fun postInstallConfiguration() {
        // Sometimes the build number put in here by Studio is incompatible with the
        // Intellij gradle plugin, so we overwrite it with one that we know it will accept.
        with(platformUtilities) {
            buildTxt.createNewFile()
            buildTxt.writeText(studioVersions.buildTxtOverride)
        }
    }

    override val ideaProperties by lazy {
        File(projectRoot, "idea.properties")
    }
}
