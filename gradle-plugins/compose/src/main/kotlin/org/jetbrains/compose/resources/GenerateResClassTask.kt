package org.jetbrains.compose.resources

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.jetbrains.compose.internal.IdeaImportTask
import java.io.File

internal abstract class GenerateResClassTask : IdeaImportTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val resClassName: Property<String>

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

        val resClassName = resClassName.get()
        logger.info("Generate $resClassName.kt")

        val pkgName = packageName.get()
        val moduleDirectory = packagingDir.getOrNull()?.let { it.invariantSeparatorsPath + "/" } ?: ""
        val isPublic = makeAccessorsPublic.get()
        getResFileSpec(pkgName, resClassName, moduleDirectory, isPublic).writeTo(dir)
    }
}
