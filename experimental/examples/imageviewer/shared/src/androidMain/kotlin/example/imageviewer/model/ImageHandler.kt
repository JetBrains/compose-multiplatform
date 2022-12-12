package example.imageviewer.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import example.imageviewer.utils.cacheImage
import example.imageviewer.utils.cacheImagePostfix
import example.imageviewer.utils.scaleBitmapAspectRatio
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

suspend fun loadFullImage(source: String): Picture {
    try {
        val url = URL(source)//todo ktor
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.connect()

        val input: InputStream = connection.inputStream
        val bitmap: Bitmap? = BitmapFactory.decodeStream(input)
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

    return Picture(image = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
}

suspend fun loadImages(cachePath: String, list: List<String>): List<Picture> =
    withContext(SupervisorJob() + Dispatchers.IO) {
        list.mapIndexed { index, source->
            async {
                val name = getNameURL(source)
                val path = cachePath + File.separator + name
                if (File(path + "info").exists()) {
                    getCachedMiniature(filePath = path)
                } else {
                    getFreshMiniature(source = source, path = cachePath)
                }?.copy(id = index)
            }
        }.awaitAll().filterNotNull()
    }

suspend fun getFreshMiniature(
    source: String,
    path: String
):Picture? {
    return try {
        val url = URL(source)//todo ktor
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.connect()

        val input: InputStream = connection.inputStream
        val result: Bitmap? = BitmapFactory.decodeStream(input)

        if (result != null) {
            val picture = Picture(
                source,
                getNameURL(source),
                scaleBitmapAspectRatio(result, 200, 164),
                result.width,
                result.height
            )

            cacheImage(path + getNameURL(source), picture)
            picture
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend private fun getCachedMiniature(
    filePath: String
):Picture? {
    return try {
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

        val result: Bitmap? = BitmapFactory.decodeFile(filePath)

        if (result != null) {
            val picture = Picture(
                source,
                getNameURL(source),
                result,
                width,
                height
            )
            picture
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getNameURL(url: String): String {
    return url.substring(url.lastIndexOf('/') + 1, url.length)
}