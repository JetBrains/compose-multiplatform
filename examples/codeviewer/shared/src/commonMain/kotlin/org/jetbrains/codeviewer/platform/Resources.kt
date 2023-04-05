package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
internal expect fun font(name: String, res: String, weight: FontWeight, style: FontStyle): Font