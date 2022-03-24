package com.jetbrains.compose.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.WindowPosition

@Composable
fun ColorPallet(initColor: Long, onSelect: (Long) -> Unit) {
  Column {
    Row {
      listOf(Color.Red, Color.Green, Color.Blue, Color.Black, Color.Gray, Color.Yellow, Color.Cyan).forEach {
        val width = 40f
        val height = 40f
        Canvas(Modifier.size(width.dp, height.dp).clickable {
          onSelect((it.value shr 32).toLong())
        }) {
          drawRect(color = it, size = Size(width, height))
        }
      }
    }
    Divider(Modifier.size(5.dp))
    var currentColor: Color by remember { mutableStateOf(Color(initColor)) }

    Canvas(Modifier.size(50.dp, 50.dp).clickable {
      onSelect((currentColor.toArgb().toUInt()).toLong())
    }) {
      drawRect(currentColor, size = Size(50f, 50f))
    }
    Divider(Modifier.size(5.dp))
    Row {
      Canvas(Modifier.size(256.dp, 256.dp).pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent()
            if (event.buttons.isPrimaryPressed) {
              currentColor = currentColor.copy(red = event.changes.first().position.x / 256)
              currentColor = currentColor.copy(green = event.changes.first().position.y / 256)
            }
          }
        }
      }) {
        for (r in 0..0xFF) {
          for (g in 0..0xFF) {
            drawRect(
              color = currentColor.copy(red = r / 255f, green = g / 255f),
              topLeft = Offset(r.toFloat(), g.toFloat()),
              size = Size(1f, 1f)
            )
          }
        }
      }
      val BAND_WIDTH = 40
      Canvas(Modifier.size(BAND_WIDTH.dp, 256.dp).pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent()
            if (event.buttons.isPrimaryPressed) {
              currentColor = currentColor.copy(blue = event.changes.first().position.y / 256)
            }
          }
        }
      }) {
        for (b in 0..0xFF) {
          drawRect(color = Color(0, 0, b), topLeft = Offset(0f, b.toFloat()), size = Size(BAND_WIDTH.toFloat(), 1f))
        }
      }
    }

  }
}


