package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageAsset
import androidx.compose.ui.graphics.asImageAsset
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

actual suspend fun imageFromUrl(url: String): ImageAsset = withContext(Dispatchers.IO) {
    val bytes = URL(url).openStream().buffered().use(InputStream::readBytes)
    Image.makeFromEncoded(bytes).asImageAsset()
}

@Composable
actual fun font(name: String, res: String, weight: FontWeight, style: FontStyle): Font =
       androidx.compose.ui.text.platform.font(name, "font/$res.ttf", weight, style)