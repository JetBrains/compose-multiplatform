package com.mylibrary.demo

import androidx.compose.ui.window.singleWindowApplication
import com.mylibrary.HelloWidget

fun main() = singleWindowApplication(
    title = "Library demo"
) {
    HelloWidget()
}