package example.imageviewer

import example.imageviewer.model.ContentRepository

fun <A, B> ContentRepository<A>.adapter(transform: (A) -> B): ContentRepository<B> {
    val origin = this
    return object : ContentRepository<B> {
        override suspend fun loadContent(url: String): B {
            return transform(origin.loadContent(url))
        }
    }
}
