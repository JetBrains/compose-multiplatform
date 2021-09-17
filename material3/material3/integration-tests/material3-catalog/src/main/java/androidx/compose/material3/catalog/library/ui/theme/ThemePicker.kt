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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues

// TODO: Use components/values from Material3 when available
@Composable
@Suppress("UNUSED_PARAMETER")
fun ThemePicker(
    theme: Theme,
    onThemeChange: (theme: Theme) -> Unit
) {
    LazyColumn(
        contentPadding = rememberInsetsPaddingValues(
            insets = LocalWindowInsets.current.navigationBars,
            additionalTop = ThemePickerPadding,
            additionalBottom = ThemePickerPadding
        )
    ) {
        item {
            Text(
                text = stringResource(id = R.string.theming_options),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            Spacer(modifier = Modifier.height(ThemePickerPadding))
        }
        // TODO: Use values from Material3 theme model when available
        item {
            Text(
                text = "Work in progress",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = ThemePickerPadding)
            )
            Spacer(modifier = Modifier.height(ThemePickerPadding))
        }
    }
}

private val ThemePickerPadding = 16.dp
