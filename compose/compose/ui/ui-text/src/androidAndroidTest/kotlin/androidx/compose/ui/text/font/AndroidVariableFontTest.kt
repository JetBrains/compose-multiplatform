/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.text.font
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.os.ParcelFileDescriptor
import android.text.TextPaint
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTextApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidVariableFontTest {
    private var tempFile: File? = null
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val assetFontPath = "subdirectory/asset_variable_font.ttf"

    @Before
    fun setup() {
        deleteFile()
        writeFile()
    }

    @After
    fun cleanupAfter() {
        deleteFile()
    }

    private fun deleteFile() {
        if (tempFile?.exists() == true) {
            tempFile?.delete()
        }
    }

    private fun writeFile() {
        tempFile = File.createTempFile("tmp_file_font", ".ttf", context.filesDir)
        context.assets.open(assetFontPath).use { input ->
            val bytes = input.readBytes()
            context.openFileOutput(tempFile?.name, Context.MODE_PRIVATE).use { output ->
                output.write(bytes)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun fileFont_differentFontVariationSettings_differentResults() {
        val font1 = Font(
            file = tempFile!!,
            variationSettings = FontVariation.Settings(FontVariation.weight(1))
        ) as AndroidFont
        val font1000 = Font(
            file = tempFile!!,
            variationSettings = FontVariation.Settings(FontVariation.weight(1000))
        ) as AndroidFont

        val bitmap1 = font1.typefaceLoader.loadBlocking(context, font1)!!.drawToBitmap("A")
        val bitmap2 = font1000.typefaceLoader.loadBlocking(context, font1000)!!.drawToBitmap("A")
        assertThat(bitmap1.sameAs(bitmap2)).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun fontFile_defaultsWeight_whenWeightSet() {
        val font1 = Font(
            file = tempFile!!,
            weight = FontWeight(1)
        ) as AndroidFont
        val font1000 = Font(
            file = tempFile!!,
            weight = FontWeight(1000)
        ) as AndroidFont

        val bitmap1 = font1.typefaceLoader.loadBlocking(context, font1)!!.drawToBitmap("A")
        val bitmap2 = font1000.typefaceLoader.loadBlocking(context, font1000)!!.drawToBitmap("A")
        assertThat(bitmap1.sameAs(bitmap2)).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun assetFont_differentFontVariationSettings_differentResults() {
        val font1 = Font(
            path = assetFontPath,
            context.assets,
            variationSettings = FontVariation.Settings(FontVariation.weight(1))
        ) as AndroidFont
        val font1000 = Font(
            path = assetFontPath,
            context.assets,
            variationSettings = FontVariation.Settings(FontVariation.weight(1000))
        ) as AndroidFont

        val bitmap1 = font1.typefaceLoader.loadBlocking(context, font1)!!.drawToBitmap("A")
        val bitmap2 = font1000.typefaceLoader.loadBlocking(context, font1000)!!.drawToBitmap("A")
        assertThat(bitmap1.sameAs(bitmap2)).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun assetFile_defaultsWeight_whenWeightSet() {
        val font1 = Font(
            path = assetFontPath,
            context.assets,
            weight = FontWeight(1)
        ) as AndroidFont
        val font1000 = Font(
            path = assetFontPath,
            context.assets,
            weight = FontWeight(1000)
        ) as AndroidFont

        val bitmap1 = font1.typefaceLoader.loadBlocking(context, font1)!!.drawToBitmap("A")
        val bitmap2 = font1000.typefaceLoader.loadBlocking(context, font1000)!!.drawToBitmap("A")
        assertThat(bitmap1.sameAs(bitmap2)).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun parcelFont_differentFontVariationSettings_differentResults() {
        val font1 = context.openFileInput(tempFile?.name).use { inputStream ->
            Font(
                ParcelFileDescriptor.dup(inputStream.fd),
                variationSettings = FontVariation.Settings(FontVariation.weight(1))
            ) as AndroidFont
        }
        val font1000 = context.openFileInput(tempFile?.name).use { inputStream ->
            Font(
                ParcelFileDescriptor.dup(inputStream.fd),
                variationSettings = FontVariation.Settings(FontVariation.weight(1000))
            ) as AndroidFont
        }

        val bitmap1 = font1.typefaceLoader.loadBlocking(context, font1)!!.drawToBitmap("A")
        val bitmap2 = font1000.typefaceLoader.loadBlocking(context, font1000)!!.drawToBitmap("A")
        assertThat(bitmap1.sameAs(bitmap2)).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun parcelFile_defaultsWeight_whenWeightSet() {
        val font1 = context.openFileInput(tempFile?.name).use { inputStream ->
            Font(
                ParcelFileDescriptor.dup(inputStream.fd),
                variationSettings = FontVariation.Settings(FontVariation.weight(1))
            ) as AndroidFont
        }
        val font1000 = context.openFileInput(tempFile?.name).use { inputStream ->
            Font(
                ParcelFileDescriptor.dup(inputStream.fd),
                variationSettings = FontVariation.Settings(FontVariation.weight(1000))
            ) as AndroidFont
        }

        val bitmap1 = font1.typefaceLoader.loadBlocking(context, font1)!!.drawToBitmap("A")
        val bitmap2 = font1000.typefaceLoader.loadBlocking(context, font1000)!!.drawToBitmap("A")
        assertThat(bitmap1.sameAs(bitmap2)).isFalse()
    }
}

private fun Typeface.drawToBitmap(s: String): Bitmap {
    val tp = TextPaint()
    tp.color = Color.WHITE
    tp.bgColor = Color.YELLOW
    tp.textSize = 25f
    tp.typeface = this
    tp.hinting = TextPaint.HINTING_OFF
    tp.isAntiAlias = false
    val bitmap = Bitmap.createBitmap(30, 30, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawRect(0f, 0f, 30f, 30f, tp)
    tp.color = Color.BLUE
    canvas.drawText(s, 0f, 20f, tp)
    return bitmap
}
