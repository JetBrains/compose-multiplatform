/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3.demos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun ColorSchemeDemo() {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.padding(8.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("Surfaces", style = MaterialTheme.typography.bodyLarge)
            SurfaceColorSwatch(
                surface = colorScheme.background,
                surfaceText = "Background",
                onSurface = colorScheme.onBackground,
                onSurfaceText = "On Background"
            )
            Spacer(modifier = Modifier.height(16.dp))
            SurfaceColorSwatch(
                surface = colorScheme.surface,
                surfaceText = "Surface",
                onSurface = colorScheme.onSurface,
                onSurfaceText = "On Surface"
            )
            Spacer(modifier = Modifier.height(16.dp))
            SurfaceColorSwatch(
                surface = colorScheme.surfaceVariant,
                surfaceText = "Surface Variant",
                onSurface = colorScheme.onSurfaceVariant,
                onSurfaceText = "On Surface Variant"
            )
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Inverse Surface",
                        color = colorScheme.inverseSurface,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Inverse On Surface",
                        color = colorScheme.inverseOnSurface,
                    )
                },
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Inverse Primary",
                        color = colorScheme.inversePrimary,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Surface Tint",
                        color = colorScheme.surfaceTint,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("Content", style = MaterialTheme.typography.bodyLarge)
            ContentColorSwatch(
                color = colorScheme.primary,
                colorText = "Primary",
                onColor = colorScheme.onPrimary,
                onColorText = "On Primary",
                colorContainer = colorScheme.primaryContainer,
                colorContainerText = "Primary Container",
                onColorContainer = colorScheme.onPrimaryContainer,
                onColorContainerText = "On Primary Container")
            Spacer(modifier = Modifier.height(16.dp))
            ContentColorSwatch(
                color = colorScheme.secondary,
                colorText = "Secondary",
                onColor = colorScheme.onSecondary,
                onColorText = "On Secondary",
                colorContainer = colorScheme.secondaryContainer,
                colorContainerText = "Secondary Container",
                onColorContainer = colorScheme.onSecondaryContainer,
                onColorContainerText = "On Secondary Container")
            Spacer(modifier = Modifier.height(16.dp))
            ContentColorSwatch(
                color = colorScheme.tertiary,
                colorText = "Tertiary",
                onColor = colorScheme.onTertiary,
                onColorText = "On Tertiary",
                colorContainer = colorScheme.tertiaryContainer,
                colorContainerText = "Tertiary Container",
                onColorContainer = colorScheme.onTertiaryContainer,
                onColorContainerText = "On Tertiary Container")
            Spacer(modifier = Modifier.height(16.dp))
            ContentColorSwatch(
                color = colorScheme.error,
                colorText = "Error",
                onColor = colorScheme.onError,
                onColorText = "On Error",
                colorContainer = colorScheme.errorContainer,
                colorContainerText = "Error Container",
                onColorContainer = colorScheme.onErrorContainer,
                onColorContainerText = "On Error Container")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Utility", style = MaterialTheme.typography.bodyLarge)
            ColorTile(
                text = "Outline",
                color = colorScheme.outline,
            )
        }
    }
}

@Composable
private fun SurfaceColorSwatch(
    surface: Color,
    surfaceText: String,
    onSurface: Color,
    onSurfaceText: String
) {
    ColorTile(
        text = surfaceText,
        color = surface,
    )
    ColorTile(
        text = onSurfaceText,
        color = onSurface,
    )
}

@Composable
private fun ContentColorSwatch(
    color: Color,
    colorText: String,
    onColor: Color,
    onColorText: String,
    colorContainer: Color,
    colorContainerText: String,
    onColorContainer: Color,
    onColorContainerText: String,
) {
    DoubleTile(
        leftTile = {
            ColorTile(
                text = colorText,
                color = color
            )
        },
        rightTile = {
            ColorTile(
                text = onColorText,
                color = onColor,
            )
        },
    )
    DoubleTile(
        leftTile = {
            ColorTile(
                text = colorContainerText,
                color = colorContainer,
            )
        },
        rightTile = {
            ColorTile(
                text = onColorContainerText,
                color = onColorContainer,
            )
        },
    )
}

@Composable
private fun DoubleTile(leftTile: @Composable () -> Unit, rightTile: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f)) { leftTile() }
        Box(modifier = Modifier.weight(1f)) { rightTile() }
    }
}

@Composable
private fun ColorTile(text: String, color: Color) {
    var borderColor = Color.Transparent
    if (color == Color.Black) {
        borderColor = Color.White
    } else if (color == Color.White) borderColor = Color.Black

    Surface(
        modifier = Modifier.height(48.dp).fillMaxWidth(),
        color = color,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text,
            Modifier.padding(4.dp),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    if (color.luminance() < .25) Color.White else Color.Black
                )
        )
    }
}
