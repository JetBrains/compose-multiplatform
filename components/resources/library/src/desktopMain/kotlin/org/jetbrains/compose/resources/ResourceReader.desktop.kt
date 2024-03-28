package org.jetbrains.compose.resources

import java.io.InputStream

internal actual fun getPlatformResourceReader(): ResourceReader = object : ResourceReader {
    override suspend fun read(path: String): ByteArray {
        val resource = getResourceAsStream(path)
        return resource.readBytes()
    }

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
        val resource = getResourceAsStream(path)
        val result = ByteArray(size.toInt())
        resource.use { input ->
            input.skip(offset)
            input.read(result, 0, size.toInt())
        }
        return result
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun getResourceAsStream(path: String): InputStream {
        val classLoader = Thread.currentThread().contextClassLoader ?: this.javaClass.classLoader
        return classLoader.getResourceAsStream(path) ?: throw MissingResourceException(path)
    }
}