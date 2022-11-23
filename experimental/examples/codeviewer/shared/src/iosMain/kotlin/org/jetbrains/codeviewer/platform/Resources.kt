package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
internal actual fun font(name: String, res: String, weight: FontWeight, style: FontStyle): Font = TODO("Not yet implemented")
//       androidx.compose.ui.text.platform.Font("font/$res.ttf", weight, style)