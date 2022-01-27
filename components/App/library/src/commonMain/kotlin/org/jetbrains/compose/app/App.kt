package org.jetbrains.compose.app

import androidx.compose.runtime.Composable

fun app(name: String = "application", title: String = "Compose Application", body: @Composable () -> Unit) {
    appImpl(name, title, body)
}

internal expect fun appImpl(name: String, title: String, body: @Composable () -> Unit)

// TODO: not sure if having abstract here is a good idea
expect abstract class EmbedderContext

expect fun embed(context: EmbedderContext,  body: @Composable () -> Unit)