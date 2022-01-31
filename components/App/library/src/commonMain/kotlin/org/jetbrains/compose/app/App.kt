package org.jetbrains.compose.app

import androidx.compose.runtime.Composable

interface AppScope

@Composable
expect fun AppScope.Frame(content: @Composable () -> Unit)

fun app(name: String = "application", title: String = "Compose Application", content: @Composable AppScope.() -> Unit) {
    appImpl(name, title, content)
}

internal expect fun appImpl(name: String, title: String, content: @Composable AppScope.() -> Unit)

expect fun embed(context: Any,  body: @Composable () -> Unit)