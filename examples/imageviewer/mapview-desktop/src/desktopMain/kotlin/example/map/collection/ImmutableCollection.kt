package example.map.collection

/**
 * Interface for thread-safe immutable collections
 */
interface ImmutableCollection<T> {
    fun add(element: T): RemoveResult<T>
    fun remove(): RemoveResult<T>
    val size: Int
    fun isEmpty(): Boolean
    fun isNotEmpty(): Boolean = isEmpty().not()
}

data class RemoveResult<T>(val collection: ImmutableCollection<T>, val removed: T?)
