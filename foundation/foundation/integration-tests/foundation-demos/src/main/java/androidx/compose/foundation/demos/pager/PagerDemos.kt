/*
 * Copyright 2022 The Android Open Source Project
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

/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.demos.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

val SimplePager = listOf(
    ComposableDemo("Horizontal") { HorizontalPagerDemo() },
    ComposableDemo("Vertical") { VerticalPagerDemo() },
)

val PagerDemos = listOf(
    DemoCategory("Simple", SimplePager),
    DemoCategory("Carrousel", Carrousel),
    DemoCategory("State Interactions", PagerStateInteractions)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalPagerDemo() {
    val pagerState = rememberPagerState()
    VerticalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
        pageCount = PagesCount
    ) {
        PagerItem(it)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HorizontalPagerDemo() {
    val pagerState = rememberPagerState()

    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
        pageCount = PagesCount
    ) {
        PagerItem(it)
    }
}

@Composable
internal fun PagerItem(index: Int) {
    Box(
        modifier = Modifier
            .padding(10.dp)
            .background(Color.Blue)
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Text(text = index.toString(), fontSize = 32.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PagerControls(modifier: Modifier = Modifier, pagerState: PagerState) {
    val animationScope = rememberCoroutineScope()
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Button(onClick = { animationScope.launch { pagerState.animateScrollToPage(0) } }) {
            Text(text = "Start")
        }
        Button(onClick = {
            animationScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }) {
            Text(text = "Previous")
        }
        Button(onClick = {
            animationScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }) {
            Text(text = "Next")
        }
        Button(onClick = {
            animationScope.launch {
                pagerState.animateScrollToPage(PagesCount - 1)
            }
        }) {
            Text(text = "End")
        }
    }
}

internal const val PagesCount = 40