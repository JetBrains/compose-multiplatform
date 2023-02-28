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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun SimpleHorizontalPagerSample() {
    // Creates a 1-pager/viewport horizontal pager with single page snapping
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        pageCount = 10
    ) { page ->
        Box(
            modifier = Modifier
                .padding(10.dp)
                .background(Color.Blue)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(text = page.toString(), fontSize = 32.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun SimpleVerticalPagerSample() {
    // Creates a 1-pager/viewport vertical pager with single page snapping
    VerticalPager(
        modifier = Modifier.fillMaxSize(),
        pageCount = 10
    ) { page ->
        Box(
            modifier = Modifier
                .padding(10.dp)
                .background(Color.Blue)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(text = page.toString(), fontSize = 32.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun PagerWithStateSample() {
    // You can use PagerState to define an initial page
    val state = rememberPagerState(initialPage = 5)
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = state,
        pageCount = 10
    ) { page ->
        Box(
            modifier = Modifier
                .padding(10.dp)
                .background(Color.Blue)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(text = page.toString(), fontSize = 32.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun CustomPageSizeSample() {

    // [PageSize] should be defined as a top level constant in order to avoid unnecessary re-
    // creations.
    val CustomPageSize = object : PageSize {
        override fun Density.calculateMainAxisPageSize(
            availableSpace: Int,
            pageSpacing: Int
        ): Int {
            // [availableSpace] represents the whole Pager width (in this case), we'd like to have
            // 3 pages per viewport, so we divide it by 3, taking into consideration the start
            // and end spacings.
            return (availableSpace - 2 * pageSpacing) / 3
        }
    }

    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        pageCount = 10,
        pageSize = CustomPageSize
    ) { page ->
        Box(
            modifier = Modifier
                .padding(10.dp)
                .background(Color.Blue)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(text = page.toString(), fontSize = 32.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun ObservingStateChangesInPagerStateSample() {
    val pagerState = rememberPagerState()
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier.weight(0.9f),
            state = pagerState,
            pageCount = 10
        ) { page ->
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .background(Color.Blue)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = page.toString(), fontSize = 32.sp)
            }
        }
        Column(modifier = Modifier
            .weight(0.1f)
            .fillMaxWidth()) {
            Text(text = "Current Page: ${pagerState.currentPage}")
            Text(text = "Target Page: ${pagerState.targetPage}")
            Text(text = "Settled Page Offset: ${pagerState.settledPage}")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun AnimateScrollPageSample() {
    val state = rememberPagerState()
    val animationScope = rememberCoroutineScope()
    Column {
        HorizontalPager(
            modifier = Modifier.weight(0.7f),
            state = state,
            pageCount = 10
        ) { page ->
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .background(Color.Blue)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = page.toString(), fontSize = 32.sp)
            }
        }

        Box(modifier = Modifier.weight(0.3f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(onClick = {
                animationScope.launch {
                    state.animateScrollToPage(state.currentPage + 1)
                }
            }) {
                Text(text = "Next Page")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun ScrollToPageSample() {
    val state = rememberPagerState()
    val scrollScope = rememberCoroutineScope()
    Column {
        HorizontalPager(
            modifier = Modifier.height(400.dp),
            state = state,
            pageCount = 10
        ) { page ->
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .background(Color.Blue)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = page.toString(), fontSize = 32.sp)
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            androidx.compose.material.Button(onClick = {
                scrollScope.launch {
                    state.scrollToPage(state.currentPage + 1)
                }
            }) {
                Text(text = "Next Page")
            }
        }
    }
}