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
import com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.service.ServiceRegistry
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Base task with common logic for updating and launching studio in both the frameworks/support
 * project and playground projects. Project-specific configuration is provided by
 * [RootStudioTask] and [PlaygroundStudioTask].
 */
abstract class StudioTask : DefaultTask() {

    // TODO: support -y and --update-only options? Can use @Option for this
    @TaskAction
    fun studiow() {
        install()
        launch()
    }

    private val platformUtilities by lazy {
        StudioPlatformUtilities.get(projectRoot, studioInstallationDir)
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    /**
     * If `true`, checks for `ANDROIDX_PROJECTS` environment variable to decide which
     * projects need to be loaded.
     */
    @get:Internal
    protected open val requiresProjectList: Boolean = true

    @get:Internal
    protected val projectRoot: File = project.rootDir

    @get:Internal
    protected open val installParentDir: File = project.rootDir

    @Suppress("UnstableApiUsage") // For use of VersionCatalog
    private val studioVersion by lazy {
        val libs = project.extensions.getByType(
            VersionCatalogsExtension::class.java
        ).find("libs").get()
        fun getVersion(key: String): String {
            val version = libs.findVersion(key)
            return if (version.isPresent) {
                version.get().requiredVersion
            } else {
                throw GradleException("Could not find a version for `$key`")
            }
        }
        getVersion("androidStudio")
    }

    /**
     * Directory name (not path) that Studio will be unzipped into.
     */
    private val studioDirectoryName: String
        get() {
            val osName = StudioPlatformUtilities.osName
            return "android-studio-$studioVersion-$osName"
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
     * The studio.vmoptions file that we want to start Studio with
     */
    @get:Internal
    open val vmOptions = File(project.getSupportRootFolder(), "development/studio/studio.vmoptions")

    /**
     * [StudioArchiveCreator] that will ensure that an archive is present at [studioArchivePath]
     */
    @get:Internal
    protected abstract val studioArchiveCreator: StudioArchiveCreator

    /**
     * List of additional environment variables to pass into the Studio application.
     */
    @get:Internal
    open val additionalEnvironmentProperties: Map<String, String> = emptyMap()

    private val licenseAcceptedFile: File by lazy {
        File("$studioInstallationDir/STUDIOW_LICENSE_ACCEPTED")
    }

    /**
     * Install Studio and removes any old installation files if they exist.
     */
    private fun install() {
        val successfulInstallFile = File("$studioInstallationDir/INSTALL_SUCCESSFUL")
        if (!licenseAcceptedFile.exists() && !successfulInstallFile.exists()) {
            // Attempt to remove any old installations in the parent studio/ folder
            studioInstallationDir.parentFile.deleteRecursively()
            // Create installation directory and any needed parent directories
            studioInstallationDir.mkdirs()
            studioArchiveCreator(
                execOperations,
                studioVersion,
                studioArchiveName,
                studioArchivePath
            )
            println("Extracting archive...")
            extractStudioArchive()
            with(platformUtilities) { updateJvmHeapSize() }
            // Finish install process
            successfulInstallFile.createNewFile()
        }
    }

    /**
     * Launches Studio if the user accepts / has accepted the license agreement.
     */
    private fun launch() {
        if (checkLicenseAgreement(services)) {
            if (requiresProjectList && !System.getenv().containsKey("ANDROIDX_PROJECTS")) {
                throw GradleException(
                    """
                    Please specify which set of projects you'd like to open in studio
                    with ANDROIDX_PROJECTS=MAIN ./gradlew studio

                    For possible options see settings.gradle
                    """.trimIndent()
                )
            }
            println("Launching studio...")
            launchStudio()
        } else {
            println("Exiting without launching studio...")
        }
    }

    private fun launchStudio() {
        ProcessBuilder().apply {
            inheritIO()
            with(platformUtilities) { command(launchCommandArguments) }

            val additionalStudioEnvironmentProperties = mapOf(
                // These environment variables are used to set up AndroidX's default configuration.
                "STUDIO_PROPERTIES" to ideaProperties.absolutePath,
                "STUDIO_VM_OPTIONS" to vmOptions.absolutePath,
                // This environment variable prevents Studio from showing IDE inspection warnings
                // for nullability issues, if the context is deprecated. This environment variable
                // is consumed by InteroperabilityDetector.kt
                "ANDROID_LINT_NULLNESS_IGNORE_DEPRECATED" to "true",
                // This environment variable is read by AndroidXRootImplPlugin to ensure that
                // Studio-initiated Gradle tasks are run against the same version of AGP that was
                // used to start Studio, which prevents version mismatch after repo sync.
                "EXPECTED_AGP_VERSION" to ANDROID_GRADLE_PLUGIN_VERSION
            ) + additionalEnvironmentProperties

            // Append to the existing environment variables set by gradlew and the user.
            environment().putAll(additionalStudioEnvironmentProperties)
            start()
        }
    }

    private fun checkLicenseAgreement(services: ServiceRegistry): Boolean {
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
        execOperations.exec {
                execSpec -> platformUtilities.extractArchive(fromPath, toPath, execSpec)
        }
        // Remove studio archive once done
        File(studioArchivePath).delete()
    }

    companion object {
        private const val STUDIO_TASK = "studio"

        fun Project.registerStudioTask() {
            val studioTask = when (studioType()) {
                StudioType.ANDROIDX -> RootStudioTask::class.java
                StudioType.PLAYGROUND -> PlaygroundStudioTask::class.java
            }
            tasks.register(STUDIO_TASK, studioTask)
        }
    }
}

/**
 * Task for launching studio in the frameworks/support project
 */
abstract class RootStudioTask : StudioTask() {
    override val studioArchiveCreator = UrlArchiveCreator
    override val ideaProperties get() = projectRoot.resolve("development/studio/idea.properties")
}

/**
 * Task for launching studio in a playground project
 */
abstract class PlaygroundStudioTask : RootStudioTask() {
    @get:Internal
    val supportRootFolder = (project.rootProject.property("ext") as ExtraPropertiesExtension)
        .let { it.get("supportRootFolder") as File }

    /**
     * Playground projects have only 1 setup so there is no need to specify the project list.
     */
    override val requiresProjectList get() = false
    override val installParentDir get() = supportRootFolder
    override val additionalEnvironmentProperties: Map<String, String>
        get() = mapOf("ALLOW_PUBLIC_REPOS" to "true")
    override val ideaProperties
        get() = supportRootFolder.resolve("../playground-common/idea.properties")
    override val vmOptions
        get() = supportRootFolder.resolve("../playground-common/studio.vmoptions")
}
