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

package androidx.compose.material.catalog.library

import androidx.compose.material.catalog.library.model.Theme
import androidx.compose.material.catalog.library.model.ThemeSaver
import androidx.compose.material.catalog.library.ui.theme.CatalogTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun MaterialCatalogApp() {
    var theme by rememberSaveable(stateSaver = ThemeSaver) { mutableStateOf(Theme()) }
    CatalogTheme(theme = theme) {
        NavGraph(
            theme = theme,
            onThemeChange = { theme = it }
        )
    }
}
