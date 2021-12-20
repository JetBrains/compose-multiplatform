package org.jetbrains.compose.web.core.tests

import kotlin.test.*
import org.jetbrains.compose.web.testutils.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.skiko.ping

class CanvasSkikoTests  {
    @Test
    fun canvasSkikoTest() = runTest() {
        composition {
            Canvas() {
                ping()
            }
        }
    }
}