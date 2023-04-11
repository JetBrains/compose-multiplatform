package example.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

fun ContentRepository<Tile, ByteArray>.decorateWithDiskCache(
    backgroundScope: CoroutineScope,
    cacheDir: File
): ContentRepository<Tile, ByteArray> {

    class FileSystemLock()

    val origin = this
    val locksCount = 100
    val locks = Array(locksCount) { FileSystemLock() }

    fun getLock(key: Tile) = locks[key.hashCode() % locksCount]

    return object : ContentRepository<Tile, ByteArray> {
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

        override suspend fun loadContent(key: Tile): ByteArray {
            if (!cacheDir.exists()) {
                return origin.loadContent(key)
            }
            val file = with(key) {
                cacheDir.resolve("tile-$zoom-$x-$y.png")
            }

            val fromCache: ByteArray? = synchronized(getLock(key)) {
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
                val image = origin.loadContent(key)
                backgroundScope.launch {
                    synchronized(getLock(key)) {
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
