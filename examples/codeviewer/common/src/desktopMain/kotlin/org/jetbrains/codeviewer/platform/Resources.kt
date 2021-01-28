package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skija.Image
import java.io.InputStream
import java.net.URL

@Composable
actual fun imageResource(res: String) = androidx.compose.ui.res.imageResource("drawable/$res.png")

actual suspend fun imageFromUrl(url: String): ImageBitmap = withContext(Dispatchers.IO) {
    val bytes = URL(url).openStream().buffered().use(InputStream::readBytes)
    Image.makeFromEncoded(bytes).asImageBitmap()
}

@Composable
actual fun Font(name: String, res: String, weight: FontWeight, style: FontStyle): Font =
       androidx.compose.ui.text.platform.Font("font/$res.ttf", weight, style)