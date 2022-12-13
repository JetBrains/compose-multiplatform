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

actual suspend fun loadImages(cachePath: String, list: List<String>): List<Picture> =
    withContext(SupervisorJob() + Dispatchers.IO) {
        list.mapIndexed { index, source->
            async {
                val name = getNameURL(source)
                val path = cachePath + File.separator + name
                if (File(path + "info").exists()) {
                    getCachedMiniature(filePath = path)
                } else {
                    getFreshMiniature(source = source, path = cachePath)
                }
            }
        }.awaitAll().filterNotNull()
    }

private suspend fun getFreshMiniature(
    source: String,
    path: String
):Picture? {
    TODO()
//    return try {
//        val url = URL(source)//todo ktor
//        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
//        connection.connectTimeout = 5000
//        connection.connect()
//
//        val input: InputStream = connection.inputStream
//        val result: Bitmap? = BitmapFactory.decodeStream(input)
//
//        if (result != null) {
//            val picture = Picture(
//                source,
//                scaleBitmapAspectRatio(result, 200, 164),
//            )
//
//            cacheImage(path + getNameURL(source), picture)
//            picture
//        } else {
//            null
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//        null
//    }
}

private suspend fun getCachedMiniature(
    filePath: String
):Picture? {
    TODO()
//    return try {
//        val read = BufferedReader(
//            InputStreamReader(
//                FileInputStream(filePath + cacheImagePostfix),
//                StandardCharsets.UTF_8
//            )
//        )
//
//        val source = read.readLine()
//        val width = read.readLine().toInt()
//        val height = read.readLine().toInt()
//
//        read.close()
//
//        val result: Bitmap? = BitmapFactory.decodeFile(filePath)
//
//        if (result != null) {
//            val picture = Picture(
//                source,
//                result
//            )
//            picture
//        } else {
//            null
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//        null
//    }
}
