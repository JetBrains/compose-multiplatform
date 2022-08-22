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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.snapping.calculateDistanceToDesiredSnapPosition
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastMaxBy
import kotlin.math.abs

/**
 * Creates and remember a [PagerState] to be used with a [Pager]
 *
 * @param initialPage The pager that should be shown first.
 * @param initialPageOffset The offset of the initial page with respect to the start of the layout.
 */
@ExperimentalFoundationApi
@Composable
internal fun rememberPagerState(initialPage: Int = 0, initialPageOffset: Int = 0): PagerState {
    return rememberSaveable(saver = PagerState.Saver) {
        PagerState(initialPage = initialPage, initialPageOffset = initialPageOffset)
    }
}

/**
 * The state that can be used to control [VerticalPager] and [HorizontalPager]
 * @param initialPage The initial page to be displayed
 * @param initialPageOffset The offset of the initial page with respect to the start of the layout.
 */
@ExperimentalFoundationApi
internal class PagerState(
    initialPage: Int = 0,
    initialPageOffset: Int = 0
) : ScrollableState {

    internal val lazyListState = LazyListState(initialPage, initialPageOffset)

    internal var pageSpacing by mutableStateOf(0)

    internal val pageSize: Int
        get() = visiblePages.firstOrNull()?.size ?: 0

    private val visiblePages: List<LazyListItemInfo>
        get() = lazyListState.layoutInfo.visibleItemsInfo

    private val pageAvailableSpace: Int
        get() = pageSize + pageSpacing

    private val pageCount: Int
        get() = lazyListState.layoutInfo.totalItemsCount

    private val closestPageToSnappedPosition: LazyListItemInfo?
        get() = visiblePages.fastMaxBy {
            -abs(
                lazyListState.density.calculateDistanceToDesiredSnapPosition(
                    lazyListState.layoutInfo,
                    it,
                    SnapAlignmentStartToStart
                )
            )
        }

    private val distanceToSnapPosition: Float
        get() = closestPageToSnappedPosition?.let {
            lazyListState.density.calculateDistanceToDesiredSnapPosition(
                lazyListState.layoutInfo,
                it,
                SnapAlignmentStartToStart
            )
        } ?: 0f

    /**
     * [InteractionSource] that will be used to dispatch drag events when this
     * list is being dragged. If you want to know whether the fling (or animated scroll) is in
     * progress, use [isScrollInProgress].
     */
    val interactionSource: InteractionSource get() = lazyListState.interactionSource

    /**
     * The page that sits closest to the snapped position.
     */
    val currentPage: Int by derivedStateOf { closestPageToSnappedPosition?.index ?: 0 }

    private var settledPageState by mutableStateOf(initialPage)

    /**
     * The page that is currently "settled". This is an animation/gesture unaware page in the sense
     * that it will not be updated while the pages are being scrolled, but rather when the
     * animation/scroll settles.
     */
    val settledPage: Int by derivedStateOf {
        if (pageCount == 0) 0 else settledPageState.coerceInPageRange()
    }

    /**
     * Indicates how far the current page is to the snapped position, this will vary from
     * [MinPageOffset] (page is offset towards the start of the layout) to [MaxPageOffset]
     * (page is offset towards the end of the layout). This is 0.0 if the [currentPage] is in the
     * snapped position. The value will flip once the current page changes.
     */
    val currentPageOffset: Float by derivedStateOf {
        val currentPagePositionOffset = closestPageToSnappedPosition?.offset ?: 0
        val pageUsedSpace = pageAvailableSpace.toFloat()
        ((-currentPagePositionOffset) / (pageUsedSpace)).coerceIn(
            MinPageOffset, MaxPageOffset
        )
    }

    /**
     * Scroll (jump immediately) to a given [page]
     * @param page The destination page to scroll to
     */
    suspend fun scrollToPage(page: Int) {
        val targetPage = page.coerceInPageRange()
        val pageOffsetToCorrectPosition = distanceToSnapPosition.toInt()
        lazyListState.scrollToItem(targetPage, pageOffsetToCorrectPosition)
    }

    /**
     * Scroll animate to a given [page]. If the [page] is too far away from [currentPage] we will
     * not compose all pages in the way. We will pre-jump to a nearer page, compose and animate
     * the rest of the pages until [page].
     * @param page The destination page to scroll to
     * @param animationSpec An [AnimationSpec] to move between pages. We'll use a [spring] as the
     * default animation.
     */
    suspend fun animateScrollToPage(
        page: Int,
        animationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)
    ) {
        if (page == currentPage) return
        var currentPosition = currentPage
        // If our future page is too far off, that is, outside of the current viewport
        val firstVisiblePageIndex = visiblePages.first().index
        val lastVisiblePageIndex = visiblePages.last().index
        if (((page > currentPage && page > lastVisiblePageIndex) ||
                (page < currentPage && page < firstVisiblePageIndex)) &&
            abs(page - currentPage) >= MaxPagesForAnimateScroll
        ) {
            val preJumpPosition = if (page > currentPage) {
                (page - visiblePages.size).coerceAtLeast(currentPosition)
            } else {
                page + visiblePages.size.coerceAtMost(currentPosition)
            }
            // Pre-jump to 1 viewport away from destination item, if possible
            scrollToPage(preJumpPosition)
            currentPosition = preJumpPosition
        }

        val targetPage = page.coerceInPageRange()
        val targetOffset = targetPage * pageAvailableSpace
        val currentOffset = currentPosition * pageAvailableSpace
        val pageOffsetToSnappedPosition = distanceToSnapPosition

        val displacement = targetOffset - currentOffset + pageOffsetToSnappedPosition
        lazyListState.animateScrollBy(displacement, animationSpec)
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        lazyListState.scroll(scrollPriority, block)
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return lazyListState.dispatchRawDelta(delta)
    }

    override val isScrollInProgress: Boolean
        get() = lazyListState.isScrollInProgress

    private fun Int.coerceInPageRange() = coerceIn(0, pageCount - 1)
    internal fun updateOnScrollStopped() {
        settledPageState = currentPage
    }

    companion object {
        /**
         * To keep current page and current page offset saved
         */
        val Saver: Saver<PagerState, *> = listSaver(
            save = {
                listOf(
                    it.closestPageToSnappedPosition?.index ?: 0,
                    it.closestPageToSnappedPosition?.offset ?: 0
                )
            },
            restore = {
                PagerState(
                    initialPage = it[0],
                    initialPageOffset = it[1]
                )
            }
        )
    }
}

private const val MinPageOffset = -0.5f
private const val MaxPageOffset = 0.5f
internal val SnapAlignmentStartToStart: Density.(layoutSize: Float, itemSize: Float) -> Float =
    { _, _ -> 0f }
private const val MaxPagesForAnimateScroll = 3