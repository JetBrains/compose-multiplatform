package org.jetbrains.compose.resources

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File

/**
 * This task should be FAST and SAFE! Because it is being run during IDE import.
 */
internal abstract class CodeGenerationTask : DefaultTask()

internal abstract class GenerateResClassTask : CodeGenerationTask() {
    companion object {
        private const val RES_FILE_NAME = "Res"
    }

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    abstract val packagingDir: Property<File>

    @get:Input
    abstract val shouldGenerateCode: Property<Boolean>

    @get:Input
    abstract val makeAccessorsPublic: Property<Boolean>

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

    @TaskAction
    fun generate() {
        try {
            val dir = codeDir.get().asFile
            dir.deleteRecursively()
            dir.mkdirs()

            if (shouldGenerateCode.get()) {
                logger.info("Generate $RES_FILE_NAME.kt")

                val pkgName = packageName.get()
                val moduleDirectory = packagingDir.getOrNull()?.let { it.invariantSeparatorsPath + "/" } ?: ""
                val isPublic = makeAccessorsPublic.get()
                getResFileSpec(pkgName, RES_FILE_NAME, moduleDirectory, isPublic).writeTo(dir)
            } else {
                logger.info("Generation Res class is disabled")
            }
        } catch (e: Exception) {
            //message must contain two ':' symbols to be parsed by IDE UI!
            logger.error("e: $name task was failed:", e)
        }
    }
}
