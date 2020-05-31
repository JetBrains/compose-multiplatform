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

import androidx.build.StudioType
import androidx.build.getSupportRootFolder
import androidx.build.studioType
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.service.ServiceRegistry
import java.io.File

/**
 * Base task with common logic for updating and launching studio in both the frameworks/support
 * project and the frameworks/support/ui project. Project-specific configuration is provided by
 * [RootStudioTask] and [ComposeStudioTask].
 */
abstract class StudioTask : DefaultTask() {

    // TODO: support -y and --update-only options? Can use @Option for this
    @TaskAction
    fun studiow() {
        update()
        launch()
    }

    private val platformUtilities by lazy {
        StudioPlatformUtilities.get(projectRoot, studioInstallationDir)
    }

    @get:Internal
    protected val projectRoot: File = project.rootDir

    @get:Internal
    protected open val installParentDir: File = project.rootDir

    private val studioVersions by lazy { StudioVersions.get() }

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
    private val studioArchiveName: String
        get() = studioDirectoryName + platformUtilities.archiveExtension

    /**
     * The install directory containing Studio
     *
     * Note: Given that the contents of this directory changes a lot, we don't want to annotate this
     * property for task avoidance - it's not stable enough for us to get any value out of this.
     */
    private val studioInstallationDir by lazy {
        File(installParentDir, "studio/$studioDirectoryName")
    }

    /**
     * Absolute path of the Studio archive
     */
    private val studioArchivePath: String by lazy {
        File(studioInstallationDir.parentFile, studioArchiveName).absolutePath
    }

    /**
     * The idea.properties file that we want to tell Studio to use
     */
    @get:Internal
    protected abstract val ideaProperties: File

    /**
     * [StudioArchiveCreator] that will ensure that an archive is present at [studioArchivePath]
     */
    @get:Internal
    protected abstract val studioArchiveCreator: StudioArchiveCreator

    /**
     * Updates the Studio installation and removes any old installation files if they exist.
     */
    private fun update() {
        if (!studioInstallationDir.exists()) {
            // Create installation directory and any needed parent directories
            studioInstallationDir.mkdirs()
            // Attempt to remove any old installations in the parent studio/ folder
            removeOldInstallations()
            studioArchiveCreator(project, studioVersions, studioArchiveName, studioArchivePath)
            println("Extracting archive...")
            extractStudioArchive()
            with(platformUtilities) { updateJvmHeapSize() }
        }
    }

    /**
     * Launches Studio if the user accepts / has accepted the license agreement.
     */
    private fun launch() {
        if (checkLicenseAgreement(services)) {
            println("Launching studio...")
            launchStudio()
        } else {
            println("Exiting without launching studio...")
        }
    }

    private fun launchStudio() {
        val vmOptions = File(project.getSupportRootFolder(), "development/studio/studio.vmoptions")

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
        private const val STUDIO_TASK = "studio"

        fun Project.registerStudioTask() {
            val studioTask = when (studioType()) {
                StudioType.ANDROIDX -> RootStudioTask::class.java
                StudioType.COMPOSE -> ComposeStudioTask::class.java
                StudioType.PLAYGROUND -> PlaygroundStudioTask::class.java
            }
            tasks.register(STUDIO_TASK, studioTask)
        }
    }
}

/**
 * Task for launching studio in the frameworks/support project
 */
open class RootStudioTask : StudioTask() {
    override val studioArchiveCreator = UrlArchiveCreator
    override val ideaProperties get() = projectRoot.resolve("development/studio/idea.properties")
}

/**
 * Task for launching studio in the frameworks/support/ui (Compose) project
 */
open class ComposeStudioTask : StudioTask() {
    override val studioArchiveCreator = UrlArchiveCreator
    override val ideaProperties get() = projectRoot.resolve("idea.properties")
}

/**
 * Task for launching studio in a playground project
 */
open class PlaygroundStudioTask : RootStudioTask() {
    override val installParentDir get() = project.rootProject.projectDir.resolve("..")
}
