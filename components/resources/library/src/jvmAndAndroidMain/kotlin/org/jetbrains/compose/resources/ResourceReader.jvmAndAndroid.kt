package org.jetbrains.compose.resources

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun readResourceBytes(path: String): ByteArray {
    val resource = getClassLoader().getResourceAsStream(path) ?: throw MissingResourceException(path)
    return resource.readBytes()
}

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual fun getResourceUriString(path: String): String {
    val resource = getClassLoader()?.getResource(path) ?: throw MissingResourceException(path)
    return resource.toURI().toString()
}

private object JvmResourceReader
private fun getClassLoader() =
    Thread.currentThread().contextClassLoader ?: JvmResourceReader.javaClass.classLoader