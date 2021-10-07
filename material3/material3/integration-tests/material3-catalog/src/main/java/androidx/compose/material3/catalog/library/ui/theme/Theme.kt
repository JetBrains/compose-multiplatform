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

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.catalog.library.model.ColorMode
import androidx.compose.material3.catalog.library.model.TextDirection
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.model.ThemeMode
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@Composable
@Suppress("UNUSED_PARAMETER")
fun CatalogTheme(
    theme: Theme,
    content: @Composable () -> Unit
) {
    // TODO(b/201804011): Add sampleDynamicColorScheme
    val colorScheme =
        if (theme.colorMode == ColorMode.TrueDynamic &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            when (theme.themeMode) {
                ThemeMode.Light -> dynamicLightColorScheme(context)
                ThemeMode.Dark -> dynamicDarkColorScheme(context)
                ThemeMode.System ->
                    if (!isSystemInDarkTheme()) {
                        dynamicLightColorScheme(context)
                    } else {
                        dynamicDarkColorScheme(context)
                    }
            }
        } else {
            when (theme.themeMode) {
                ThemeMode.Light -> lightColorScheme()
                ThemeMode.Dark -> darkColorScheme()
                ThemeMode.System ->
                    if (!isSystemInDarkTheme()) {
                        lightColorScheme()
                    } else {
                        darkColorScheme()
                    }
            }
        }

    val layoutDirection = when (theme.textDirection) {
        TextDirection.Ltr -> LayoutDirection.Ltr
        TextDirection.Rtl -> LayoutDirection.Rtl
        TextDirection.System -> LocalLayoutDirection.current
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalDensity provides
            Density(
                density = LocalDensity.current.density,
                fontScale = theme.fontScale,
            ),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
