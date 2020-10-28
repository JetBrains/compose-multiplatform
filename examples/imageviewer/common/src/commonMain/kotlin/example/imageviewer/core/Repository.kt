package example.imageviewer.core

interface Repository<T> {
    fun get() : T
}