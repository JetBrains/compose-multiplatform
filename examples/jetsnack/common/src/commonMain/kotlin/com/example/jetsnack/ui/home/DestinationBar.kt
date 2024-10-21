/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetsnack.ui.home

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.common.generated.resources.Res
import com.example.common.generated.resources.label_select_delivery
import com.example.jetsnack.ui.LocalNavAnimatedVisibilityScope
import com.example.jetsnack.ui.LocalSharedTransitionScope
import com.example.jetsnack.ui.components.JetsnackDivider
import com.example.jetsnack.ui.snackdetail.spatialExpressiveSpring
import com.example.jetsnack.ui.theme.AlphaNearOpaque
import com.example.jetsnack.ui.theme.JetsnackTheme
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DestinationBar(modifier: Modifier = Modifier) {
    val sharedElementScope =
        LocalSharedTransitionScope.current ?: throw IllegalStateException("No shared element scope")
    val navAnimatedScope =
        LocalNavAnimatedVisibilityScope.current ?: throw IllegalStateException("No nav scope")
    with(sharedElementScope) {
        with(navAnimatedScope) {
            Column(
                modifier = modifier
                    .renderInSharedTransitionScopeOverlay()
                    .animateEnterExit(
                        enter = slideInVertically(spatialExpressiveSpring()) { -it * 2 },
                        exit = slideOutVertically(spatialExpressiveSpring()) { -it * 2 }
                    )
            ) {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = {
                        Row {
                            Text(
                                text = "Huidekoperstraat 26-28, 1017 ZM Amsterdam | https://kotl.in/wasm-gio23",
                                style = MaterialTheme.typography.titleMedium,
                                color = JetsnackTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterVertically)
                            )
                            IconButton(
                                onClick = { /* todo */ },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ExpandMore,
                                    tint = JetsnackTheme.colors.brand,
                                    contentDescription = stringResource(Res.string.label_select_delivery)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors().copy(
                        containerColor = JetsnackTheme.colors.uiBackground
                            .copy(alpha = AlphaNearOpaque),
                        titleContentColor = JetsnackTheme.colors.textSecondary
                    ),
                )
                JetsnackDivider()
            }
        }
    }
}
