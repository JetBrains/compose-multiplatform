package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
expect fun imageResource(res: String): ImageBitmap

expect suspend fun imageFromUrl(url: String): ImageBitmap

@Composable
expect fun Font(name: String, res: String, weight: FontWeight, style: FontStyle): Font