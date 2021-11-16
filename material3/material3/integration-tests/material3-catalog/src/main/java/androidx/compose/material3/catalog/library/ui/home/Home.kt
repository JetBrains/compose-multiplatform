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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.catalog.library.model.Component
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.ui.common.CatalogScaffold
import androidx.compose.material3.catalog.library.ui.component.ComponentItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues

@Composable
@OptIn(ExperimentalFoundationApi::class)
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
        BoxWithConstraints(modifier = Modifier.padding(paddingValues)) {
            LazyVerticalGrid(
                modifier = Modifier.padding(paddingValues),
                cells = GridCells.Adaptive(HomeCellMinSize),
                content = {
                    items(components) { component ->
                        ComponentItem(
                            component = component,
                            onClick = onComponentClick
                        )
                    }
                },
                contentPadding = rememberInsetsPaddingValues(
                    insets = LocalWindowInsets.current.navigationBars,
                    additionalStart = HomePadding,
                    additionalTop = HomePadding,
                    additionalEnd = HomePadding,
                    additionalBottom = HomePadding
                )
            )
        }
    }
}

private val HomeCellMinSize = 180.dp
private val HomePadding = 12.dp