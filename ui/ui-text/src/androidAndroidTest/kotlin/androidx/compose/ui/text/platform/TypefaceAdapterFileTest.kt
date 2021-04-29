/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.compose.ui.text.platform

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.matchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.mock
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@MediumTest
@OptIn(ExperimentalTextApi::class)
class TypefaceAdapterFileTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val assetFontPath = "subdirectory/asset_font.ttf"
    private val tmpFontPath = "tmp_file_font.ttf"
    private fun TypefaceAdapter() = TypefaceAdapter(resourceLoader = mock())

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

    @Test
    @MediumTest
    fun customSingleFont_fromAssetManager() {
        val defaultTypeface = TypefaceAdapter().create()

        val fontFamily = Font(context.assets, assetFontPath).toFontFamily()

        val typeface = TypefaceAdapter().create(fontFamily = fontFamily)

        assertThat(typeface).isNotNull()
        // asset font have ~ defined as the only character supported.
        assertThat(typeface.bitmap("~")).isNotEqualToBitmap(defaultTypeface.bitmap("~"))
    }

    @Ignore
    @Test
    @MediumTest
    fun customSingleFont_fromFile() {
        val defaultTypeface = TypefaceAdapter().create()

        val fontFile = File(context.filesDir, tmpFontPath)
        val fontFamily = Font(fontFile).toFontFamily()

        val typeface = TypefaceAdapter().create(fontFamily = fontFamily)

        assertThat(typeface).isNotNull()
        // asset font have ~ defined as the only character supported.
        assertThat(typeface.bitmap("~")).isNotEqualToBitmap(defaultTypeface.bitmap("~"))
    }

    @Test
    @MediumTest
    fun customSingleFont_fromFileDescriptor() {
        val defaultTypeface = TypefaceAdapter().create()

        context.openFileInput(tmpFontPath).use { inputStream ->
            val fontFamily = Font(ParcelFileDescriptor.dup(inputStream.fd)).toFontFamily()
            val typeface = TypefaceAdapter().create(fontFamily = fontFamily)

            assertThat(typeface).isNotNull()
            // asset font have ~ defined as the only character supported.
            assertThat(typeface.bitmap("~")).isNotEqualToBitmap(defaultTypeface.bitmap("~"))
        }
    }
}