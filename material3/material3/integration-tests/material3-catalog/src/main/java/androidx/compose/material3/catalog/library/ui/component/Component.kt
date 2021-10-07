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

package androidx.compose.material3.catalog.library.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.catalog.library.model.Component
import androidx.compose.material3.catalog.library.model.Example
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.ui.common.CatalogScaffold
import androidx.compose.material3.catalog.library.ui.example.ExampleItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues

// TODO: Use components/values from Material3 when available
@Composable
fun Component(
    component: Component,
    theme: Theme,
    onThemeChange: (theme: Theme) -> Unit,
    onExampleClick: (example: Example) -> Unit,
    onBackClick: () -> Unit
) {
    CatalogScaffold(
        topBarTitle = component.name,
        showBackNavigationIcon = true,
        theme = theme,
        guidelinesUrl = component.guidelinesUrl,
        docsUrl = component.docsUrl,
        sourceUrl = component.sourceUrl,
        onThemeChange = onThemeChange,
        onBackClick = onBackClick
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = ComponentPadding),
            contentPadding = rememberInsetsPaddingValues(
                insets = LocalWindowInsets.current.navigationBars
            )
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ComponentIconVerticalPadding)
                ) {
                    Image(
                        painter = painterResource(id = component.icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(ComponentIconSize)
                            .align(Alignment.Center),
                        colorFilter = if (component.tintIcon) {
                            ColorFilter.tint(
                                LocalContentColor.current.copy(alpha = ContentAlpha.disabled)
                            )
                        } else {
                            null
                        }
                    )
                }
            }
            item {
                Text(
                    text = stringResource(id = R.string.description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(ComponentPadding))
                Text(
                    text = component.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(ComponentDescriptionPadding))
            }
            item {
                Text(
                    text = stringResource(id = R.string.examples),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(ComponentPadding))
            }
            if (component.examples.isNotEmpty()) {
                items(component.examples) { example ->
                    ExampleItem(
                        example = example,
                        onClick = onExampleClick
                    )
                    Spacer(modifier = Modifier.height(ComponentPadding))
                }
            } else {
                item {
                    Text(
                        text = stringResource(id = R.string.no_examples),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(ComponentPadding))
                }
            }
        }
    }
}

private val ComponentIconSize = 108.dp
private val ComponentIconVerticalPadding = 42.dp
private val ComponentPadding = 16.dp
private val ComponentDescriptionPadding = 32.dp
