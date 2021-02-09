/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.demos

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.samples.ButtonSample
import androidx.compose.material.samples.ButtonWithIconSample
import androidx.compose.material.samples.FluidExtendedFab
import androidx.compose.material.samples.IconButtonSample
import androidx.compose.material.samples.IconToggleButtonSample
import androidx.compose.material.samples.OutlinedButtonSample
import androidx.compose.material.samples.SimpleExtendedFabNoIcon
import androidx.compose.material.samples.SimpleExtendedFabWithIcon
import androidx.compose.material.samples.SimpleFab
import androidx.compose.material.samples.TextButtonSample
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DefaultSpace = 20.dp

@Composable
fun ButtonDemo() {
    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(DefaultSpace)
    ) {
        item {
            Buttons()
        }
        item {
            Fabs()
        }
        item {
            IconButtons()
        }
        item {
            CustomShapeButton()
        }
    }
}

@Composable
private fun Buttons() {
    Text("Buttons")
    Spacer(Modifier.height(DefaultSpace))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ButtonSample()
        OutlinedButtonSample()
        TextButtonSample()
    }

    Spacer(Modifier.height(DefaultSpace))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary
            )
        ) {
            Text("Secondary Color")
        }
        ButtonWithIconSample()
    }

    Spacer(Modifier.height(DefaultSpace))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Button(onClick = {}, enabled = false) {
            Text("Disabled")
        }
        OutlinedButton(onClick = {}, enabled = false) {
            Text("Disabled")
        }
        TextButton(onClick = {}, enabled = false) {
            Text("Disabled")
        }
    }
}

@Composable
private fun Fabs() {
    Text("Floating action buttons")
    Spacer(Modifier.height(DefaultSpace))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        SimpleFab()
        SimpleExtendedFabNoIcon()
        SimpleExtendedFabWithIcon()
    }
    Spacer(Modifier.height(DefaultSpace))
    FluidExtendedFab()
}

@Composable
private fun IconButtons() {
    Text("Icon buttons")
    Spacer(Modifier.height(DefaultSpace))

    Row {
        IconButtonSample()
        IconToggleButtonSample()
        IconToggleButtonDisabled()
    }
}

@Composable
private fun CustomShapeButton() {
    Text("Custom shape button")
    Spacer(Modifier.height(DefaultSpace))
    OutlinedButton(
        onClick = {},
        modifier = Modifier.size(110.dp),
        shape = TriangleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Yellow
        ),
        border = BorderStroke(width = 2.dp, color = Color.Black)
    ) {
        Column {
            Text("Click")
            Text("here")
        }
    }
}

@Composable
private fun IconToggleButtonDisabled() {
    var checked by remember { mutableStateOf(false) }

    IconToggleButton(checked = checked, enabled = false, onCheckedChange = { checked = it }) {
        val tint by animateColorAsState(if (checked) Color(0xFFEC407A) else Color(0xFFB0BEC5))
        Icon(Icons.Filled.Favorite, contentDescription = "Favorite", tint = tint)
    }
}

private val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
}
