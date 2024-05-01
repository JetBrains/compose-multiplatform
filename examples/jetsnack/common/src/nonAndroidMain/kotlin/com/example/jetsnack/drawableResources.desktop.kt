package com.example.jetsnack

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.example.jetsnack.MppR
import com.example.jetsnack.ui.myiconpack.EmptyStateSearch
import org.jetbrains.skiko.currentNanoTime

@Composable
actual fun painterResource(id: Int): Painter {
    return when(id) {
        MppR.drawable.empty_state_search -> rememberVectorPainter(EmptyStateSearch)
        else -> TODO()
    }
}

private var lastId = currentNanoTime().toInt()


private val _empty_state_search = lastId++
actual val MppR.drawable.empty_state_search: Int get() = _empty_state_search