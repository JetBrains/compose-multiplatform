package org.jetbrains.compose.resources

import java.io.InputStream

actual fun getDefaultResourceReader(): ResourceReader =
    JvmResourceReader.Default

class JvmResourceReader(
    private val classLoader: ClassLoader = JvmResourceReader::class.java.classLoader
) : ResourceReader {

    companion object {
        val Default = JvmResourceReader()
    }

    override suspend fun read(path: String): ByteArray =
        getResourceAsStream(path)
            .use { input -> input.readBytes() }

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
        val resource = getResourceAsStream(path)
        val result = ByteArray(size.toInt())
        resource.use { input ->
            input.skipBytes(offset)
            input.readNBytes(result, 0, size.toInt())
        }
        return result
    }

    //skipNBytes requires API 12
    private fun InputStream.skipBytes(offset: Long) {
        var skippedBytes = 0L
        while (skippedBytes < offset) {
            val count = skip(offset - skippedBytes)
            if (count == 0L) break
            skippedBytes += count
        }
    }

    override fun getUri(path: String): String =
        classLoader
            .getResource(path)
            ?.toURI()
            ?.toString()
            ?: throw MissingResourceException(path)

    private fun getResourceAsStream(path: String): InputStream =
        classLoader.getResourceAsStream(path) ?: throw MissingResourceException(path)

}
