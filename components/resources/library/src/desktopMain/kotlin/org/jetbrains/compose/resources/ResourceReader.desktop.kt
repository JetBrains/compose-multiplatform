package org.jetbrains.compose.resources

import java.io.InputStream

@ExperimentalResourceApi
internal actual fun getPlatformResourceReader(): ResourceReader =
    JvmResourceReader.Default

@ExperimentalResourceApi
class JvmResourceReader(
    private val classLoader: ClassLoader
) : ResourceReader {

    companion object {
        internal val Default = JvmResourceReader(JvmResourceReader::class.java.classLoader)
    }

    override suspend fun read(path: String): ByteArray {
        val resource = getResourceAsStream(path)
        return resource.use { input -> input.readBytes() }
    }

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

    override fun getUri(path: String): String {
        val resource = classLoader.getResource(path) ?: throw MissingResourceException(path)
        return resource.toURI().toString()
    }

    private fun getResourceAsStream(path: String): InputStream {
        return classLoader.getResourceAsStream(path) ?: throw MissingResourceException(path)
    }

}