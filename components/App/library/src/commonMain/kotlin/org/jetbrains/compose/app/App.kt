package org.jetbrains.compose.app

import androidx.compose.runtime.Composable

interface ComposeAppScope

@Composable
expect fun ComposeAppScope.Frame(content: @Composable () -> Unit)

// Complex multi-frame application
fun composeApplication(name: String = "application", title: String = "Compose Application", content: @Composable ComposeAppScope.() -> Unit) {
    composeApplicationImpl(name, title, content)
}

// Simple single-frame application.
fun composeApp(name: String = "application", content: @Composable () -> Unit) {
    composeAppImpl(name, content)
}

internal expect fun composeApplicationImpl(name: String, title: String, content: @Composable ComposeAppScope.() -> Unit)
internal expect fun composeAppImpl(name: String, content: @Composable () -> Unit)
