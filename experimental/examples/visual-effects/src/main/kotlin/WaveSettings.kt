/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.demo.visuals

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal class ColorSettings {
    var enabled by mutableStateOf(true)
    var waveLength by mutableStateOf(30.0)
    var simple by mutableStateOf(true)
    var period by mutableStateOf(80.0)
}

internal class SettingsState {
    companion object {
        var red by mutableStateOf(ColorSettings())
        var green by mutableStateOf(ColorSettings())
        var blue by mutableStateOf(ColorSettings())
    }
}

@Composable
internal fun SettingsPanel(settings: ColorSettings, name: String) {
    Row {
        Text(name)
        Checkbox(settings.enabled, onCheckedChange = { settings.enabled = it })
        Checkbox(settings.simple, onCheckedChange = { settings.simple = it })
        Slider(
            (settings.waveLength.toFloat() - 10) / 90,
            { settings.waveLength = 10 + 90 * it.toDouble() },
            Modifier.width(100.dp)
        )
        Slider(
            (settings.period.toFloat() - 10) / 90,
            { settings.period = 10 + 90 * it.toDouble() },
            Modifier.width(100.dp)
        )
    }
}
