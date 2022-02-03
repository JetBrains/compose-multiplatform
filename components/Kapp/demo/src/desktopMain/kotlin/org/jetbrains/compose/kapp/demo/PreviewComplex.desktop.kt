package org.jetbrains.compose.kapp.demo

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

@Preview
@Composable
fun PreviewFrame0() {
    FrameContent(0, mutableStateOf(1))
}

@Preview
@Composable
fun PreviewFrame1() {
    FrameContent(1, mutableStateOf(2))
}

fun main() = multiFrameApp()
