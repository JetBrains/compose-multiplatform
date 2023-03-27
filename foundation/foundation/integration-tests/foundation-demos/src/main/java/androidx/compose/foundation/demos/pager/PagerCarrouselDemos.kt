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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Carrousel = listOf(
    ComposableDemo("Horizontal") { HorizontalCarrouselDemo() },
    ComposableDemo("Vertical") { VerticalCarrouselDemo() },
    ComposableDemo("3 pages per viewport") { HorizontalCustomPageSizeDemo() },
    ComposableDemo("Max Scroll = 3 pages") {
        HorizontalCustomPageSizeWithCustomMaxScrollDemo()
    },
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalCarrouselDemo() {
    val pagerState = rememberPagerState()

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier,
            state = pagerState,
            pageCount = PagesCount,
            pageSize = PageSize.Fixed(200.dp)
        ) {
            CarrouselItem(it, Orientation.Vertical)
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalCarrouselDemo() {
    val pagerState = rememberPagerState()

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        VerticalPager(
            modifier = Modifier.weight(0.9f),
            state = pagerState,
            pageCount = PagesCount,
            pageSize = PageSize.Fixed(200.dp)
        ) {
            CarrouselItem(it, Orientation.Horizontal)
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalCustomPageSizeDemo() {
    val pagerState = rememberPagerState()

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier,
            state = pagerState,
            pageCount = PagesCount,
            pageSize = ThreePagesPerViewport,
            pageSpacing = 8.dp
        ) {
            CarrouselItem(index = it, fillOrientation = Orientation.Vertical)
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalCustomPageSizeWithCustomMaxScrollDemo() {
    val pagerState = rememberPagerState()

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier,
            state = pagerState,
            pageCount = PagesCount,
            pageSize = ThreePagesPerViewport,
            pageSpacing = 8.dp,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(3)
            )
        ) {
            CarrouselItem(index = it, fillOrientation = Orientation.Vertical)
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
    }
}

@Composable
private fun CarrouselItem(index: Int, fillOrientation: Orientation) {
    val fillAxisModifier = if (fillOrientation == Orientation.Vertical) Modifier
        .fillMaxWidth()
        .height(256.dp) else Modifier
        .fillMaxHeight()
        .width(256.dp)
    Box(
        modifier = Modifier
            .then(fillAxisModifier)
            .padding(10.dp)
            .background(Color.Magenta),
        contentAlignment = Alignment.Center
    ) {
        Text(text = index.toString(), fontSize = 32.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val ThreePagesPerViewport = object : PageSize {
    override fun Density.calculateMainAxisPageSize(
        availableSpace: Int,
        pageSpacing: Int
    ): Int {
        return (availableSpace - 2 * pageSpacing) / 3
    }
}