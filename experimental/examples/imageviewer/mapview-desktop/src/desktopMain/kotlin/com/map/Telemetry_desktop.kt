package com.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal actual fun Telemetry(state: InternalMapState) {
    Column(Modifier.background(Color(0x77ffFFff))) {
        Text(state.toShortString())
    }
}
