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
    override fun getUri(path: String): String {
        val classLoader = getClassLoader()
        val resource = classLoader.getResource(path) ?: throw MissingResourceException(path)
        return resource.toURI().toString()
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun getResourceAsStream(path: String): InputStream {
        val classLoader = getClassLoader()
        return classLoader.getResourceAsStream(path) ?: throw MissingResourceException(path)
    }

    private fun getClassLoader(): ClassLoader {
        return Thread.currentThread().contextClassLoader ?: this.javaClass.classLoader!!
    }
}