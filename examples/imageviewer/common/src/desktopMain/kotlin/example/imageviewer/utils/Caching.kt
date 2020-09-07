/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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