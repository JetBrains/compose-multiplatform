package org.jetbrains.compose.kapp

import androidx.compose.runtime.Composable

interface KAppScope

@Composable
expect fun KAppScope.Frame(content: @Composable () -> Unit)

// Complex multi-frame application
fun kapp(name: String = "application", title: String = "Compose Application", content: @Composable KAppScope.() -> Unit) {
    kappImpl(name, title, content)
}

// Simple single-frame application.
fun simpleKapp(name: String = "application", content: @Composable () -> Unit) {
    simpleKappImpl(name, content)
}

internal expect fun kappImpl(name: String, title: String, content: @Composable KAppScope.() -> Unit)
internal expect fun simpleKappImpl(name: String, content: @Composable () -> Unit)
