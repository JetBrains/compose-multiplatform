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
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Blocking
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTextApi::class)
class AndroidFontTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val assetFontPath = "subdirectory/asset_font.ttf"
    private val tmpFontPath = "tmp_file_font.ttf"

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
        val fontFile = File(context.filesDir, tmpFontPath)
        if (fontFile.exists()) {
            fontFile.delete()
        }
    }

    private fun writeFile() {
        context.assets.open(assetFontPath).use { input ->
            val bytes = input.readBytes()
            context.openFileOutput(tmpFontPath, Context.MODE_PRIVATE).use { output ->
                output.write(bytes)
            }
        }
    }

    private fun makeAssetFont() = Font(assetFontPath, context.assets) as AndroidFont

    @Suppress("DEPRECATION")
    private fun makeAssetFontDeprecated() = Font(context.assets, assetFontPath) as AndroidFont

    @Test
    fun test_load_from_assets() {
        val font = makeAssetFont()
        assertThat(font.typefaceLoader.loadBlocking(context, font)).isNotNull()
    }

    @Test
    fun assetFont_isBlocking() {
        val font = makeAssetFont()
        assertThat(font.loadingStrategy).isEqualTo(Blocking)
    }

    @Test
    fun assetFont_returnsSameInstance() {
        val font = makeAssetFont()
        val typeface1 = font.typefaceLoader.loadBlocking(context, font)
        val typeface2 = font.typefaceLoader.loadBlocking(context, font)
        assertThat(typeface1).isSameInstanceAs(typeface2)
    }

    @Test
    fun assetFont_doesntThrowForAsync() {
        val font = makeAssetFont()
        // don't care about result, but it's not supposed to throw
        font.typefaceLoader.loadBlocking(context, font)
    }

    @Test
    fun assetFont_isBlocking_Deprecated() {
        val font = makeAssetFontDeprecated()
        assertThat(font.loadingStrategy).isEqualTo(Blocking)
    }

    @Test
    fun assetFont_returnsSameInstance_Deprecated() {
        val font = makeAssetFontDeprecated()
        val typeface1 = font.typefaceLoader.loadBlocking(context, font)
        val typeface2 = font.typefaceLoader.loadBlocking(context, font)
        assertThat(typeface1).isSameInstanceAs(typeface2)
    }

    @Test
    fun assetFont_doesntThrowForAsync_Deprecated() {
        val font = makeAssetFontDeprecated()
        // don't care about result, but it's not supposed to throw
        font.typefaceLoader.loadBlocking(context, font)
    }

    private fun makeFileFont(): AndroidFont {
        val fontFile = File(context.filesDir, tmpFontPath)
        return Font(file = fontFile) as AndroidFont
    }

    @Test
    fun test_load_from_file() {
        val font = makeFileFont()
        assertThat(font.typefaceLoader.loadBlocking(context, font)).isNotNull()
    }

    @Test
    fun fileFont_isBlocking() {
        val font = makeFileFont()
        assertThat(font.loadingStrategy).isEqualTo(Blocking)
    }

    @Test
    fun fileFont_doesntThrowForAsync() {
        val font = makeFileFont()
        // don't care about result, but it's not supposed to throw
        font.typefaceLoader.loadBlocking(context, font)
    }

    @Test
    fun fileFont_returnsSameInstance() {
        val font = makeFileFont()
        val typeface1 = font.typefaceLoader.loadBlocking(context, font)
        val typeface2 = font.typefaceLoader.loadBlocking(context, font)
        assertThat(typeface1).isSameInstanceAs(typeface2)
    }

    @Test
    fun differentFileFonts_returnsDifferentInstances() {
        val font = makeFileFont()
        val font2 = makeFileFont()
        val loader = font.typefaceLoader
        val typeface1 = loader.loadBlocking(context, font)
        val typeface2 = loader.loadBlocking(context, font2)
        assertThat(typeface1).isNotSameInstanceAs(typeface2)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun makeFileDescriptorFont(): AndroidFont {
        return context.openFileInput(tmpFontPath).use { inputStream ->
            Font(ParcelFileDescriptor.dup(inputStream.fd)) as AndroidFont
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @MediumTest
    fun test_load_from_file_descriptor() {
        val font = makeFileDescriptorFont()
        val typeface = font.typefaceLoader.loadBlocking(context, font)
        assertThat(typeface).isNotNull()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @MediumTest
    fun fileDescriptorFont_isBlocking() {
        val font = makeFileDescriptorFont()
        assertThat(font.loadingStrategy).isEqualTo(Blocking)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @MediumTest
    fun fileDescriptor_returnsSameInstance() {
        val font = makeFileDescriptorFont()
        val typeface1 = font.typefaceLoader.loadBlocking(context, font)
        val typeface2 = font.typefaceLoader.loadBlocking(context, font)
        assertThat(typeface1).isSameInstanceAs(typeface2)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @MediumTest
    fun differentFileDescriptorFonts_returnsDifferentInstances() {
        val font = makeFileDescriptorFont()
        val font2 = makeFileDescriptorFont()
        val loader = font.typefaceLoader
        val typeface1 = loader.loadBlocking(context, font)
        val typeface2 = loader.loadBlocking(context, font2)
        assertThat(typeface1).isNotSameInstanceAs(typeface2)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @MediumTest
    fun fileDescriptorFont_doesntThrowForAsync() {
        val font = makeFileDescriptorFont()
        // don't care about result, but it's not supposed to throw
        font.typefaceLoader.loadBlocking(context, font)
    }
}