/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.internal.publishing.utils.*

@Suppress("unused") // public api
abstract class UploadToSonatypeTask : DefaultTask() {
    // the task must always re-run anyway, so all inputs can be declared Internal
    @get:Internal
    abstract val sonatypeServer: Property<String>

    @get:Internal
    abstract val user: Property<String>

    @get:Internal
    abstract val password: Property<String>

    @get:Internal
    abstract val stagingProfileName: Property<String>

    @get:Internal
    abstract val autoCommitOnSuccess: Property<Boolean>

    @get:Internal
    abstract val version: Property<String>

    @get:Internal
    abstract val modulesToUpload: ListProperty<ModuleToUpload>

    @TaskAction
    fun run() {
        SonatypeRestApiClient(
            sonatypeServer = sonatypeServer.get(),
            user = user.get(),
            password = password.get(),
            logger = logger
        ).use { client -> run(client) }
    }

    private fun run(sonatype: SonatypeApi) {
        val stagingProfiles = sonatype.stagingProfiles()
        val stagingProfileName = stagingProfileName.get()
        val stagingProfile = stagingProfiles.data.firstOrNull { it.name == stagingProfileName }
            ?: error(
                "Cannot find staging profile '$stagingProfileName' among existing staging profiles: " +
                        stagingProfiles.data.joinToString { "'${it.name}'" }
            )
        val modules = modulesToUpload.get()

        validate(stagingProfile, modules)

        val stagingRepo = sonatype.createStagingRepo(
            stagingProfile, "Staging repo for '${stagingProfile.name}' release '${version.get()}'"
        )
        try {
            for (module in modules) {
                sonatype.upload(stagingRepo, module)
            }
            if (autoCommitOnSuccess.get()) {
                sonatype.closeStagingRepo(stagingRepo)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun validate(stagingProfile: StagingProfile, modules: List<ModuleToUpload>) {
        val validationIssues = arrayListOf<Pair<ModuleToUpload, ModuleValidator.Status.Error>>()
        for (module in modules) {
            val status = ModuleValidator(stagingProfile, module, version.get()).validate()
            if (status is ModuleValidator.Status.Error) {
                validationIssues.add(module to status)
            }
        }
        if (validationIssues.isNotEmpty()) {
            val message = buildString {
                appendLine("Some modules violate Maven Central requirements:")
                for ((module, status) in validationIssues) {
                    appendLine("* ${module.coordinate} (files: ${module.localDir})")
                    for (error in status.errors) {
                        appendLine("  * $error")
                    }
                }
            }
            error(message)
        }
    }
}