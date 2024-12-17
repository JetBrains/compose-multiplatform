/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.internal.publishing.utils.SpaceApiClient
import org.jetbrains.compose.internal.publishing.utils.SpaceApiClient.PackageInfo
import space.jetbrains.api.runtime.types.PackageRepositoryIdentifier
import space.jetbrains.api.runtime.types.ProjectIdentifier
import java.util.regex.Pattern

abstract class FindModulesInSpaceTask : DefaultTask() {
    @get:Input
    abstract val requestedCoordinates: Property<String>

    @get:Input
    abstract val spaceInstanceUrl: Property<String>

    @get:Internal
    abstract val spaceClientId: Property<String>

    @get:Internal
    abstract val spaceClientSecret: Property<String>

    @get:Input
    abstract val spaceProjectId: Property<String>

    @get:Input
    abstract val spaceRepoId: Property<String>

    @get:OutputFile
    abstract val modulesTxtFile: RegularFileProperty

    @TaskAction
    fun run() {
        val space = SpaceApiClient(
            serverUrl = spaceInstanceUrl.get(),
            clientId = spaceClientId.get(),
            clientSecret = spaceClientSecret.get()
        )

        val projectId = ProjectIdentifier.Id(spaceProjectId.get())
        val repoId = PackageRepositoryIdentifier.Id(spaceRepoId.get())
        val modules = ArrayList<String>()
        val requestedCoordinates = requestedCoordinates.get().split(",")
        requestedCoordinates.forEach { req ->
            val version = req.substringAfterLast(":") // suppose we don't support wildcards in version
            space.forEachPackageWithVersion(projectId, repoId, version) { pkg ->
                val pkgStr = pkg.toString()
                if (pkgStr.matchesWildcard(req)) {
                    modules.add(pkgStr)
                }
            }
        }

        modulesTxtFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(modules.joinToString("\n"))
        }
    }
}

private fun String.matchesWildcard(pattern: String): Boolean = "\\Q$pattern\\E"
    .replace("*", "\\E.*\\Q")
    .toRegex()
    .matches(this)
