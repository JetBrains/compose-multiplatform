package com.map.collection

/**
 * Стэк, который работает как LRU cache.
 * При переполнении maxSize удаляются элементы из глубины стэка, а новый элемент кладётся на вершину стэка.
 */
fun <T> createStack(maxSize: Int): CollectionAddRemove<T> = Stack(maxSize)

private data class Stack<T>(val maxSize: Int, val list: List<T> = emptyList()) : CollectionAddRemove<T> {
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
        if (list.isNotEmpty()) {
            return RemoveResult(
                collection = copy(list = list.dropLast(1)),
                removed = list.last()
            )
        } else {
            return RemoveResult(
                collection = this,
                null
            )
        }
    }

    override val size: Int get() = list.size
    override fun isEmpty(): Boolean = list.isEmpty()
}
