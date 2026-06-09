package org.jetbrains.compose.resources

import org.gradle.api.GradleException
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.internal.utils.gradleError
import java.io.File
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo

internal typealias Resources = Map<ResourceType, Map<String, List<ResourceItem>>>

/**
 * Class containing all [ResourceItem] discovered under `composeResources`. This
 * forms the basis for auto-generating accessors to these resources. See
 * [GenerateResourceAccessorsTask] for more details.
 *
 * It will throw a [GradleException] on creation if any resource file or
 * directory is invalid.
 *
 * A number of restrictions are put on the structure and naming of these
 * files and directories. Some are based on filesystem limitations across
 * the supported platforms, while others are functional limitations.
 *
 * All file and directory comparisons are case-insensitive, while identifiers
 * in `/values/` are case-sensitive.
 *
 * For resource directories and any subfolders the following rules apply:
 *
 * - `/values/` cannot contain subfolders.
 * - `/values/` can only contain `.xml` files that defines properties. It is an
 *   error if the same property key is defined in multiple files.
 * - Subfolders cannot share a name with a file (without extension) on the same
 *   level. This restriction applies across qualified folders, e.g.
 *   `/drawable/subdir/` and `/drawable-dark/subdir.png` is not allowed.
 * - Subfolder names must not contain whitespace, `.` or `-` (or other symbols).
 * - Subfolder names must not start with a digit, e.g. `/drawable/1MyDir`.
 * - If not breaking other rules, subfolders allow non-ascii characters like
 *   danish `/drawable/æøå` or Unicode like arabic `/drawable/كوتلن`.
 * - `/files/` is a "free" folder. The user can dump anything in here. We do not
 *   impose any restrictions on this folder.
 *
 * For resource files the following rules apply:
 *
 * - Whitespace or `.` (or any symbols except `-`) are not allowed in file names.
 * - Filenames (without their extension) must be unique in a directory.
 *
 * Compose Resources generate accessors for all resources not in `/files/`. This
 * conversion is not direct as for folders but does support a few cases:
 *
 * - Filenames starting with a digit gets a `_` suffix, e.g. `_1MyFile`.
 * - Filenames with a `-` will replace it with `_`, e.g. `my-file.png` is turned
 *   into `my_file`.
 *
 * The above are just the most common errors, which are all checked by the
 * ResourceHolder. To strike a balance between abuild speed and DX, other (more
 * niche) errors are left to the file system to report. Some examples:
 *
 * - Use of emoji in file or directory names.
 * - Filenames exceeding the max length on Windows (255 characters).
 *
 * Some other non-obvious functionality includes:
 *
 * - Resource keys do not need to map to the same file type across qualifiers.
 *   E.g. `/drawable/compose.png` and `/drawable-dark/COMPOSE.jpg` map to the
 *   same resource identifier.
 *
 * @param rootResDir Reference to the `composeResources` directory containing
 * procecessed resources. lll XML files containing resources have been converte
 * to `.cvr` files.
 * @param allowAccessorSubDirs If `true`, `/drawable/` and `/font` are allowed
 * to have subdirectories, which will be reflected in the generated resource
 * accessor classes.
 *
 * See [PrepareComposeResourcesTask].
 */
internal class ResourceHolder(rootResDir: File, private val allowAccessorSubDirs: Boolean = true) {

    val resources: Resources

    init {
        resources = validateAndGenerateResourceItems(rootResDir)
    }

    private fun validateAndGenerateResourceItems(rootResDir: File): Resources {
        // Get first level dirs
        val dirs = rootResDir.listNotHiddenFiles()
        dirs.forEach { f ->
            if (!f.isDirectory) {
                gradleError("${f.name} is not directory! Raw files should be placed in '${rootResDir.name}/files' directory.")
            }
        }

        // Group by resource type while being aware of qualifiers (identifiers must be unique within each type)
        // We sort all directories, so the base dir is always in the first position
        val typeGroups = dirs
            .groupBy {it.name.substringBefore('-').lowercase() }
            .mapValues { (_, variantDirs) -> variantDirs.sortedBy { it.name } }
            .toSortedMap()

        // Generat ResourceItem wrappers for all resource files while validating naming constraints in one pass.
        val allResources = mutableMapOf<ResourceType, Map<String, List<ResourceItem>>>()
        typeGroups.forEach { (typeName, variantDirs) ->
            when (typeName) {
                "drawable",
                "font" -> {
                    val type = ResourceType.fromString(typeName) ?: gradleError("Unsupported resource type: '$typeName'")
                    val resources = resolveStandardResources(type, variantDirs, rootResDir.toPath(), allowAccessorSubDirs)
                    allResources[type] = resources
                }
                "files" -> resolveFileResources(variantDirs)
                "values" -> {
                    resolveValuesResources(variantDirs, rootResDir.toPath()).forEach { (type, resources) ->
                        val previous = allResources.put(type, resources)
                        if (previous != null) {
                            // This check is correct as we handle types in sorted order and `values` are last.
                            error("`values` are not allowed to add resources to types already handled: $type")
                        }
                    }
                }
                "string" -> {
                    gradleError("Forbidden directory name '${variantDirs.first().name}'! String resources should be declared in 'values/strings.xml'.")
                }
                else -> {
                    gradleError("Unknown resource type: '${variantDirs.first().name}'.")
                }
            }
        }
        return allResources
    }

