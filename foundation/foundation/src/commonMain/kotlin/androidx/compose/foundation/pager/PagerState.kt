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
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMaxBy
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Creates and remember a [PagerState] to be used with a [Pager]
 *
 * Please refer to the sample to learn how to use this API.
 * @sample androidx.compose.foundation.samples.PagerWithStateSample
 *
 * @param initialPage The pager that should be shown first.
 * @param initialPageOffsetFraction The offset of the initial page as a fraction of the page size.
 * This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 * snapped position.
 */
@ExperimentalFoundationApi
@Composable
fun rememberPagerState(
    initialPage: Int = 0,
    initialPageOffsetFraction: Float = 0f
): PagerState {
    return rememberSaveable(saver = PagerState.Saver) {
        PagerState(initialPage = initialPage, initialPageOffsetFraction = initialPageOffsetFraction)
    }
}

/**
 * The state that can be used to control [VerticalPager] and [HorizontalPager]
 * @param initialPage The initial page to be displayed
 * @param initialPageOffsetFraction The offset of the initial page with respect to the start of
 * the layout.
 */
@ExperimentalFoundationApi
@Stable
class PagerState(
    val initialPage: Int = 0,
    val initialPageOffsetFraction: Float = 0f
) : ScrollableState {

    init {
        require(initialPageOffsetFraction in -0.5..0.5) {
            "initialPageOffsetFraction $initialPageOffsetFraction is " +
                "not within the range -0.5 to 0.5"
        }
    }

    internal var snapRemainingScrollOffset by mutableStateOf(0f)

    private var lazyListState by mutableStateOf<LazyListState?>(null)

    internal var pageSpacing by mutableStateOf(0)

    private val awaitLazyListStateSet = AwaitLazyListStateSet()

    internal val pageSize: Int
        get() = visiblePages.firstOrNull()?.size ?: 0

    private val density: Density
        get() = lazyListState?.density ?: UnitDensity

    internal val layoutInfo: LazyListLayoutInfo
        get() = lazyListState?.layoutInfo ?: EmptyLayoutInfo

    private val visiblePages: List<LazyListItemInfo>
        get() = layoutInfo.visibleItemsInfo

    private val pageAvailableSpace: Int
        get() = pageSize + pageSpacing

    /**
     * How far the current page needs to scroll so the target page is considered to be the next
     * page.
     */
    private val positionThresholdFraction: Float
        get() = with(density) {
            val minThreshold = minOf(DefaultPositionThreshold.toPx(), pageSize / 2f)
            minThreshold / pageSize.toFloat()
        }

    internal val pageCount: Int
        get() = layoutInfo.totalItemsCount

    private val closestPageToSnappedPosition: LazyListItemInfo?
        get() = visiblePages.fastMaxBy {
            -abs(
                density.calculateDistanceToDesiredSnapPosition(
                    layoutInfo,
                    it,
                    SnapAlignmentStartToStart
                )
            )
        }

    internal val firstVisiblePage: LazyListItemInfo?
        get() = visiblePages.lastOrNull {
            density.calculateDistanceToDesiredSnapPosition(
                layoutInfo,
                it,
                SnapAlignmentStartToStart
            ) <= 0
        }

    private val distanceToSnapPosition: Float
        get() = closestPageToSnappedPosition?.let {
            density.calculateDistanceToDesiredSnapPosition(
                layoutInfo,
                it,
                SnapAlignmentStartToStart
            )
        } ?: 0f

    /**
     * [InteractionSource] that will be used to dispatch drag events when this
     * list is being dragged. If you want to know whether the fling (or animated scroll) is in
     * progress, use [isScrollInProgress].
     */
    val interactionSource: InteractionSource
        get() = lazyListState?.interactionSource ?: EmptyInteractionSources

    /**
     * The page that sits closest to the snapped position. This is an observable value and will
     * change as the pager scrolls either by gesture or animation.
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     *
     */
    val currentPage: Int by derivedStateOf { closestPageToSnappedPosition?.index ?: initialPage }

    private var animationTargetPage by mutableStateOf(-1)

    private var settledPageState by mutableStateOf(initialPage)

    /**
     * The page that is currently "settled". This is an animation/gesture unaware page in the sense
     * that it will not be updated while the pages are being scrolled, but rather when the
     * animation/scroll settles.
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val settledPage: Int by derivedStateOf {
        if (pageCount == 0) 0 else settledPageState.coerceInPageRange()
    }

    /**
     * The page this [Pager] intends to settle to.
     * During fling or animated scroll (from [animateScrollToPage] this will represent the page
     * this pager intends to settle to. When no scroll is ongoing, this will be equal to
     * [currentPage].
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val targetPage: Int by derivedStateOf {
        val finalPage = if (!isScrollInProgress) {
            currentPage
        } else if (animationTargetPage != -1) {
            animationTargetPage
        } else if (snapRemainingScrollOffset == 0.0f) {
            // act on scroll only
            if (abs(currentPageOffsetFraction) >= abs(positionThresholdFraction)) {
                currentPage + currentPageOffsetFraction.sign.toInt()
            } else {
                currentPage
            }
        } else {
            // act on flinging
            val pageDisplacement = snapRemainingScrollOffset / pageAvailableSpace
            (currentPage + pageDisplacement.roundToInt())
        }
        finalPage.coerceInPageRange()
    }

    /**
     * Indicates how far the current page is to the snapped position, this will vary from
     * -0.5 (page is offset towards the start of the layout) to 0.5 (page is offset towards the end
     * of the layout). This is 0.0 if the [currentPage] is in the snapped position. The value will
     * flip once the current page changes.
     *
     * This property is observable and shouldn't be used as is in a composable function due to
     * potential performance issues. To use it in the composition, please consider using a
     * derived state (e.g [derivedStateOf]) to only have recompositions when the derived
     * value changes.
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val currentPageOffsetFraction: Float by derivedStateOf {
        val currentPagePositionOffset = closestPageToSnappedPosition?.offset ?: 0
        val pageUsedSpace = pageAvailableSpace.toFloat()
        if (pageUsedSpace == 0f) {
            // Default to 0 when there's no info about the page size yet.
            initialPageOffsetFraction
        } else {
            ((-currentPagePositionOffset) / (pageUsedSpace)).coerceIn(
                MinPageOffset, MaxPageOffset
            )
        }
    }

    /**
     * Scroll (jump immediately) to a given [page].
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ScrollToPageSample
     *
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     * destination page will be offset from its snapped position.
     */
    suspend fun scrollToPage(page: Int, pageOffsetFraction: Float = 0f) {
        debugLog { "Scroll from page=$currentPage to page=$page" }
        awaitScrollDependencies()
        require(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        val targetPage = page.coerceInPageRange()
        val pageOffsetToCorrectPosition = (pageAvailableSpace * pageOffsetFraction).roundToInt()
        requireNotNull(lazyListState).scrollToItem(targetPage, pageOffsetToCorrectPosition)
    }

    /**
     * Scroll animate to a given [page]. If the [page] is too far away from [currentPage] we will
     * not compose all pages in the way. We will pre-jump to a nearer page, compose and animate
     * the rest of the pages until [page].
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.AnimateScrollPageSample
     *
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     * destination page will be offset from its snapped position.
     * @param animationSpec An [AnimationSpec] to move between pages. We'll use a [spring] as the
     * default animation.
     */
    suspend fun animateScrollToPage(
        page: Int,
        pageOffsetFraction: Float = 0f,
        animationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)
    ) {
        if (page == currentPage) return
        awaitScrollDependencies()
        require(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        var currentPosition = currentPage
        val targetPage = page.coerceInPageRange()
        animationTargetPage = targetPage
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

            debugLog {
                "animateScrollToPage with pre-jump to position=$preJumpPosition"
            }

            // Pre-jump to 1 viewport away from destination item, if possible
            requireNotNull(lazyListState).scrollToItem(preJumpPosition)
            currentPosition = preJumpPosition
        }

        val targetOffset = targetPage * pageAvailableSpace
        val currentOffset = currentPosition * pageAvailableSpace
        val pageOffsetToSnappedPosition =
            distanceToSnapPosition + pageOffsetFraction * pageAvailableSpace

        val displacement = targetOffset - currentOffset + pageOffsetToSnappedPosition

        debugLog { "animateScrollToPage $displacement pixels" }
        requireNotNull(lazyListState).animateScrollBy(displacement, animationSpec)
        animationTargetPage = -1
    }

    private suspend fun awaitScrollDependencies() {
        awaitLazyListStateSet.waitFinalLazyListSetting()
        requireNotNull(lazyListState).awaitLayoutModifier.waitForFirstLayout()
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        lazyListState?.scroll(scrollPriority, block)
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return lazyListState?.dispatchRawDelta(delta) ?: 0f
    }

    override val isScrollInProgress: Boolean
        get() = lazyListState?.isScrollInProgress ?: false

    override val canScrollForward: Boolean
        get() = lazyListState?.canScrollForward ?: true

    override val canScrollBackward: Boolean
        get() = lazyListState?.canScrollBackward ?: true

    private fun Int.coerceInPageRange() = if (pageCount > 0) {
        coerceIn(0, pageCount - 1)
    } else {
        0
    }

    internal fun updateOnScrollStopped() {
        settledPageState = currentPage
    }

    internal fun loadNewState(newState: LazyListState) {
        lazyListState = newState
        awaitLazyListStateSet.onStateLoaded()
    }

    companion object {
        /**
         * To keep current page and current page offset saved
         */
        val Saver: Saver<PagerState, *> = listSaver(
            save = {
                listOf(
                    it.currentPage,
                    it.currentPageOffsetFraction
                )
            },
            restore = {
                PagerState(
                    initialPage = it[0] as Int,
                    initialPageOffsetFraction = it[1] as Float
                )
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal suspend fun PagerState.animateToNextPage() {
    if (currentPage + 1 < pageCount) animateScrollToPage(currentPage + 1)
}

@OptIn(ExperimentalFoundationApi::class)
internal suspend fun PagerState.animateToPreviousPage() {
    if (currentPage - 1 >= 0) animateScrollToPage(currentPage - 1)
}

private const val MinPageOffset = -0.5f
private const val MaxPageOffset = 0.5f
internal val SnapAlignmentStartToStart: Density.(layoutSize: Float, itemSize: Float) -> Float =
    { _, _ -> 0f }
internal val DefaultPositionThreshold = 56.dp
private const val MaxPagesForAnimateScroll = 3

private class AwaitLazyListStateSet {
    private var continuation: Continuation<Unit>? = null
    private var stateWasLoaded = false

    suspend fun waitFinalLazyListSetting() {
        if (!stateWasLoaded) {
            val previousContinuation = continuation
            suspendCoroutine<Unit> { continuation = it }
            previousContinuation?.resume(Unit)
        }
    }

    fun onStateLoaded() {
        if (!stateWasLoaded) {
            stateWasLoaded = true
            continuation?.resume(Unit)
            continuation = null
        }
    }
}

private val EmptyLayoutInfo = object : LazyListLayoutInfo {
    override val visibleItemsInfo: List<LazyListItemInfo> = emptyList()
    override val viewportStartOffset: Int = 0
    override val viewportEndOffset: Int = 0
    override val totalItemsCount: Int = 0
    override val mainAxisItemSpacing: Int = 0
}

private val UnitDensity = object : Density {
    override val density: Float = 1f
    override val fontScale: Float = 1f
}

private val EmptyInteractionSources = object : InteractionSource {
    override val interactions: Flow<Interaction>
        get() = emptyFlow()
}

private const val DEBUG = false
private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("PagerState: ${generateMsg()}")
    }
}