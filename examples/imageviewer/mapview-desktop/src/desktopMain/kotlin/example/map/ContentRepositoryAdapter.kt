package example.map

fun <K, A, B> ContentRepository<K, A>.adapter(transform: (A) -> B): ContentRepository<K, B> {
    val origin = this
    return object : ContentRepository<K, B> {
        override suspend fun loadContent(key: K): B {
            return transform(origin.loadContent(key))
        }
    }
}
