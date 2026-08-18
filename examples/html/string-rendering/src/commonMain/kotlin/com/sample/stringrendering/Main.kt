package com.sample.stringrendering

import org.jetbrains.compose.web.composeHtmlToString

fun main() {
    val document = composeHtmlToString {
        LandingDocument()
    }

    println("<!DOCTYPE html>$document")
}
