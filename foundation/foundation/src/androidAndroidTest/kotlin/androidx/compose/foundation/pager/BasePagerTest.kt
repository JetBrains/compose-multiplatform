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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule

@OptIn(ExperimentalFoundationApi::class)
internal open class BasePagerTest(private val config: ParamConfig) {
    @get:Rule
    val rule = createComposeRule()

    val isVertical = config.orientation == Orientation.Vertical
    lateinit var scope: CoroutineScope
    var pagerSize: Int = 0
    var placed = mutableSetOf<Int>()
    var pageSize: Int = 0

    @Stable
    fun Modifier.crossAxisSize(size: Dp) =
        if (isVertical) {
            this.width(size)
        } else {
            this.height(size)
        }

    fun TouchInjectionScope.swipeWithVelocityAcrossMainAxis(velocity: Float, delta: Float? = null) {
        val end = if (delta == null) {
            layoutEnd
        } else {
            if (isVertical) {
                layoutStart.copy(y = layoutStart.y + delta)
            } else {
                layoutStart.copy(x = layoutStart.x + delta)
            }
        }
        swipeWithVelocity(layoutStart, end, velocity)
    }

    internal fun createPager(
        state: PagerState,
        modifier: Modifier = Modifier,
        pagerCount: () -> Int = { DefaultPageCount },
        offscreenPageLimit: Int = 0,
        pageSize: PageSize = PageSize.Fill,
        userScrollEnabled: Boolean = true,
        snappingPage: PagerSnapDistance = PagerSnapDistance.atMost(1),
        effects: @Composable () -> Unit = {}
    ) {
        rule.setContent {
            val flingBehavior =
                PagerDefaults.flingBehavior(
                    state = state,
                    pagerSnapDistance = snappingPage
                )
            CompositionLocalProvider(LocalLayoutDirection provides config.layoutDirection) {
                scope = rememberCoroutineScope()
                HorizontalOrVerticalPager(
                    pageCount = pagerCount(),
                    state = state,
                    beyondBoundsPageCount = offscreenPageLimit,
                    modifier = modifier
                        .testTag(PagerTestTag)
                        .onSizeChanged { pagerSize = if (isVertical) it.height else it.width },
                    pageSize = pageSize,
                    userScrollEnabled = userScrollEnabled,
                    reverseLayout = config.reverseLayout,
                    flingBehavior = flingBehavior,
                    pageSpacing = config.pageSpacing,
                    contentPadding = config.mainAxisContentPadding
                ) {
                    Page(index = it)
                }
                effects()
            }
        }
    }

    @Composable
    internal fun Page(index: Int) {
        Box(modifier = Modifier
            .onPlaced {
                placed.add(index)
                pageSize = if (isVertical) it.size.height else it.size.width
            }
            .fillMaxSize()
            .background(Color.Blue)
            .testTag("$index"),
            contentAlignment = Alignment.Center) {
            BasicText(text = index.toString())
        }
    }

    internal fun onPager(): SemanticsNodeInteraction {
        return rule.onNodeWithTag(PagerTestTag)
    }

    internal val scrollForwardSign: Int
        get() = if (isVertical) {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                1
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                -1
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                1
            } else {
                -1
            }
        } else {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                -1
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                1
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                1
            } else {
                -1
            }
        }

    internal val TouchInjectionScope.layoutStart: Offset
        get() = if (isVertical) {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                topCenter
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                bottomCenter
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                topCenter
            } else {
                bottomCenter
            }
        } else {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                centerRight
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                centerLeft
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                centerLeft
            } else {
                centerRight
            }
        }

    internal val TouchInjectionScope.layoutEnd: Offset
        get() = if (isVertical) {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                bottomCenter
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                topCenter
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                bottomCenter
            } else {
                topCenter
            }
        } else {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                centerLeft
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                centerRight
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                centerRight
            } else {
                centerLeft
            }
        }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    internal fun HorizontalOrVerticalPager(
        pageCount: Int,
        state: PagerState = rememberPagerState(),
        modifier: Modifier = Modifier,
        userScrollEnabled: Boolean = true,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        beyondBoundsPageCount: Int = 0,
        pageSize: PageSize = PageSize.Fill,
        flingBehavior: SnapFlingBehavior = PagerDefaults.flingBehavior(state = state),
        pageSpacing: Dp = 0.dp,
        pageContent: @Composable (pager: Int) -> Unit
    ) {
        if (isVertical) {
            VerticalPager(
                pageCount = pageCount,
                state = state,
                modifier = modifier,
                userScrollEnabled = userScrollEnabled,
                reverseLayout = reverseLayout,
                contentPadding = contentPadding,
                beyondBoundsPageCount = beyondBoundsPageCount,
                pageSize = pageSize,
                flingBehavior = flingBehavior,
                pageSpacing = pageSpacing,
                pageContent = pageContent
            )
        } else {
            HorizontalPager(
                pageCount = pageCount,
                state = state,
                modifier = modifier,
                userScrollEnabled = userScrollEnabled,
                reverseLayout = reverseLayout,
                contentPadding = contentPadding,
                beyondBoundsPageCount = beyondBoundsPageCount,
                pageSize = pageSize,
                flingBehavior = flingBehavior,
                pageSpacing = pageSpacing,
                pageContent = pageContent
            )
        }
    }

    internal fun confirmPageIsInCorrectPosition(
        currentPageIndex: Int,
        pageToVerifyPosition: Int = currentPageIndex
    ) {
        val leftContentPadding =
            config.mainAxisContentPadding.calculateLeftPadding(config.layoutDirection)
        val topContentPadding = config.mainAxisContentPadding.calculateTopPadding()

        val (left, top) = with(rule.density) {
            val spacings = config.pageSpacing.roundToPx()
            val initialPageOffset = currentPageIndex * (pageSize + spacings)

            val position = pageToVerifyPosition * (pageSize + spacings) - initialPageOffset
            if (isVertical) {
                0.dp to position.toDp()
            } else {
                position.toDp() to 0.dp
            }
        }
        rule.onNodeWithTag("$pageToVerifyPosition")
            .assertPositionInRootIsEqualTo(left + leftContentPadding, top + topContentPadding)
    }
}

internal class ParamConfig(
    val orientation: Orientation,
    val reverseLayout: Boolean = false,
    val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    val pageSpacing: Dp = 0.dp,
    val mainAxisContentPadding: PaddingValues = PaddingValues(0.dp)
) {
    override fun toString(): String {
        return "orientation=$orientation " +
            "reverseLayout=$reverseLayout " +
            "layoutDirection=$layoutDirection " +
            "pageSpacing=$pageSpacing " +
            "mainAxisContentPadding=$mainAxisContentPadding"
    }
}

internal const val PagerTestTag = "pager"
internal const val DefaultPageCount = 20
internal val TestOrientation = listOf(Orientation.Vertical, Orientation.Horizontal)
internal val TestReverseLayout = listOf(false, true)
internal val TestLayoutDirection = listOf(LayoutDirection.Rtl, LayoutDirection.Ltr)
internal val TestPageSpacing = listOf(0.dp, 8.dp)
internal fun testContentPaddings(orientation: Orientation) = listOf(
    PaddingValues(0.dp),
    if (orientation == Orientation.Vertical)
        PaddingValues(vertical = 16.dp)
    else PaddingValues(horizontal = 16.dp),
    PaddingValues(start = 16.dp),
    PaddingValues(end = 16.dp)
)
