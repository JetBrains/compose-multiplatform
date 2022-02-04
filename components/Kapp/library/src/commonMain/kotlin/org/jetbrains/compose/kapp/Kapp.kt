package org.jetbrains.compose.kapp

import androidx.compose.runtime.Composable

interface KAppScope

interface FrameScope {
    val widthPixels: Int
    val heightPixels: Int
    val density: Float
}

@Composable
expect fun KAppScope.Frame(content: @Composable FrameScope.() -> Unit)

// Complex multi-frame application
fun kapp(name: String = "application", title: String = "Compose Application", content: @Composable KAppScope.() -> Unit) {
    kappImpl(name, title, content)
}

// Simple single-frame application.
fun simpleKapp(name: String = "application", content: @Composable FrameScope.() -> Unit) {
    simpleKappImpl(name, content)
}

internal expect fun kappImpl(name: String, title: String, content: @Composable KAppScope.() -> Unit)
internal expect fun simpleKappImpl(name: String, content: @Composable FrameScope.() -> Unit)
