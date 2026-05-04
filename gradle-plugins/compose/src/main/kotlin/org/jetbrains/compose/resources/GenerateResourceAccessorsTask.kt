package org.jetbrains.compose.resources

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.compose.internal.IdeaImportTask
import java.io.File

@DisableCachingByDefault(because = "IDE import task — not worth caching")
internal abstract class GenerateResourceAccessorsTask : IdeaImportTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val resClassName: Property<String>

    @get:Input
    abstract val sourceSetName: Property<String>

    @get:Input
    @get:Optional
    abstract val packagingDir: Property<File>

    @get:Input
    abstract val makeAccessorsPublic: Property<Boolean>

    @get:Input
    abstract val disableResourceContentHashGeneration: Property<Boolean>

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resDir: Property<File>

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

    override fun safeAction() {
        val kotlinDir = codeDir.get().asFile
        val rootResDir = resDir.get()
        val sourceSet = sourceSetName.get()

        logger.info("Clean directory $kotlinDir")
        kotlinDir.deleteRecursively()
        kotlinDir.mkdirs()

        logger.info("Generate accessors for $rootResDir")

        val resources = ResourceHolder(rootResDir)
        val pkgName = packageName.get()
        val moduleDirectory = packagingDir.getOrNull()?.let { it.invariantSeparatorsPath + "/" } ?: ""
        val resClassName = resClassName.get()
        val isPublic = makeAccessorsPublic.get()
        val generateResourceContentHashAnnotation = !disableResourceContentHashGeneration.get()
        getAccessorsSpecs(
            resources,
            pkgName,
            sourceSet,
            moduleDirectory,
            resClassName,
            isPublic,
            generateResourceContentHashAnnotation
        ).forEach { it.writeTo(kotlinDir) }
    }
}

internal fun File.listNotHiddenFiles(): List<File> =
    listFiles()?.filter { !it.isHidden }.orEmpty()

internal fun String.asUnderscoredIdentifier(): String =
    replace('-', '_')
        .let { if (it.isNotEmpty() && it.first().isDigit()) "_$it" else it }
