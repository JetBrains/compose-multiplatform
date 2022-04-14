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

package androidx.compose.material.catalog.library.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.catalog.library.R
import androidx.compose.material.catalog.library.model.DefaultTheme
import androidx.compose.material.catalog.library.model.MaxLargeShapeCornerSize
import androidx.compose.material.catalog.library.model.MaxMediumShapeCornerSize
import androidx.compose.material.catalog.library.model.MaxSmallShapeCornerSize
import androidx.compose.material.catalog.library.model.Theme
import androidx.compose.material.catalog.library.model.ThemeColor
import androidx.compose.material.catalog.library.model.ThemeFontFamily
import androidx.compose.material.catalog.library.model.ThemeShapeCornerFamily
import androidx.compose.material.catalog.library.model.getColor
import androidx.compose.material.catalog.library.model.getFontFamily
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ThemePicker(
    theme: Theme,
    onThemeChange: (theme: Theme) -> Unit
) {
    var themeState by remember { mutableStateOf(theme) }

    LazyColumn(
        contentPadding = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            .add(
                WindowInsets(
                    top = ThemePickerPadding,
                    bottom = ThemePickerPadding
                )
            )
            .asPaddingValues()
    ) {
        item {
            Text(
                text = stringResource(id = R.string.theming_options),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            Spacer(modifier = Modifier.height(ThemePickerPadding))
        }
        item {
            Text(
                text = stringResource(id = R.string.primary_color),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            LazyRow(contentPadding = PaddingValues(end = ThemePickerPadding)) {
                items(ThemeColor.values()) { themeColor ->
                    ThemeColorItem(
                        modifier = Modifier.padding(
                            start = ThemePickerPadding,
                            top = ThemePickerPadding,
                            bottom = ThemePickerPadding
                        ),
                        themeColor = themeColor,
                        selected = { it == themeState.primaryColor },
                        onClick = { themeState = themeState.copy(primaryColor = it) }
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(id = R.string.secondary_color),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            LazyRow(contentPadding = PaddingValues(end = ThemePickerPadding)) {
                items(ThemeColor.values()) { themeColor ->
                    ThemeColorItem(
                        modifier = Modifier.padding(
                            start = ThemePickerPadding,
                            top = ThemePickerPadding,
                            bottom = ThemePickerPadding
                        ),
                        themeColor = themeColor,
                        selected = { it == themeState.secondaryColor },
                        onClick = { themeState = themeState.copy(secondaryColor = it) }
                    )
                }
            }
            Divider(modifier = Modifier.padding(horizontal = ThemePickerPadding))
            Spacer(modifier = Modifier.height(ThemePickerPadding))
        }
        item {
            Text(
                text = stringResource(id = R.string.font_family),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            // LazyVerticalGrid can't be used within LazyColumn due to nested scrolling
            val themeFontFamilies = ThemeFontFamily.values()
            Column(modifier = Modifier.padding(ThemePickerPadding)) {
                Row {
                    ThemeFontFamilyItem(
                        modifier = Modifier.weight(1f),
                        themeFontFamily = themeFontFamilies[0],
                        selected = { it == themeState.fontFamily },
                        onClick = { themeState = themeState.copy(fontFamily = it) }
                    )
                    Spacer(modifier = Modifier.width(ThemePickerPadding))
                    ThemeFontFamilyItem(
                        modifier = Modifier.weight(1f),
                        themeFontFamily = themeFontFamilies[1],
                        selected = { it == themeState.fontFamily },
                        onClick = { themeState = themeState.copy(fontFamily = it) }
                    )
                }
                Spacer(modifier = Modifier.height(ThemePickerPadding))
                Row {
                    ThemeFontFamilyItem(
                        modifier = Modifier.weight(1f),
                        themeFontFamily = themeFontFamilies[2],
                        selected = { it == themeState.fontFamily },
                        onClick = { themeState = themeState.copy(fontFamily = it) }
                    )
                    Spacer(modifier = Modifier.width(ThemePickerPadding))
                    ThemeFontFamilyItem(
                        modifier = Modifier.weight(1f),
                        themeFontFamily = themeFontFamilies[3],
                        selected = { it == themeState.fontFamily },
                        onClick = { themeState = themeState.copy(fontFamily = it) }
                    )
                }
                Spacer(modifier = Modifier.height(ThemePickerPadding))
                Row {
                    ThemeFontFamilyItem(
                        modifier = Modifier.weight(1f),
                        themeFontFamily = themeFontFamilies[4],
                        selected = { it == themeState.fontFamily },
                        onClick = { themeState = themeState.copy(fontFamily = it) }
                    )
                }
            }
            Divider(modifier = Modifier.padding(horizontal = ThemePickerPadding))
            Spacer(modifier = Modifier.height(ThemePickerPadding))
        }
        item {
            Text(
                text = stringResource(id = R.string.shape_corner_family),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            LazyRow(
                contentPadding = PaddingValues(
                    start = ThemePickerPadding,
                    top = ThemePickerPadding,
                    bottom = ThemePickerPadding
                )
            ) {
                items(ThemeShapeCornerFamily.values()) { themeShapeCornerFamily ->
                    ThemeShapeCornerFamilyItem(
                        modifier = Modifier.padding(end = ThemePickerPadding),
                        themeShapeCornerFamily = themeShapeCornerFamily,
                        selected = { it == themeState.shapeCornerFamily },
                        onClick = { themeState = themeState.copy(shapeCornerFamily = it) }
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(id = R.string.small_shape_corner_size),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            ThemeShapeCornerSizeItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ThemePickerPadding),
                themeShapeCornerSize = themeState.smallShapeCornerSize,
                themeShapeCornerSizeMax = MaxSmallShapeCornerSize,
                onValueChange = { themeState = themeState.copy(smallShapeCornerSize = it) }
            )
            Spacer(modifier = Modifier.height(ThemePickerPadding))
        }
        item {
            Text(
                text = stringResource(id = R.string.medium_shape_corner_size),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            ThemeShapeCornerSizeItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ThemePickerPadding),
                themeShapeCornerSize = themeState.mediumShapeCornerSize,
                themeShapeCornerSizeMax = MaxMediumShapeCornerSize,
                onValueChange = { themeState = themeState.copy(mediumShapeCornerSize = it) }
            )
            Spacer(modifier = Modifier.height(ThemePickerPadding))
        }
        item {
            Text(
                text = stringResource(id = R.string.large_shape_corner_size),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            ThemeShapeCornerSizeItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ThemePickerPadding),
                themeShapeCornerSize = themeState.largeShapeCornerSize,
                themeShapeCornerSizeMax = MaxLargeShapeCornerSize,
                onValueChange = { themeState = themeState.copy(largeShapeCornerSize = it) }
            )
            Spacer(modifier = Modifier.height(ThemePickerPadding))
            Divider()
            Spacer(modifier = Modifier.height(ThemePickerPadding))
        }
        item {
            Row(modifier = Modifier.padding(horizontal = ThemePickerPadding)) {
                Button(
                    onClick = {
                        onThemeChange(themeState)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = themeState != theme,
                    elevation = null
                ) {
                    Text(text = stringResource(R.string.apply))
                }
                Spacer(modifier = Modifier.width(ThemePickerPadding))
                OutlinedButton(
                    onClick = {
                        themeState = DefaultTheme
                        onThemeChange(themeState)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = theme != DefaultTheme
                ) {
                    Text(text = stringResource(R.string.reset))
                }
            }
        }
    }
}

@Composable
private fun ThemeColorItem(
    modifier: Modifier = Modifier,
    themeColor: ThemeColor,
    selected: (themeColor: ThemeColor) -> Boolean,
    onClick: (themeColor: ThemeColor) -> Unit
) {
    val darkTheme = !MaterialTheme.colors.isLight
    RadioButton(
        selected = selected(themeColor),
        onClick = { onClick(themeColor) },
        modifier = modifier,
        colors = RadioButtonDefaults.colors(
            selectedColor = themeColor.getColor(darkTheme),
            unselectedColor = themeColor.getColor(darkTheme)
        )
    )
}

@Composable
private fun ThemeFontFamilyItem(
    modifier: Modifier = Modifier,
    themeFontFamily: ThemeFontFamily,
    selected: (themeFontFamily: ThemeFontFamily) -> Boolean,
    onClick: (themeFontFamily: ThemeFontFamily) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected(themeFontFamily),
            onClick = { onClick(themeFontFamily) }
        )
        Spacer(modifier = Modifier.width(ThemePickerPadding))
        Text(
            text = themeFontFamily.label,
            style = MaterialTheme.typography.body2.copy(
                fontFamily = themeFontFamily.getFontFamily()
            )
        )
    }
}

@Composable
private fun ThemeShapeCornerFamilyItem(
    modifier: Modifier = Modifier,
    themeShapeCornerFamily: ThemeShapeCornerFamily,
    selected: (themeShapeCornerFamily: ThemeShapeCornerFamily) -> Boolean,
    onClick: (themeShapeCornerFamily: ThemeShapeCornerFamily) -> Unit
) {
    TextButton(
        onClick = { onClick(themeShapeCornerFamily) },
        modifier = modifier,
        colors = if (selected(themeShapeCornerFamily)) {
            ButtonDefaults.textButtonColors()
        } else {
            ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
            )
        }
    ) {
        val iconId = when (themeShapeCornerFamily) {
            ThemeShapeCornerFamily.Rounded -> R.drawable.ic_shape_rounded_corner_24dp
            ThemeShapeCornerFamily.Cut -> R.drawable.ic_shape_cut_corner_24dp
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(iconId),
                contentDescription = themeShapeCornerFamily.label
            )
            Spacer(modifier = Modifier.width(ThemePickerPadding))
            Text(text = themeShapeCornerFamily.label)
        }
    }
}

@Composable
private fun ThemeShapeCornerSizeItem(
    modifier: Modifier = Modifier,
    themeShapeCornerSize: Int,
    themeShapeCornerSizeMax: Int,
    onValueChange: (themeShapeCornerSize: Int) -> Unit
) {
    Column(modifier = modifier) {
        Slider(
            value = themeShapeCornerSize.toFloat(),
            onValueChange = { value -> onValueChange(value.toInt()) },
            valueRange = 0f..themeShapeCornerSizeMax.toFloat(),
        )
        Text(
            text = stringResource(id = R.string.dp, themeShapeCornerSize),
            style = MaterialTheme.typography.body2
        )
    }
}

private val ThemePickerPadding = 16.dp
