/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing

import org.jetbrains.compose.internal.publishing.utils.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.file.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.signatory.Signatory
import org.gradle.plugins.signing.type.pgp.ArmoredSignatureType
import java.io.File
import java.util.jar.JarOutputStream

@Suppress("unused") // public api
abstract class FixModulesBeforePublishingTask : DefaultTask() {
    @get:InputFiles
    abstract val inputRepoDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputRepoDir: DirectoryProperty

    @get:Nested
    val signatory: Signatory
        get() = project.extensions.getByType(SigningExtension::class.java).signatory

    private val checksums: Checksum = defaultChecksums()

    @TaskAction
    fun run() {
        val inputDir = inputRepoDir.get().asFile
        val outputDir = outputRepoDir.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }

        for (inputFile in inputDir.walk()) {
            if (inputFile.isDirectory
                || checksums.isChecksumFile(inputFile)
                || inputFile.name.endsWith(".asc")
            ) continue

            val outputFile = outputDir.resolve(inputFile.relativeTo(inputDir).path)
            outputFile.parentFile.mkdirs()

            logger.info("Copying and processing $inputFile to $outputFile")
            if (inputFile.name.endsWith(".pom", ignoreCase = true)) {
                val pom = PomDocument(inputFile)
                fixPomIfNeeded(pom)
                pom.saveTo(outputFile)
                if (pom.packaging != "pom") {
                    fixSourcesAndJavadocJarIfNeeded(
                        inputDir = inputFile.parentFile,
                        outputDir = outputFile.parentFile,
                        baseName = inputFile.nameWithoutExtension
                    )
                }
            } else {
                inputFile.copyTo(outputFile)
            }
        }

        for (outputFile in outputDir.walk().filter { it.isFile }) {
            // todo: make parallel
            val signatureFile = outputFile.generateSignature()
            checksums.generateChecksumFilesFor(outputFile)
            checksums.generateChecksumFilesFor(signatureFile)
        }
    }

    private fun fixPomIfNeeded(pom: PomDocument) {
        pom.fillMissingTags(
            projectUrl = "https://github.com/JetBrains/compose-jb",
            projectInceptionYear = "2020",
            licenseName = "The Apache Software License, Version 2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt",
            licenseDistribution = "repo",
            scmConnection = "scm:git:https://github.com/JetBrains/compose-jb.git",
            scmDeveloperConnection = "scm:git:https://github.com/JetBrains/compose-jb.git",
            scmUrl = "https://github.com/JetBrains/compose-jb",
            developerName = "Compose Multiplatform Team",
            developerOrganization = "JetBrains",
            developerOrganizationUrl = "https://www.jetbrains.com",
        )
    }

    private fun fixSourcesAndJavadocJarIfNeeded(inputDir: File, outputDir: File, baseName: String) {
        val srcJar = inputDir.resolve("$baseName-sources.jar")
        if (!srcJar.exists()) {
            logger.warn("$srcJar does not exist. Generating empty stub")
            outputDir.resolve(srcJar.name).generateEmptyJar()
        }
        val javadocJar = inputDir.resolve("$baseName-javadoc.jar")
        if (!javadocJar.exists()) {
            logger.warn("$javadocJar does not exist. Generating empty stub")
            outputDir.resolve(javadocJar.name).generateEmptyJar()
        }
    }

    private fun File.generateEmptyJar(): File =
        apply {
            JarOutputStream(this.outputStream().buffered()).use { }
        }

    private fun File.generateSignature(): File {
        return ArmoredSignatureType().sign(signatory, this)
    }
}
