package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageAsset
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
expect fun imageResource(res: String): ImageAsset

expect suspend fun imageFromUrl(url: String): ImageAsset

@Composable
expect fun font(name: String, res: String, weight: FontWeight, style: FontStyle): Font