    // We do not create accessors for `/files`. Its content is entirely user-controlled.
    // The only restriction is not supporting qualifiers.
    private fun resolveFileResources(variantDirs: List<File>) {
        variantDirs.firstOrNull { it.name.contains("-") }?.let {
            gradleError("The 'files' directory doesn't support qualifiers: '${it.name}'.")
        }
    }

    // Resource keys defined in XML files in `/values` must be unique across all XML files.
    // While Android allows references to e.g. `drawable` keys in `values`. This is
    // currently disallowed in Compose Resources, and this method should only return
    // ResourceType's not seen before.
    private fun resolveValuesResources(variantDirs: List<File>, rootDir: Path): Map<ResourceType, Map<String, List<ResourceItem>>> {

        // XmlValuesConverterTask should have ensured that we only see `.cvr` files in these directories.
        // If not, something is wrong with the task configuration.
        fun assertValidCvrFile(file: File) {
            if (!file.isFile || !file.extension.equals(XmlValuesConverterTask.CONVERTED_RESOURCE_EXT, true)) {
                error("Unsupported file or directory found in 'values': ${file.absolutePath}")
            }
        }

        return variantDirs
            .flatMap { dir ->
                val valueKeys = mutableSetOf<String>()
                dir.listNotHiddenFiles()
                    .flatMap { file ->
                        assertValidCvrFile(file)
                        val typeAndQualifiers = dir.name.split("-")
                        val qualifiers = typeAndQualifiers.takeLast(typeAndQualifiers.size - 1)
                        val relativePath = file.toPath().relativeTo(rootDir)
                        val resources = getValueResourceItems(file, qualifiers, relativePath)
                        resources.forEach {
                            if (!valueKeys.add(it.name)) {
                                gradleError("Duplicate resource key '${it.name}' found in directory: ${dir.name}")
                            }
                        }
                        resources
                    }
            }
            .groupBy { it.type }
            .mapValues { (_, items) -> items.groupBy { it.name } }
    }

    // Create ResourceItem metadata for a "standard" resource type. These types support nested subdirectories.
    private fun resolveStandardResources(
        type: ResourceType,
        variantDirs: List<File>,
        rootResDir: Path,
        allowAccessorSubDirs: Boolean
    ): Map<String, List<ResourceItem>> {

        // Track all resource keys pr. directory so we can catch name conflicts
        val idsPrDirectory: MutableMap<Path, MutableSet<String>> = mutableMapOf()

        val result = variantDirs.flatMap { dir ->
            val typeAndQualifiers = dir.name.split("-")
            val qualifiers = typeAndQualifiers.takeLast(typeAndQualifiers.size - 1)
            dir.walkTopDown()
                .drop(1)
                .filter { !it.isHidden }
                .mapNotNull { entry ->
                    val relativePath = entry.toPath().relativeTo(rootResDir)
                    if (entry.isDirectory) {
                        val dirKey = createResourceDirectoryKey(relativePath)
                        when {
                            dirKey == null -> return@mapNotNull null // Ignore top-level resource type directory
                            !allowAccessorSubDirs -> gradleError("Resource subdirectory is not allowed: $relativePath")
                        }
                        val success = idsPrDirectory.getOrPut(relativePath.parent) { mutableSetOf() }.add(dirKey.lowercase())
                        if (!success) {
                            gradleError("Resource subdirectory conflicts with another resource file or directory: $relativePath")
                        }
                        null
                    } else {
                        val dirKey = createResourceDirectoryKey(relativePath.parent)
                        val fileKey = createResourceFileKey(relativePath)

                        // Check that the resource name is not already taken.
                        // e.g. `compose.png` and `compose.jpg` are not allowed in the same directory.
                        val resourceName = when (dirKey != null) {
                            true -> "$dirKey.$fileKey"
                            false -> fileKey
                        }
                        val success = idsPrDirectory.getOrPut(relativePath.parent) { mutableSetOf() }.add(resourceName.lowercase())
                        if (!success) {
                            gradleError("Resource file conflicts with another resource file or directory: $relativePath")
                        }

                        ResourceItem(
                            type = type,
                            qualifiers = qualifiers,
                            name = resourceName,
                            path = relativePath,
                            contentHash = entry.resourceContentHash()
                        )
                    }
                }
        }
            .groupBy { it.type }
            .mapValues { (_, items) -> items.groupBy { it.normalizedName } }

        // When generating accessors, we want to use the casing of the nonqualified directory.
        // This is always the first entry
        return result.entries.single().value.let {
            it.mapKeys { (key, values) -> values.first().name }
        }
    }

