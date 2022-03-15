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
            ColorTile(
                text = "On Background",
                color = colorScheme.onBackground,
            )
            ColorTile(
                text = "Background",
                color = colorScheme.background,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "On Surface",
                        color = colorScheme.onSurface,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "On Surface Variant",
                        color = colorScheme.onSurfaceVariant,
                    )
                },
            )
            ColorTile(text = "Surface", color = colorScheme.surface)
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Inverse Primary",
                        color = colorScheme.inversePrimary,
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
                        text = "Surface Variant",
                        color = colorScheme.surfaceVariant,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Inverse Surface",
                        color = colorScheme.inverseSurface,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Surface Tint",
                        color = colorScheme.surfaceTint,
                    )
                },
                rightTile = { Box(Modifier.fillMaxWidth()) },
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("Content", style = MaterialTheme.typography.bodyLarge)
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "On Primary Container",
                        color = colorScheme.onPrimaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "On Primary",
                        color = colorScheme.onPrimary,
                    )
                },
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Primary Container",
                        color = colorScheme.primaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Primary",
                        color = colorScheme.primary,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "On Secondary Container",
                        color = colorScheme.onSecondaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "On Secondary",
                        color = colorScheme.onSecondary,
                    )
                },
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Secondary Container",
                        color = colorScheme.secondaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Secondary",
                        color = colorScheme.secondary,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "On Tertiary Container",
                        color = colorScheme.onTertiaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "On Tertiary",
                        color = colorScheme.onTertiary,
                    )
                },
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Tertiary Container",
                        color = colorScheme.tertiaryContainer,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Tertiary",
                        color = colorScheme.tertiary,
                    )
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Utility", style = MaterialTheme.typography.bodyLarge)
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "On Error",
                        color = colorScheme.onError,
                    )
                },
                rightTile = {
                    ColorTile(
                        text = "Outline",
                        color = colorScheme.outline,
                    )
                }
            )
            DoubleTile(
                leftTile = {
                    ColorTile(
                        text = "Error",
                        color = colorScheme.error,
                    )
                },
                rightTile = { Box(Modifier.fillMaxWidth()) },
            )
        }
    }
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
