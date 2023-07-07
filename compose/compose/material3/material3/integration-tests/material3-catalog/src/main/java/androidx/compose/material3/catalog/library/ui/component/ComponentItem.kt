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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.catalog.library.model.Component
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentItem(
    component: Component,
    onClick: (component: Component) -> Unit
) {
    OutlinedCard(
        onClick = { onClick(component) },
        modifier = Modifier
            .height(ComponentItemHeight)
            .padding(ComponentItemOuterPadding)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(ComponentItemInnerPadding)) {
            Image(
                painter = painterResource(id = component.icon),
                contentDescription = null,
                modifier = Modifier
                    .size(ComponentItemIconSize)
                    .align(Alignment.Center),
                colorFilter = if (component.tintIcon) {
                    ColorFilter.tint(LocalContentColor.current)
                } else {
                    null
                },
                contentScale = ContentScale.Inside
            )
            Text(
                text = component.name,
                modifier = Modifier.align(Alignment.BottomStart),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private val ComponentItemHeight = 180.dp
private val ComponentItemOuterPadding = 4.dp
private val ComponentItemInnerPadding = 16.dp
private val ComponentItemIconSize = 80.dp
