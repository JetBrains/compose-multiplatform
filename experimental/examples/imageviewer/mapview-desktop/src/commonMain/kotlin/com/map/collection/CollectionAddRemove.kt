package com.map.collection

/**
 * Интерфейс для потокобезопасных immutable коллекций
 */
interface CollectionAddRemove<T> {
    fun add(element: T): RemoveResult<T>
    fun remove(): RemoveResult<T>
    val size: Int
    fun isEmpty(): Boolean
    fun isNotEmpty(): Boolean = isEmpty().not()
}

data class RemoveResult<T>(val collection: CollectionAddRemove<T>, val removed: T?)
