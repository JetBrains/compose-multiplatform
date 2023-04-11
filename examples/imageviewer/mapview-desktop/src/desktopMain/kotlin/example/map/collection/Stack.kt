package example.map.collection

/**
 * A stack that works like LRU cache.
 * When maxSize overflows, elements are removed from the depth of the stack,
 * and a new element is placed on top of the stack.
 */
fun <T> createStack(maxSize: Int): ImmutableCollection<T> = Stack(maxSize)

private data class Stack<T>(
    val maxSize: Int,
    val list: List<T> = emptyList()
) : ImmutableCollection<T> {
    init {
        check(maxSize > 0) { "specify maxSize > 0" }
    }

    override fun add(element: T): RemoveResult<T> {
        return if (list.size >= maxSize) {
            RemoveResult(
                collection = copy(list = list.drop(1) + element),
                removed = list.first()
            )
        } else {
            RemoveResult(
                collection = copy(list = list + element),
                removed = null
            )
        }
    }

    override fun remove(): RemoveResult<T> {
        return if (list.isNotEmpty()) {
            RemoveResult(
                collection = copy(list = list.dropLast(1)),
                removed = list.last()
            )
        } else {
            RemoveResult(
                collection = this,
                null
            )
        }
    }

    override val size: Int get() = list.size
    override fun isEmpty(): Boolean = list.isEmpty()
}
