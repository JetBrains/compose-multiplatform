package org.jetbrains.compose.demo.visuals

import androidx.compose.runtime.Composable
import org.jetbrains.compose.demo.visuals.NYContent

actual fun width(): Int = 400
actual fun height(): Int = 800

@Composable
fun MainView() = NYContent()