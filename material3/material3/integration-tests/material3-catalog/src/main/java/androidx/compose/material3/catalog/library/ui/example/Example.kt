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

package androidx.compose.material3.catalog.library.ui.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.catalog.library.model.Component
import androidx.compose.material3.catalog.library.model.Example
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.ui.common.CatalogScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun Example(
    component: Component,
    example: Example,
    theme: Theme,
    onThemeChange: (theme: Theme) -> Unit,
    onBackClick: () -> Unit
) {
    CatalogScaffold(
        topBarTitle = example.name,
        showBackNavigationIcon = true,
        theme = theme,
        guidelinesUrl = component.guidelinesUrl,
        docsUrl = component.docsUrl,
        sourceUrl = example.sourceUrl,
        onThemeChange = onThemeChange,
        onBackClick = onBackClick
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                ),
            contentAlignment = Alignment.Center
        ) {
            example.content()
        }
    }
}
