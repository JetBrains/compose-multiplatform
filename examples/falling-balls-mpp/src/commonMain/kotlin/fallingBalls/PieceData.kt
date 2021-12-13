/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

data class PieceData constructor(val game: Game, val velocity: Float, val color: Color) {
    var picked: Boolean by mutableStateOf(false)
    var position: Float by mutableStateOf(0f)

    private fun Game.pickPiece(piece: PieceData) {
        score += piece.velocity.toInt()
        clicked++
        if (clicked == numBlocks) {
            finished = true
        }
    }

    fun update(dt: Long) {
        if (picked) return
        val delta = (dt / 1E8 * velocity).toFloat()
        position = if (game.isInBoundaries(this)) position + delta else 0f
    }

    fun pick() {
        if (!picked && !game.paused) {
            picked = true
            game.pickPiece(this)
        }
    }
}
