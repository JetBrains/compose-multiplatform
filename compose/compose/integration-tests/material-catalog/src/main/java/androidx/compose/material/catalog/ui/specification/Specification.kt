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

package androidx.compose.material.catalog.ui.specification

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.catalog.R
import androidx.compose.material.catalog.model.Specification
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun Specification(
    specifications: List<Specification>,
    onSpecificationClick: (specification: Specification) -> Unit
) {
    SpecificationScaffold(
        topBarTitle = stringResource(id = R.string.compose_material_catalog)
    ) { paddingValues ->
        LazyColumn(
            content = {
                item {
                    Text(
                        text = stringResource(id = R.string.specifications),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(SpecificationPadding))
                }
                items(specifications) { specification ->
                    SpecificationItem(
                        specification = specification,
                        onClick = onSpecificationClick
                    )
                    Spacer(modifier = Modifier.height(SpecificationItemPadding))
                }
            },
            contentPadding = WindowInsets.safeDrawing
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                .add(
                    WindowInsets(
                        left = SpecificationPadding,
                        top = SpecificationPadding,
                        right = SpecificationPadding,
                    )
                )
                .asPaddingValues(),
            modifier = Modifier.padding(paddingValues)
        )
    }
}

private val SpecificationPadding = 16.dp
private val SpecificationItemPadding = 8.dp
