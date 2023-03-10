package example.imageviewer.model

interface ContentRepository<T> {
    suspend fun loadContent(url: String): T
}

interface WrappedHttpClient {
    suspend fun getAsBytes(urlString: String): ByteArray
}

fun createNetworkRepository(ktorClient: WrappedHttpClient) = object : ContentRepository<ByteArray> {
    override suspend fun loadContent(url: String): ByteArray =
        ktorClient.getAsBytes(url)
//        ktorClient.get(urlString = url).readBytes()
}

fun <A, B> ContentRepository<A>.adapter(transform: (A) -> B): ContentRepository<B> {
    val origin = this
    return object : ContentRepository<B> {
        override suspend fun loadContent(url: String): B {
            return transform(origin.loadContent(url))
        }
    }
}
