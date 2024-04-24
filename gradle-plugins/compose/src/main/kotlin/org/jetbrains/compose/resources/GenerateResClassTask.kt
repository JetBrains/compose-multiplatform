package org.jetbrains.compose.resources

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * This task should be FAST and SAFE! Because it is being run during IDE import.
 */
internal abstract class IdeaImportTask : DefaultTask() {
    @get:Input
    val ideaIsInSync: Provider<Boolean> = project.provider {
        System.getProperty("idea.sync.active", "false").toBoolean()
    }

    @TaskAction
    fun run() {
        try {
            safeAction()
        } catch (e: Exception) {
            //message must contain two ':' symbols to be parsed by IDE UI!
            logger.error("e: $name task was failed:", e)
            if (!ideaIsInSync.get()) throw e
        }
    }

    abstract fun safeAction()
}

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
    abstract val shouldGenerateCode: Property<Boolean>

    @get:Input
    abstract val makeAccessorsPublic: Property<Boolean>

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

    override fun safeAction() {
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
    }
}
