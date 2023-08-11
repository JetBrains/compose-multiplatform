package com.lib

import androidx.compose.runtime.Composable
import com.example.common.TextLeafNode

actual val Abc.composableIntVal: Int
    @Composable get () = 100

actual fun getPlatformName(): String = "Js"

@Composable
actual fun ComposableExpectActual() {
    TextLeafNode("Js")
}

@Composable
actual fun ComposableExpectActualWithDefaultParameter(p1: String) {
    TextLeafNode("$p1-Js")
}