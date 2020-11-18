package org.jetbrains.codeviewer.platform

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.io.InputStream
import java.net.URL

@Composable
actual fun imageResource(res: String): ImageBitmap {
    val context = AmbientContext.current
    val id = context.resources.getIdentifier(res, "drawable", context.packageName)
    return androidx.compose.ui.res.imageResource(id)
}

actual suspend fun imageFromUrl(url: String): ImageBitmap {
    val bytes = URL(url).openStream().buffered().use(InputStream::readBytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
}

@Composable
actual fun font(name: String, res: String, weight: FontWeight, style: FontStyle): Font {
    val context = AmbientContext.current
    val id = context.resources.getIdentifier(res, "font", context.packageName)
    return androidx.compose.ui.text.font.font(id, weight, style)
}