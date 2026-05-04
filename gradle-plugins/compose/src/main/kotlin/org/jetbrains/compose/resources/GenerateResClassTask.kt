package org.jetbrains.compose.resources

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.compose.internal.IdeaImportTask
import java.io.File
import kotlin.io.path.name

@DisableCachingByDefault(because = "IDE import task — not worth caching")
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

    @get:InputFiles
    abstract val preparedResources: DirectoryProperty

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
        val resourceDirectories = getAllDirectories(preparedResources.asFileTree)
        val rootDir = preparedResources.get().asFile
        val typeToSubdirPaths = collectSubDirPathsForResourceTypes(resourceDirectories, rootDir)
        getResFileSpec(pkgName, resClassName, moduleDirectory, isPublic, typeToSubdirPaths).writeTo(dir)
    }

    private fun getAllDirectories(fileTree: FileTree): Set<File> {
        val dirs = mutableSetOf<File>()
        fileTree.visit { fileDetails ->
            if (fileDetails.isDirectory) {
                dirs.add(fileDetails.file)
            }
        }
        return dirs
    }

    // For each top-level resource type, return all subdirectories as a split
    // relative path from their resource directory.
    // E.g. `/drawable/sub/sub2` is returned as `["sub", "sub2"]`.
    private fun collectSubDirPathsForResourceTypes(
        dirs: Set<File>,
        root: File
    ): Map<ResourceType, Set<List<String>>> {
        val result = mutableMapOf<ResourceType, MutableSet<List<String>>>()
        dirs.forEach { dir ->
            val relativePath = dir.relativeTo(root).toPath()
            val typeName = relativePath.getName(0).name.split("-").first()
            val type = ResourceType.fromString(typeName) ?: return@forEach // Ignore any resource folders we cannot create accessors for
            if (relativePath.nameCount > 1) {
                val subDirPath = relativePath.subpath(1, relativePath.nameCount).map { it.toString() }
                result.getOrPut(type) { mutableSetOf() }.add(subDirPath)
            } else {
                result.getOrPut(type) { mutableSetOf() }
            }
        }
        return result
    }
}
