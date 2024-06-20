package org.jetbrains.compose.resources

import android.content.res.AssetManager
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import java.io.FileNotFoundException
import java.io.InputStream

internal actual fun getPlatformResourceReader(): ResourceReader = object : ResourceReader {
    private val assets: AssetManager by lazy {
        val context = androidContext ?: error(
            "Android context is not initialized. " +
                    "If it happens in the Preview mode then call PreviewContextConfigurationEffect() function."
        )
        context.assets
    }

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
            val count = read(byteArray, offset + readBytes, size - readBytes)
            if (count <= 0) break
            readBytes += count
        }
    }

    override fun getUri(path: String): String {
        val uri = if (assets.hasFile(path)) {
            Uri.parse("file:///android_asset/$path")
        } else {
            val classLoader = getClassLoader()
            val resource = classLoader.getResource(path) ?: throw MissingResourceException(path)
            resource.toURI()
        }
        return uri.toString()
    }

    private fun getResourceAsStream(path: String): InputStream {
        return try {
            assets.open(path)
        } catch (e: FileNotFoundException) {
            val classLoader = getClassLoader()
            classLoader.getResourceAsStream(path) ?: throw MissingResourceException(path)
        }
    }

    private fun getClassLoader(): ClassLoader {
        return this.javaClass.classLoader ?: error("Cannot find class loader")
    }

    private fun AssetManager.hasFile(path: String): Boolean {
        var inputStream: InputStream? = null
        val result = try {
            inputStream = open(path)
            true
        } catch (e: FileNotFoundException) {
            false
        } finally {
            inputStream?.close()
        }
        return result
    }
}

internal actual val ProvidableCompositionLocal<ResourceReader>.currentOrPreview: ResourceReader
    @Composable get() {
        PreviewContextConfigurationEffect()
        return current
    }
