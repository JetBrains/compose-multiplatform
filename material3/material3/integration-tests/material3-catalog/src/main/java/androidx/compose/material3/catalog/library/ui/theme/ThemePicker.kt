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

package androidx.compose.material3.catalog.library.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.catalog.library.model.ColorMode
import androidx.compose.material3.catalog.library.model.TextDirection
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.model.ThemeMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

// TODO: Use components/values from Material3 when available
@Composable
@Suppress("UNUSED_PARAMETER")
fun ThemePicker(
    theme: Theme,
    onThemeChange: (theme: Theme) -> Unit
) {
    LazyColumn {
        item {
            Text(
                text = stringResource(id = R.string.theme),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            // LazyVerticalGrid can't be used within LazyColumn due to nested scrolling
            val themeModes = ThemeMode.values()
            Column(modifier = Modifier.padding(ThemePickerPadding)) {
                Row {
                    ThemeModeItem(
                        modifier = Modifier.weight(1f),
                        themeMode = themeModes[0],
                        selected = themeModes[0] == theme.themeMode,
                        onClick = { onThemeChange(theme.copy(themeMode = it)) }
                    )
                    Spacer(modifier = Modifier.width(ThemePickerPadding))
                    ThemeModeItem(
                        modifier = Modifier.weight(1f),
                        themeMode = themeModes[1],
                        selected = themeModes[1] == theme.themeMode,
                        onClick = { onThemeChange(theme.copy(themeMode = it)) }
                    )
                }
                Spacer(modifier = Modifier.height(ThemePickerPadding))
                Row {
                    ThemeModeItem(
                        modifier = Modifier.weight(1f),
                        themeMode = themeModes[2],
                        selected = themeModes[2] == theme.themeMode,
                        onClick = { onThemeChange(theme.copy(themeMode = it)) }
                    )
                }
            }
            Divider(modifier = Modifier.padding(horizontal = ThemePickerPadding))
            Spacer(modifier = Modifier.height(ThemePickerPadding))
        }
        item {
            Text(
                text = stringResource(id = R.string.color_mode),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            // LazyVerticalGrid can't be used within LazyColumn due to nested scrolling
            val colorModes = ColorMode.values()
            Column(modifier = Modifier.padding(ThemePickerPadding)) {
                Row {
                    ColorModeItem(
                        modifier = Modifier.weight(1f),
                        colorMode = colorModes[0],
                        selected = colorModes[0] == theme.colorMode,
                        onClick = { onThemeChange(theme.copy(colorMode = it)) }
                    )
                    Spacer(modifier = Modifier.width(ThemePickerPadding))
                    ColorModeItem(
                        modifier = Modifier.weight(1f),
                        colorMode = colorModes[1],
                        selected = colorModes[1] == theme.colorMode,
                        onClick = { onThemeChange(theme.copy(colorMode = it)) }
                    )
                }
                Spacer(modifier = Modifier.height(ThemePickerPadding))
                Row {
                    ColorModeItem(
                        modifier = Modifier.weight(1f),
                        colorMode = colorModes[2],
                        selected = colorModes[2] == theme.colorMode,
                        onClick = { onThemeChange(theme.copy(colorMode = it)) }
                    )
                }
            }
            Divider(modifier = Modifier.padding(horizontal = ThemePickerPadding))
            Spacer(modifier = Modifier.height(ThemePickerPadding))
        }
        item {
            Text(
                text = stringResource(id = R.string.text_scale),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            var fontScale by remember { mutableStateOf(theme.fontScale) }
            Slider(
                value = fontScale,
                onValueChange = { fontScale = it },
                onValueChangeFinished = { onThemeChange(theme.copy(fontScale = fontScale)) },
                valueRange = 0f..4f,
            )
        }
        item {
            Text(
                text = stringResource(id = R.string.text_direction),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            // LazyVerticalGrid can't be used within LazyColumn due to nested scrolling
            val textDirections = TextDirection.values()
            Column(modifier = Modifier.padding(ThemePickerPadding)) {
                Row {
                    TextDirectionItem(
                        modifier = Modifier.weight(1f),
                        textDirection = textDirections[0],
                        selected = textDirections[0] == theme.textDirection,
                        onClick = { onThemeChange(theme.copy(textDirection = it)) }
                    )
                    Spacer(modifier = Modifier.width(ThemePickerPadding))
                    TextDirectionItem(
                        modifier = Modifier.weight(1f),
                        textDirection = textDirections[1],
                        selected = textDirections[1] == theme.textDirection,
                        onClick = { onThemeChange(theme.copy(textDirection = it)) }
                    )
                }
                Spacer(modifier = Modifier.height(ThemePickerPadding))
                Row {
                    TextDirectionItem(
                        modifier = Modifier.weight(1f),
                        textDirection = textDirections[2],
                        selected = textDirections[2] == theme.textDirection,
                        onClick = { onThemeChange(theme.copy(textDirection = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeItem(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    selected: Boolean,
    onClick: (ThemeMode) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = { onClick(themeMode) },
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.secondary,
                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            ),
        )
        Spacer(modifier = Modifier.width(ThemePickerPadding))
        Text(
            text = themeMode.toString(),
        )
    }
}

@Composable
private fun ColorModeItem(
    modifier: Modifier = Modifier,
    colorMode: ColorMode,
    selected: Boolean,
    onClick: (ColorMode) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = { onClick(colorMode) },
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.secondary,
                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            ),
        )
        Spacer(modifier = Modifier.width(ThemePickerPadding))
        Text(
            text = colorMode.toString(),
        )
    }
}

@Composable
private fun TextDirectionItem(
    modifier: Modifier = Modifier,
    textDirection: TextDirection,
    selected: Boolean,
    onClick: (TextDirection) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = { onClick(textDirection) },
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.secondary,
                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            ),
        )
        Spacer(modifier = Modifier.width(ThemePickerPadding))
        Text(
            text = textDirection.toString(),
        )
    }
}

private val ThemePickerPadding = 16.dp
