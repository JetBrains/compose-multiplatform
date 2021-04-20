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
import android.os.ParcelFileDescriptor
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
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

    @Test
    fun test_load_from_assets() {
        val font = Font(assetManager = context.assets, path = assetFontPath) as AndroidFont
        assertThat(font.typeface).isNotNull()
    }

    @Test
    fun test_load_from_file() {
        val fontFile = File(context.filesDir, tmpFontPath)
        val font = Font(file = fontFile) as AndroidFont
        assertThat(font.typeface).isNotNull()
    }

    @Ignore
    @Test
    @MediumTest
    fun test_load_from_file_descriptor() {
        context.openFileInput(tmpFontPath).use { inputStream ->
            val font = Font(ParcelFileDescriptor.dup(inputStream.fd)) as AndroidFont
            val typeface = font.typeface
            assertThat(typeface).isNotNull()
        }
    }
}