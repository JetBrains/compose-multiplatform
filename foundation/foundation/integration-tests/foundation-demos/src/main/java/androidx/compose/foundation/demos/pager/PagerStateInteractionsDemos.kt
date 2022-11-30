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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val PagerStateInteractions = listOf(
    ComposableDemo("Moving Pages Programmatically") { StateDrivenPage() },
    ComposableDemo("Observing Page Changes - Full Size Page") { StateMonitoringPager() },
    ComposableDemo("Observing Page Changes - Custom Page Size") {
        StateMonitoringCustomPageSize()
    },
    ComposableDemo("Moving Pages Programmatically and Observing Changes") {
        StateDrivenPageWithMonitor()
    }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StateDrivenPage() {
    val pagerState = rememberPagerState()

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier.weight(0.9f),
            state = pagerState,
            pageCount = PagesCount
        ) {
            PagerItem(it)
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StateDrivenPageWithMonitor() {
    val pagerState = rememberPagerState()

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier.weight(0.8f),
            state = pagerState,
            pageCount = PagesCount
        ) {
            PagerItem(it)
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
        PageMonitor(Modifier.weight(0.1f), pagerState = pagerState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StateMonitoringPager() {
    val pagerState = rememberPagerState()
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier.weight(0.9f),
            state = pagerState,
            pageCount = PagesCount
        ) {
            PagerItem(it)
        }
        PageMonitor(Modifier.weight(0.1f), pagerState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageMonitor(modifier: Modifier, pagerState: PagerState) {
    Column(modifier.fillMaxWidth()) {
        Text(text = "Current Page: ${pagerState.currentPage}")
        Text(text = "Target Page: ${pagerState.targetPage}")
        Text(text = "Settled Page Offset: ${pagerState.settledPage}")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StateMonitoringCustomPageSize() {
    val pagerState = rememberPagerState()

    val fling = PagerDefaults.flingBehavior(
        state = pagerState, PagerSnapDistance.atMost(3)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier.weight(0.9f),
            state = pagerState,
            pageCount = PagesCount,
            pageSize = PageSize.Fixed(96.dp),
            flingBehavior = fling
        ) {
            PagerItem(it)
        }
        PageMonitor(Modifier, pagerState)
    }
}