    // `resourceDirectory` is the relative path from `composeResources` to a
    // subdirectory under one of the supported resource types.
    // Top-level resource directories like `/drawable` return `null`
    private fun createResourceDirectoryKey(resourceDirectory: Path): String? {
        val parts = resourceDirectory.map { it.toString() }.subList(1, resourceDirectory.nameCount)

        // We re-use intermediate dirs names directly as Kotlin objects (instead of trying
        // to rename them). This means that all intermediate dirs have stricter naming conventions
        // than normal directories. Kotlin Poet will escape keywords like `class`, but we check
        // for other common errors here. This does not capture all corner-cases (like use of emoji),
        // but should provide a good enough DX while still being reasonable fast.
        parts.indices.forEach { index ->
            val pathSegment = parts[index]
            pathSegment.forEachIndexed { index, char ->
                if (index == 0 && char.isDigit()) {
                    throw GradleException("Resource directory names cannot start with a digit: $resourceDirectory")
                }
                if (char == '.') {
                    throw GradleException("`.` not allowed in resource directory names: $resourceDirectory")
                }
                if (char.isWhitespace()) {
                    throw GradleException("Whitespace not allowed in resource directory names: $resourceDirectory")
                }
            }
        }

        // Convert user directories as-is, including any casing they might have.
        return when (parts.isNotEmpty()) {
            true -> parts.joinToString(".")
            false -> null
        }
    }

    // `resourceFile` is the relative path from `composeResources` to a
    // file under one of the supported resource types.
    private fun createResourceFileKey(resourceFile: Path): String {
        // Filenames will be converted to extension properties and therefore have a number of
        // restrictions, but at the same time we want to support some common naming schems:
        // - A file with a digit as the first letter gets a `_` suffix to the identifier.
        // - Files with `-` in the file name, gets it replaced with `_` in the identifier.
        //
        // Similar to directory names, we do not want to catch all corner-cases
        // here, just the most common ones, and otherwise just let the underlying file system
        // crash.
        return resourceFile
            .nameWithoutExtension
            .asUnderscoredIdentifier()
            .also { name ->
                name.forEach { char ->
                    if (char == '.') {
                        throw GradleException("`.` not allowed in resource file names: $resourceFile")
                    }
                    if (char.isWhitespace()) {
                        throw GradleException("Whitespace not allowed in resource file names: $resourceFile")
                    }
                }
            }
    }

    private fun File.resourceContentHash(): Int {
        return if ((currentOS == OS.Windows) && isTextResourceFile()) {
            // Windows has different line endings in comparison with Unixes,
            // thus text resource files binary differ there, so we need to handle this.
            readText().replace("\r\n", "\n").toByteArray().contentHashCode()
        } else {
            readBytes().contentHashCode()
        }
    }

    private fun getValueResourceItems(dataFile: File, qualifiers: List<String>, path: Path): List<ResourceItem> {
        val result = mutableListOf<ResourceItem>()
        dataFile.bufferedReader().use { f ->
            var offset = 0L
            var line: String? = f.readLine()
            while (line != null) {
                val size = line.encodeToByteArray().size

                //first line is meta info
                if (offset > 0) {
                    result.add(getValueResourceItem(line, offset, size.toLong(), qualifiers, path))
                }

                offset += size + 1 // "+1" for newline character
                line = f.readLine()
            }
        }
        return result
    }

    private fun File.isTextResourceFile(): Boolean =
        path.endsWith(".xml", true) || path.endsWith(".svg", true)


    private fun getValueResourceItem(
        recordString: String,
        offset: Long,
        size: Long,
        qualifiers: List<String>,
        path: Path
    ): ResourceItem {
        val record = ValueResourceRecord.createFromString(recordString)
        return ResourceItem(
            record.type,
            qualifiers,
            record.key.asUnderscoredIdentifier(),
            path,
            record.content.hashCode(),
            offset,
            size
        )
    }
}
