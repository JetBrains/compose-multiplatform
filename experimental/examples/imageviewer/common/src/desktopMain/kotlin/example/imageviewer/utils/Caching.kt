package example.imageviewer.utils

import java.awt.image.BufferedImage
import example.imageviewer.model.Picture
import javax.imageio.ImageIO
import java.io.File
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

val cacheImagePostfix = "info"
val cacheImagePath = System.getProperty("user.home")!! +
        File.separator + "Pictures/imageviewer" + File.separator

fun cacheImage(path: String, picture: Picture) {
    try {
        ImageIO.write(picture.image, "png", File(path))

        val bw =
            BufferedWriter(
                OutputStreamWriter(
                    FileOutputStream(path + cacheImagePostfix),
                    StandardCharsets.UTF_8
                )
            )

        bw.write(picture.source)
        bw.write("\r\n${picture.width}")
        bw.write("\r\n${picture.height}")
        bw.close()

    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun clearCache() {

    val directory = File(cacheImagePath)

    val files: Array<File>? = directory.listFiles()

    if (files != null) {
        for (file in files) {
            if (file.isDirectory)
                continue

            file.delete()
        }
    }
}