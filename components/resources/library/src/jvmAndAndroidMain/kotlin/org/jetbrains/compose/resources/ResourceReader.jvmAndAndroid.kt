package org.jetbrains.compose.resources

private object JvmResourceReader

@ExperimentalResourceApi
actual suspend fun readResourceBytes(path: String, defaultPath: String?): ByteArray {
    val classLoader = Thread.currentThread().contextClassLoader ?: JvmResourceReader.javaClass.classLoader
    val resource = classLoader.getResourceAsStream(path)
            ?: defaultPath?.let { classLoader.getResourceAsStream(it) }
            ?: throw MissingResourceException(path)
    return resource.readBytes()
}