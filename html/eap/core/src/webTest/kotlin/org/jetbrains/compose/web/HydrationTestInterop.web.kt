package org.jetbrains.compose.web

import kotlinx.browser.dom.Element

internal data class HydrationTestState(
    val label: String,
    val count: Int,
)

internal expect fun decodeHydrationTestState(json: String): HydrationTestState

internal expect fun setWindowIntProperty(name: String, value: Int)

internal expect fun clearWindowIntProperty(name: String)

internal expect fun getWindowIntProperty(name: String): Int

internal expect fun setNoncePropertyForTest(element: Element, value: String)

internal expect fun hideNoncePropertyForTest(element: Element)
