package org.jetbrains.compose.common.ui

interface Alignment {
    interface Vertical : Alignment
    interface Horizontal : Alignment

    companion object {
        val TopStart = object : Alignment {}
        val TopCenter = object : Alignment {}
        val TopEnd = object : Alignment {}
        val CenterStart = object : Alignment {}
        val Center = object : Alignment {}
        val CenterEnd = object : Alignment {}
        val BoottomStart = object : Alignment {}
        val BoottomCenter = object : Alignment {}
        val BoottomEnd = object : Alignment {}

        val Top = object : Alignment.Vertical {}
        val CenterVertically = object : Alignment.Vertical {}
        val Bottom = object : Alignment.Vertical {}

        val Start = object : Alignment.Horizontal {}
        val CenterHorizontally = object : Alignment.Horizontal {}
        val End = object : Alignment.Horizontal {}
    }
}
