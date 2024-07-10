package org.jetbrains.compose.resources

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.jetbrains.compose.internal.IdeaImportTask
import java.io.File

internal abstract class GenerateResClassTask : IdeaImportTask() {
    companion object {
        private const val RES_FILE_NAME = "Res"
    }

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    abstract val packagingDir: Property<File>

    @get:Input
    abstract val makeAccessorsPublic: Property<Boolean>

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

    override fun safeAction() {
        val dir = codeDir.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()

        logger.info("Generate $RES_FILE_NAME.kt")

        val pkgName = packageName.get()
        val moduleDirectory = packagingDir.getOrNull()?.let { it.invariantSeparatorsPath + "/" } ?: ""
        val isPublic = makeAccessorsPublic.get()
        getResFileSpec(pkgName, RES_FILE_NAME, moduleDirectory, isPublic).writeTo(dir)
    }
}
