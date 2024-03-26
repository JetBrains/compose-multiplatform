package org.jetbrains.compose.resources

private object JvmResourceReader

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun readResourceBytes(path: String): ByteArray {
    val classLoader = getClassLoader()
    val resource = classLoader.getResourceAsStream(path) ?: throw MissingResourceException(path)
    return resource.readBytes()
}

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun convertPathToUri(path: String): String {
    val resource = getClassLoader()?.getResource(path) ?: throw MissingResourceException(path)
    return resource.toURI().toString()
}

private fun getClassLoader() =
    Thread.currentThread().contextClassLoader ?: JvmResourceReader.javaClass.classLoader