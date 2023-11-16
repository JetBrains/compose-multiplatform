package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class ResourceItem(
    val qualifiers: List<String>,
    val path: String
)

private data class ResourceIndexRecord(
    val id: ResourceId,
    val items: List<ResourceItem>
) {
    companion object {
        //org.jetbrains.compose.resources.ResourcesSpecKt#generateResourceIndex
        fun fromString(str: String): ResourceIndexRecord {
            val id = str.substringBefore(" = ")
            val items = str.substringAfter(" = ").split("; ")
            val resourceItems = items.map { itemStr ->
                val parts = itemStr.split(':')
                val path = parts.last()
                val qualifiers: List<String> =
                    if (parts.size >= 2) parts.dropLast(1) else emptyList()
                ResourceItem(qualifiers, path)
            }
            return ResourceIndexRecord(id, resourceItems)
        }
    }
}

private val resourceIndexCacheMutex = Mutex()
private var resourceIndex = emptyMap<ResourceId, ResourceIndexRecord>()

private suspend fun getResourceRecord(id: ResourceId, resourceReader: ResourceReader): ResourceIndexRecord {
    val index = resourceIndexCacheMutex.withLock {
        if (resourceIndex.isEmpty()) {
            val indexFile = resourceReader.read("resources.index").decodeToString()
            resourceIndex = indexFile.lines()
                .map { ResourceIndexRecord.fromString(it) }
                .associateBy { it.id }
        }
        resourceIndex
    }
    return index[id] ?: error("Resource with ID='$id' not found")
}

internal suspend fun getPathById(id: ResourceId, resourceReader: ResourceReader): String {
    return selectFileByEnvironment(getResourceRecord(id, resourceReader).items).path
}

private fun selectFileByEnvironment(items: List<ResourceItem>): ResourceItem {
    //TODO
    return items.first()
}

@Composable
internal fun rememberFilePath(id: ResourceId): State<String> {
    val resourceReader = LocalResourceReader.current
    return rememberState(id, { "" }) { getPathById(id, resourceReader) }
}

internal val ResourceId.stringKey get() = substringAfter(':')