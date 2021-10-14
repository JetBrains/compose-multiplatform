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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.catalog.model.Specification
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// TODO: Use components/values from Material3 when available
@Composable
fun SpecificationItem(
    specification: Specification,
    onClick: (specification: Specification) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SpecificationItemHeight)
            .clickable { onClick(specification) }
            .padding(SpecificationItemPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = specification.name,
            style = MaterialTheme.typography.body1
        )
    }
}

private val SpecificationItemHeight = 56.dp
private val SpecificationItemPadding = 16.dp
