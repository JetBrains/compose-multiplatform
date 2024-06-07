package org.jetbrains.compose.resources

import java.io.File
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
            input.skipBytes(offset)
            input.readBytes(result, 0, size.toInt())
        }
        return result
    }

    //skipNBytes requires API 34
    private fun InputStream.skipBytes(offset: Long) {
        var skippedBytes = 0L
        while (skippedBytes < offset) {
            val count = skip(offset - skippedBytes)
            if (count == 0L) break
            skippedBytes += count
        }
    }

    //readNBytes requires API 34
    private fun InputStream.readBytes(byteArray: ByteArray, offset: Int, size: Int) {
        var readBytes = 0
        while (readBytes < size) {
            val count = read(byteArray,  offset + readBytes, size - readBytes)
            if (count <= 0) break
            readBytes += count
        }
    }

    override fun getUri(path: String): String {
        val classLoader = getClassLoader()
        val resource = classLoader.getResource(path) ?: run {
            //try to find a font in the android assets
            if (File(path).isFontResource()) {
                classLoader.getResource("assets/$path")
            } else null
        } ?: throw MissingResourceException(path)
        return resource.toURI().toString()
    }

    private fun getResourceAsStream(path: String): InputStream {
        val classLoader = getClassLoader()
        val resource = classLoader.getResourceAsStream(path) ?: run {
            //try to find a font in the android assets
            if (File(path).isFontResource()) {
                classLoader.getResourceAsStream("assets/$path")
            } else null
        } ?: throw MissingResourceException(path)
        return resource
    }

    private fun File.isFontResource(): Boolean {
        return this.parentFile?.name.orEmpty().startsWith("font")
    }

    private fun getClassLoader(): ClassLoader {
        return this.javaClass.classLoader ?: error("Cannot find class loader")
    }
}