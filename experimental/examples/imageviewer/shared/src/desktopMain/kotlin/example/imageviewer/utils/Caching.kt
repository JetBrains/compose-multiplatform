package example.imageviewer.utils

import example.imageviewer.model.ContentRepository
import example.imageviewer.model.getNameURL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.absoluteValue

fun ContentRepository<ByteArray>.decorateWithDiskCache(
    backgroundScope: CoroutineScope,
    cacheDir: File
): ContentRepository<ByteArray> {

    class FileSystemLock

    val origin = this
    val locksCount = 100
    val locks = Array(locksCount) { FileSystemLock() }

    fun getLock(url: String) = locks[url.hashCode().absoluteValue % locksCount]

    return object : ContentRepository<ByteArray> {
        init {
            try {
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                println("Can't create cache dir $cacheDir")
            }
        }

        override suspend fun loadContent(url: String): ByteArray {
            if (!cacheDir.exists()) {
                return origin.loadContent(url)
            }
            val file = cacheDir.resolve("cache-${getNameURL(url)}.png")
            val fromCache: ByteArray? = synchronized(getLock(url)) {
                if (file.exists()) {
                    try {
                        file.readBytes()
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        println("Can't read file $file")
                        println("Will work without disk cache")
                        null
                    }
                } else {
                    null
                }
            }

            val result = if (fromCache != null) {
                fromCache
            } else {
                val image = origin.loadContent(url)
                backgroundScope.launch {
                    synchronized(getLock(url)) {
                        // save to cacheDir
                        try {
                            file.writeBytes(image)
                        } catch (t: Throwable) {
                            println("Can't save image to file $file")
                            println("Will work without disk cache")
                        }
                    }
                }
                image
            }
            return result
        }

    }
}
