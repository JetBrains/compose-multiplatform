/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow

@Composable
fun Piece(index: Int, piece: PieceData) {
    val boxSize = 40.dp
    Box(
        Modifier
            .offset(boxSize * index * 5 / 3, piece.position.dp)
            .shadow(30.dp)
            .clip(CircleShape)
    ) {
        Box(
            Modifier
                .size(boxSize, boxSize)
                .background(if (piece.picked) Color.Gray else piece.color)
                .clickable(onClick = { piece.pick() })
        )
    }
}