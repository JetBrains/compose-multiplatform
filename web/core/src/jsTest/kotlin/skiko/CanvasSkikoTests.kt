package org.jetbrains.compose.web.core.tests

import androidx.compose.ui.window.ComposeCanvas
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.testutils.*
import org.w3c.dom.HTMLCanvasElement
import kotlin.test.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import org.jetbrains.compose.web.core.tests.AppStylesheet.attr
import org.w3c.dom.CanvasRenderingContext2D


class CanvasSkikoTests  {
    @Test
    fun canvasNoSkikoTest() = runTest {
        composition {
            Canvas {
                attr("height", "60")
                attr("width", "60")

                DomSideEffect { canvas ->
                    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
                    ctx.beginPath()
                    ctx.rect(20.0, 20.0, 40.0, 40.0)
                    ctx.stroke()
                }
            }
        }

        val domCanvas = document.createElement("canvas") as HTMLCanvasElement
        domCanvas.width = 60
        domCanvas.height = 60
        val ctx = domCanvas.getContext("2d") as CanvasRenderingContext2D
        ctx.beginPath()
        ctx.rect(10.0, 10.0, 30.0, 30.0)
        ctx.stroke()

        val canvas = nextChild() as HTMLCanvasElement
        assertEquals(domCanvas.toDataURL(), canvas.toDataURL())
    }


    @Test
    fun canvasSkikoTest() = runTest {
        composition {
            Canvas() {
                DomSideEffect { canvas ->

                    ComposeCanvas(canvas).apply {
                        setContent {
                            Button(
                                modifier = Modifier.padding(16.dp),
                                onClick = {
                                    println("Button clicked!")
                                   // switched = !switched
                                }
                            ) {
                                //Text(if (switched) "🦑 press 🐙" else "Press me!")
                                Text("Press me!")
                            }
                        }

                     }
                }
            }
        }

        val canvas = nextChild() as HTMLCanvasElement
        assertEquals("xxx", canvas.toDataURL())
    }
}