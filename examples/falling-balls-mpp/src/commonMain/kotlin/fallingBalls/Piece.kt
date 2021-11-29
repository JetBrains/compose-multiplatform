package org.jetbrains.compose.demo.falling.views

import androidx.compose.demo.*
import org.jetbrains.compose.demo.falling.*
import org.jetbrains.compose.demo.falling.views.*

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape

@Composable
fun Piece(index: Int, piece: PieceData) {
    println("$index: ${piece.position}\t$piece")
    val boxSize = 40.dp
    Box(
        Modifier
            .position(Dp(boxSize.value * index * 5 / 3), Dp(piece.position))
            .size(boxSize, boxSize)
            .background(if (piece.picked) Color.Gray else piece.color)
            .clickable { piece.pick() }
            .clip(CircleShape)
    ) {}
}
