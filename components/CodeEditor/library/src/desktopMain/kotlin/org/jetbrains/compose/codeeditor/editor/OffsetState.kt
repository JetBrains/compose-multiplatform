package org.jetbrains.compose.codeeditor.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset

@Stable
internal class OffsetState {
    companion object {
        val Unspecified = IntOffset(-1, -1)
    }

    var value by mutableStateOf(Unspecified)
        private set

    fun setX(x: Int) {
        value = value.copy(x = x)
    }

    fun setY(y: Int) {
        value = value.copy(y = y)
    }

    fun set(offset: IntOffset) {
        value = offset
    }
}

