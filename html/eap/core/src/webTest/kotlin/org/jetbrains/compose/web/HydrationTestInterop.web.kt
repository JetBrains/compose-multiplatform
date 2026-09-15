package org.jetbrains.compose.web

internal data class HydrationTestState(
    val label: String,
    val count: Int,
)

internal expect suspend fun fetchHydrationFixtureText(input: String): String

internal expect fun decodeHydrationTestState(json: String): HydrationTestState

internal expect fun setWindowIntProperty(name: String, value: Int?)

internal expect fun getWindowIntProperty(name: String): Int
