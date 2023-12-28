package org.jetbrains.compose.resources

private object JvmResourceReader

@OptIn(ExperimentalResourceApi::class)
actual suspend fun readResourceBytes(path: String): ByteArray {
    val classLoader = Thread.currentThread().contextClassLoader ?: JvmResourceReader.javaClass.classLoader
    val resource = classLoader.getResourceAsStream(path) ?: throw MissingResourceException(path)
    return resource.readBytes()
}