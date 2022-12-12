package example.imageviewer.model

import java.awt.image.BufferedImage
import example.imageviewer.utils.cacheImage
import example.imageviewer.utils.cacheImagePostfix
import example.imageviewer.utils.scaleBitmapAspectRatio
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader
import javax.imageio.ImageIO
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

fun loadFullImage(source: String): Picture {
    try {
        val url = URL(source)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.connect()

        val input: InputStream = connection.inputStream
        val bitmap: BufferedImage? = ImageIO.read(input)
        if (bitmap != null) {
            return Picture(
                source = source,
                image = bitmap,
                name = getNameURL(source),
                width = bitmap.width,
                height = bitmap.height
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return Picture(image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
}

fun loadImages(cachePath: String, list: List<String>): MutableList<Picture> {
    val result: MutableList<Picture> = ArrayList()

    for (source in list) {
        val name = getNameURL(source)
        val path = cachePath + File.separator + name

        if (File(path + "info").exists()) {
            addCachedMiniature(filePath = path, outList = result)
        } else {
            addFreshMiniature(source = source, outList = result, path = cachePath)
        }

        result.last().id = result.size - 1
    }

    return result
}

private fun addFreshMiniature(
    source: String,
    outList: MutableList<Picture>,
    path: String
) {
    try {
        val url = URL(source)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.connect()

        val input: InputStream = connection.inputStream
        val result: BufferedImage? = ImageIO.read(input)

        if (result != null) {
            val picture = Picture(
                source,
                getNameURL(source),
                scaleBitmapAspectRatio(result, 200, 164),
                result.width,
                result.height
            )

            outList.add(picture)
            cacheImage(path + getNameURL(source), picture)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun addCachedMiniature(
    filePath: String,
    outList: MutableList<Picture>
) {
    try {
        val read = BufferedReader(
            InputStreamReader(
                FileInputStream(filePath + cacheImagePostfix),
                StandardCharsets.UTF_8
            )
        )

        val source = read.readLine()
        val width = read.readLine().toInt()
        val height = read.readLine().toInt()

        read.close()

        val result: BufferedImage? = ImageIO.read(File(filePath))

        if (result != null) {
            val picture = Picture(
                source,
                getNameURL(source),
                result,
                width,
                height
            )
            outList.add(picture)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun getNameURL(url: String): String {
    return url.substring(url.lastIndexOf('/') + 1, url.length)
}