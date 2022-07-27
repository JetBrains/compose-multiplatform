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

package androidx.compose.foundation.demos.text

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.ParcelFileDescriptor
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.demos.text.FontVariationSettingsCompot.compatSetFontVariationSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Checkbox
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun VariableFontsDemo() {
    if (Build.VERSION.SDK_INT < 26) {
        Text("Variable fonts are only supported on API 26+")
     }

    val (weight, setWeight) = remember { mutableStateOf(1000f) }
    val (italic, setItalic) = remember { mutableStateOf(false) }
    LazyColumn {
        this.stickyHeader {
            Column(Modifier.background(Color.White)) {
                Slider(
                    value = weight,
                    onValueChange = setWeight,
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = 1f..1000f
                )
                Row {
                    Text("Italic: ")
                    Checkbox(checked = italic, onCheckedChange = setItalic)
                }
            }
        }
        item {
            Text("These demos show setting fontVariationSettings on a demo font that " +
                "exaggerates 'wght'. Font only supports the codepoint 'A' code=\"0x41\"")
        }
        item {
            TagLine(tag = "AssetFont")
            AssetFont(weight.toInt(), italic)
        }
        item {
            TagLine(tag = "FileFont")
            FileFont(weight.toInt(), italic)
        }
        if (Build.VERSION.SDK_INT >= 26) {
            // PDF is 26+
            item {
                TagLine(tag = "ParcelFileDescriptorFont")
                ParcelFileDescriptorFont(weight.toInt(), italic)
            }
        }
        item {
            TagLine(tag = "DeviceNamedFontFamily")
            DeviceNamedFontFamilyFont(weight.toInt(), italic)
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun AssetFont(weight: Int, italic: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val assetFonts = remember(weight, italic) {
            FontFamily(
                Font(
                    "subdirectory/asset_variable_font.ttf",
                    context.assets,
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(weight.toInt()), /* Changes "A" glyph */
                        /* italic not supported by font, ignored */
                        FontVariation.italic(if (italic) 1f else 0f)
                    )
                )
            )
        }
        Text(
            "A",
            fontSize = 48.sp,
            fontFamily = assetFonts,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun FileFont(weight: Int, italic: Boolean) {
    val context = LocalContext.current
    val filePath = remember { mutableStateOf<String?> (null) }
    LaunchedEffect(Unit) {
        filePath.value = mkTempFont(context).path
    }
    val actualPath = filePath.value ?: return

    Column(Modifier.fillMaxWidth()) {
        val assetFonts = remember(weight, italic) {
            FontFamily(
                Font(
                    File(actualPath),
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(weight.toInt()), /* Changes "A" glyph */
                        /* italic not supported by font, ignored */
                        FontVariation.italic(if (italic) 1f else 0f)
                    )
                )
            )
        }
        Text(
            "A",
            fontSize = 48.sp,
            fontFamily = assetFonts,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
@RequiresApi(26)
fun ParcelFileDescriptorFont(weight: Int, italic: Boolean) {
    val context = LocalContext.current
    val filePath = remember { mutableStateOf<String?> (null) }
    LaunchedEffect(Unit) {
        filePath.value = mkTempFont(context).path
    }
    val actualPath = filePath.value ?: return

    Column(Modifier.fillMaxWidth()) {
        val assetFonts = remember(weight, italic) {
            FontFamily(
                Font(
                    File(actualPath).toParcelFileDescriptor(context),
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(weight.toInt()), /* Changes "A" glyph */
                        /* italic not supported by font, ignored */
                        FontVariation.italic(if (italic) 1f else 0f)
                    )
                )
            )
        }
        Text(
            "A",
            fontSize = 48.sp,
            fontFamily = assetFonts,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun DeviceNamedFontFamilyFont(weight: Int, italic: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        val assetFonts = remember(weight, italic) {
            FontFamily(
                Font(
                    DeviceFontFamilyName("sans-serif"),
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(weight.toInt()), /* Changes "A" glyph */
                        /* italic not supported by font, ignored */
                        FontVariation.italic(if (italic) 1f else 0f)
                    )
                )
            )
        }
        Text(
            "Setting variation on system fonts has no effect on (most) Android builds",
            fontSize = 12.sp,
            fontFamily = assetFonts,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        val textPaint = remember { TextPaint() }
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)) {
            this.drawIntoCanvas {
                val nativeCanvas = drawContext.canvas.nativeCanvas
                textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                textPaint.textSize = 24f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    textPaint.compatSetFontVariationSettings(
                        "'wght' $weight, 'ital' ${if (italic) 1f else 0f}"
                    )
                }
                nativeCanvas.drawText(
                    "Platform 'sans-serif' behavior on this device' (nativeCanvas)" /* text */,
                    0f /* x */,
                    40f /* y */,
                    textPaint)
            }
        }
    }
}
private suspend fun mkTempFont(context: Context): File = withContext(Dispatchers.IO) {
    val temp = File.createTempFile("tmp", ".ttf", context.filesDir)
    context.assets.open("subdirectory/asset_variable_font.ttf").use { input ->
        val bytes = input.readBytes()
        context.openFileOutput(temp.name, Context.MODE_PRIVATE).use { output ->
            output.write(bytes)
        }
    }
    temp
}

private fun File.toParcelFileDescriptor(context: Context): ParcelFileDescriptor {
    context.openFileInput(name).use { input ->
        return ParcelFileDescriptor.dup(input.fd)
    }
}

@RequiresApi(26)
object FontVariationSettingsCompot {
    fun TextPaint.compatSetFontVariationSettings(variationSettings: String) {
        fontVariationSettings = variationSettings
    }
}