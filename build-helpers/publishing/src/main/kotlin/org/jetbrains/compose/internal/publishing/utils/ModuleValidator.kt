/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing.utils

import com.fasterxml.jackson.annotation.JsonRootName
import org.jetbrains.compose.internal.publishing.ModuleToUpload
import java.io.File

internal class ModuleValidator(
    private val stagingProfile: StagingProfile,
    private val module: ModuleToUpload,
    private val version: String
) {
    private val errors = arrayListOf<String>()
    private var status: Status? = null

    sealed class Status {
        object OK : Status()
        class Error(val errors: List<String>) : Status()
    }

    fun validate(): Status {
        if (status == null) {
            validateImpl()
            status = if (errors.isEmpty()) Status.OK
                     else Status.Error(errors)
        }

        return status!!
    }

    private fun validateImpl() {
        if (!module.groupId.startsWith(stagingProfile.name)) {
            errors.add("Module's group id '${module.groupId}' does not match staging repo '${stagingProfile.name}'")
        }

        if (module.version != version) {
            errors.add("Unexpected version '${module.version}' (expected: '$version')")
        }

        val pomFile = artifactFile(extension = "pom")
        val pom = when {
            pomFile.exists() ->
                try {
                    // todo: validate POM
                    Xml.deserialize<Pom>(pomFile.readText())
                } catch (e: Exception) {
                    errors.add("Cannot deserialize $pomFile: $e")
                    null
                }
            else -> null
        }

        val mandatoryFiles = arrayListOf(pomFile)
        if (pom != null && pom.packaging != "pom") {
            mandatoryFiles.add(artifactFile(extension = pom.packaging ?: "jar"))
            mandatoryFiles.add(artifactFile(extension = "jar", classifier = "sources"))
            mandatoryFiles.add(artifactFile(extension = "jar", classifier = "javadoc"))
        }

        val nonExistingFiles = mandatoryFiles.filter { !it.exists() }
        if (nonExistingFiles.isNotEmpty()) {
            errors.add("Some necessary files do not exist: [${nonExistingFiles.map { it.name }.joinToString()}]")
        }

        // signatures and checksums should not be signed themselves
        val skipSignatureCheckExtensions = setOf("asc", "md5", "sha1", "sha256", "sha512")
        val unsignedFiles = module.listFiles()
            .filter {
                it.extension !in skipSignatureCheckExtensions && !it.resolveSibling(it.name + ".asc").exists()
            }
        if (unsignedFiles.isNotEmpty()) {
            errors.add("Some files are not signed: [${unsignedFiles.map { it.name }.joinToString()}]")
        }
    }

    private fun artifactFile(extension: String, classifier: String? = null): File {
        val fileName = buildString {
            append("${module.artifactId}-${module.version}")
            if (classifier != null)
                append("-$classifier")
            append(".$extension")
        }
        return module.localDir.resolve(fileName)
    }
}


@JsonRootName("project")
private data class Pom(
    var groupId: String? = null,
    var artifactId: String? = null,
    var packaging: String? = null,
    var name: String? = null,
    var description: String? = null,
    var url: String? = null,
    var scm: Scm? = null,
    var licenses: List<License>? = null,
    var developers: List<Developer>? = null,
) {
    internal data class Scm(
        var connection: String?,
        var developerConnection: String?,
        var url: String?,
    )

    internal data class License(
        var name: String? = null,
        var url: String? = null
    )

    internal data class Developer(
        var name: String? = null,
        var organization: String? = null,
        var organizationUrl: String? = null
    )
}
