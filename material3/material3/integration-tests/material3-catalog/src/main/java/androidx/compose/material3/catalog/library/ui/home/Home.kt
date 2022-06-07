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

package androidx.compose.material3.catalog.library.ui.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.catalog.library.model.Component
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.ui.common.CatalogScaffold
import androidx.compose.material3.catalog.library.ui.component.ComponentItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun Home(
    components: List<Component>,
    theme: Theme,
    onThemeChange: (theme: Theme) -> Unit,
    onComponentClick: (component: Component) -> Unit
) {
    CatalogScaffold(
        topBarTitle = stringResource(id = R.string.compose_material_3),
        theme = theme,
        onThemeChange = onThemeChange
    ) { paddingValues ->
        LazyVerticalGrid(
            modifier = Modifier.padding(paddingValues),
            columns = GridCells.Adaptive(HomeCellMinSize),
            content = {
                items(components) { component ->
                    ComponentItem(
                        component = component,
                        onClick = onComponentClick
                    )
                }
            },
            contentPadding = WindowInsets.safeDrawing
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .add(
                    WindowInsets(
                        left = HomePadding,
                        top = HomePadding,
                        right = HomePadding,
                        bottom = HomePadding
                    )
                )
                .asPaddingValues()
        )
    }
}

private val HomeCellMinSize = 180.dp
private val HomePadding = 12.dp