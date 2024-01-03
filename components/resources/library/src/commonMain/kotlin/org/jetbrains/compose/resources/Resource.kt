package org.jetbrains.compose.resources

import androidx.compose.runtime.Immutable

@RequiresOptIn("This API is experimental and is likely to change in the future.")
annotation class ExperimentalResourceApi

@RequiresOptIn("This is internal API of the Compose gradle plugin.")
annotation class InternalResourceApi

/**
 * Represents a resource with an ID and a set of resource items.
 *
 * @property id The ID of the resource.
 * @property items The set of resource items associated with the resource.
 */
@Immutable
sealed class Resource(
    internal val id: String,
    internal val items: Set<ResourceItem>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Resource

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

/**
 * Represents a resource item with qualifiers and a path.
 *
 * @property qualifiers The qualifiers of the resource item.
 * @property path The path of the resource item.
 */
@Immutable
data class ResourceItem(
    internal val qualifiers: Set<Qualifier>,
    internal val path: String
)
