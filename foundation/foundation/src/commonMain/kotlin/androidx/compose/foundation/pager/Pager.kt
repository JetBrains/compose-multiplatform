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
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.calculateDistanceToDesiredSnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyList
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.pageDown
import androidx.compose.ui.semantics.pageLeft
import androidx.compose.ui.semantics.pageRight
import androidx.compose.ui.semantics.pageUp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * A Pager that scrolls horizontally. Pages are lazily placed in accordance to the available
 * viewport size. By definition, pages in a [Pager] have the same size, defined by [pageSize] and
 * use a snap animation (provided by [flingBehavior] to scroll pages into a specific position). You
 * can use [beyondBoundsPageCount] to place more pages before and after the visible pages.
 *
 * If you need snapping with pages of different size, you can use a [SnapFlingBehavior] with a
 * [SnapLayoutInfoProvider] adapted to a LazyList.
 * @see androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider for the implementation
 * of a [SnapLayoutInfoProvider] that uses [LazyListState].
 *
 * Please refer to the sample to learn how to use this API.
 * @sample androidx.compose.foundation.samples.SimpleHorizontalPagerSample
 *
 * @param pageCount The number of pages this Pager will contain
 * @param modifier A modifier instance to be applied to this Pager outer layout
 * @param state The state to control this pager
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first page or after the last one. Use [pageSpacing] to add spacing
 * between the pages.
 * @param pageSize Use this to change how the pages will look like inside this pager.
 * @param beyondBoundsPageCount Pages to load before and after the list of visible
 * pages. Note: Be aware that using a large value for [beyondBoundsPageCount] will cause a lot of
 * pages to be composed, measured and placed which will defeat the purpose of using lazy loading.
 * This should be used as an optimization to pre-load a couple of pages before and after the visible
 * ones.
 * @param pageSpacing The amount of space to be used to separate the pages in this Pager
 * @param verticalAlignment How pages are aligned vertically in this Pager.
 * @param flingBehavior The [FlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 * disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When you specify the key the scroll
 * position will be maintained based on the key, which means if you add/remove items before the
 * current visible item the item with the given key will be kept as the first visible one.
 * @param pageNestedScrollConnection A [NestedScrollConnection] that dictates how this [Pager]
 * behaves with nested lists. The default behavior will see [Pager] to consume all nested deltas.
 * @param pageContent This Pager's page Composable.
 */
@Composable
@ExperimentalFoundationApi
fun HorizontalPager(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondBoundsPageCount: Int = 0,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: SnapFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
        Orientation.Horizontal
    ),
    pageContent: @Composable (page: Int) -> Unit
) {
    Pager(
        modifier = modifier,
        state = state,
        pageCount = pageCount,
        pageSpacing = pageSpacing,
        userScrollEnabled = userScrollEnabled,
        orientation = Orientation.Horizontal,
        verticalAlignment = verticalAlignment,
        reverseLayout = reverseLayout,
        contentPadding = contentPadding,
        beyondBoundsPageCount = beyondBoundsPageCount,
        pageSize = pageSize,
        flingBehavior = flingBehavior,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        pageContent = pageContent
    )
}

/**
 * A Pager that scrolls vertically. Pages are lazily placed in accordance to the available
 * viewport size. By definition, pages in a [Pager] have the same size, defined by [pageSize] and
 * use a snap animation (provided by [flingBehavior] to scroll pages into a specific position). You
 * can use [beyondBoundsPageCount] to place more pages before and after the visible pages.
 *
 * If you need snapping with pages of different size, you can use a [SnapFlingBehavior] with a
 * [SnapLayoutInfoProvider] adapted to a LazyList.
 * @see androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider for the implementation
 * of a [SnapLayoutInfoProvider] that uses [LazyListState].
 *
 * Please refer to the sample to learn how to use this API.
 * @sample androidx.compose.foundation.samples.SimpleVerticalPagerSample
 *
 * @param pageCount The number of pages this Pager will contain
 * @param modifier A modifier instance to be apply to this Pager outer layout
 * @param state The state to control this pager
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first page or after the last one. Use [pageSpacing] to add spacing
 * between the pages.
 * @param pageSize Use this to change how the pages will look like inside this pager.
 * @param beyondBoundsPageCount Pages to load before and after the list of visible
 * pages. Note: Be aware that using a large value for [beyondBoundsPageCount] will cause a lot of
 * pages to be composed, measured and placed which will defeat the purpose of using lazy loading.
 * This should be used as an optimization to pre-load a couple of pages before and after the visible
 * ones.
 * @param pageSpacing The amount of space to be used to separate the pages in this Pager
 * @param horizontalAlignment How pages are aligned horizontally in this Pager.
 * @param flingBehavior The [FlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 * disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When you specify the key the scroll
 * position will be maintained based on the key, which means if you add/remove items before the
 * current visible item the item with the given key will be kept as the first visible one.
 * @param pageNestedScrollConnection A [NestedScrollConnection] that dictates how this [Pager] behaves
 * with nested lists. The default behavior will see [Pager] to consume all nested deltas.
 * @param pageContent This Pager's page Composable.
 */
@Composable
@ExperimentalFoundationApi
fun VerticalPager(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondBoundsPageCount: Int = 0,
    pageSpacing: Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: SnapFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
        Orientation.Vertical
    ),
    pageContent: @Composable (page: Int) -> Unit
) {
    Pager(
        modifier = modifier,
        state = state,
        pageCount = pageCount,
        pageSpacing = pageSpacing,
        horizontalAlignment = horizontalAlignment,
        userScrollEnabled = userScrollEnabled,
        orientation = Orientation.Vertical,
        reverseLayout = reverseLayout,
        contentPadding = contentPadding,
        beyondBoundsPageCount = beyondBoundsPageCount,
        pageSize = pageSize,
        flingBehavior = flingBehavior,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        pageContent = pageContent
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Pager(
    modifier: Modifier,
    state: PagerState,
    pageCount: Int,
    pageSize: PageSize,
    pageSpacing: Dp,
    orientation: Orientation,
    beyondBoundsPageCount: Int,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    contentPadding: PaddingValues,
    flingBehavior: SnapFlingBehavior,
    userScrollEnabled: Boolean,
    reverseLayout: Boolean,
    key: ((index: Int) -> Any)?,
    pageNestedScrollConnection: NestedScrollConnection,
    pageContent: @Composable (page: Int) -> Unit
) {
    require(beyondBoundsPageCount >= 0) {
        "beyondBoundsPageCount should be greater than or equal to 0, " +
            "you selected $beyondBoundsPageCount"
    }

    val isVertical = orientation == Orientation.Vertical
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val calculatedContentPaddings = remember(contentPadding, orientation, layoutDirection) {
        calculateContentPaddings(
            contentPadding,
            orientation,
            layoutDirection
        )
    }

    val pagerFlingBehavior = remember(flingBehavior, state) {
        PagerWrapperFlingBehavior(flingBehavior, state)
    }

    LaunchedEffect(density, state, pageSpacing) {
        with(density) { state.pageSpacing = pageSpacing.roundToPx() }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .drop(1) // Initial scroll is false
            .collect { state.updateOnScrollStopped() }
    }

    val pagerSemantics = if (userScrollEnabled) {
        Modifier.pagerSemantics(state, isVertical)
    } else {
        Modifier
    }

    BoxWithConstraints(modifier = modifier.then(pagerSemantics)) {
        val mainAxisSize = if (isVertical) constraints.maxHeight else constraints.maxWidth
        // Calculates how pages are shown across the main axis
        val pageAvailableSize = remember(
            density,
            mainAxisSize,
            pageSpacing,
            calculatedContentPaddings
        ) {
            with(density) {
                val pageSpacingPx = pageSpacing.roundToPx()
                val contentPaddingPx = calculatedContentPaddings.roundToPx()
                with(pageSize) {
                    density.calculateMainAxisPageSize(
                        mainAxisSize - contentPaddingPx,
                        pageSpacingPx
                    )
                }.toDp()
            }
        }

        val horizontalAlignmentForSpacedArrangement =
            if (!reverseLayout) Alignment.Start else Alignment.End
        val verticalAlignmentForSpacedArrangement =
            if (!reverseLayout) Alignment.Top else Alignment.Bottom

        val lazyListState = remember(state) {
            val initialPageOffset =
                with(density) { pageAvailableSize.roundToPx() } * state.initialPageOffsetFraction
            LazyListState(state.initialPage, initialPageOffset.roundToInt()).also {
                state.loadNewState(it)
            }
        }

        LazyList(
            modifier = Modifier,
            state = lazyListState,
            contentPadding = contentPadding,
            flingBehavior = pagerFlingBehavior,
            horizontalAlignment = horizontalAlignment,
            horizontalArrangement = Arrangement.spacedBy(
                pageSpacing,
                horizontalAlignmentForSpacedArrangement
            ),
            verticalArrangement = Arrangement.spacedBy(
                pageSpacing,
                verticalAlignmentForSpacedArrangement
            ),
            verticalAlignment = verticalAlignment,
            isVertical = isVertical,
            reverseLayout = reverseLayout,
            userScrollEnabled = userScrollEnabled,
            beyondBoundsItemCount = beyondBoundsPageCount
        ) {

            items(pageCount, key = key) {
                val pageMainAxisSizeModifier = if (isVertical) {
                    Modifier.height(pageAvailableSize)
                } else {
                    Modifier.width(pageAvailableSize)
                }
                Box(
                    modifier = Modifier
                        .then(pageMainAxisSizeModifier)
                        .nestedScroll(pageNestedScrollConnection),
                    contentAlignment = Alignment.Center
                ) {
                    pageContent(it)
                }
            }
        }
    }
}

private fun calculateContentPaddings(
    contentPadding: PaddingValues,
    orientation: Orientation,
    layoutDirection: LayoutDirection
): Dp {

    val startPadding = if (orientation == Orientation.Vertical) {
        contentPadding.calculateTopPadding()
    } else {
        contentPadding.calculateLeftPadding(layoutDirection)
    }

    val endPadding = if (orientation == Orientation.Vertical) {
        contentPadding.calculateBottomPadding()
    } else {
        contentPadding.calculateRightPadding(layoutDirection)
    }

    return startPadding + endPadding
}

/**
 * This is used to determine how Pages are laid out in [Pager]. By changing the size of the pages
 * one can change how many pages are shown.
 *
 * Please refer to the sample to learn how to use this API.
 * @sample androidx.compose.foundation.samples.CustomPageSizeSample
 *
 */
@ExperimentalFoundationApi
@Stable
interface PageSize {

    /**
     * Based on [availableSpace] pick a size for the pages
     * @param availableSpace The amount of space the pages in this Pager can use.
     * @param pageSpacing The amount of space used to separate pages.
     */
    fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int

    /**
     * Pages take up the whole Pager size.
     */
    @ExperimentalFoundationApi
    object Fill : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            return availableSpace
        }
    }

    /**
     * Multiple pages in a viewport
     * @param pageSize A fixed size for pages
     */
    @ExperimentalFoundationApi
    class Fixed(val pageSize: Dp) : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            return pageSize.roundToPx()
        }
    }
}

/**
 * Contains the default values used by [Pager].
 */
@ExperimentalFoundationApi
object PagerDefaults {

    /**
     * A [SnapFlingBehavior] that will snap pages to the start of the layout. One can use the
     * given parameters to control how the snapping animation will happen.
     * @see androidx.compose.foundation.gestures.snapping.SnapFlingBehavior for more information
     * on what which parameter controls in the overall snapping animation.
     *
     * @param state The [PagerState] that controls the which to which this FlingBehavior will
     * be applied to.
     * @param pagerSnapDistance A way to control the snapping destination for this [Pager].
     * The default behavior will result in any fling going to the next page in the direction of the
     * fling (if the fling has enough velocity, otherwise we'll bounce back). Use
     * [PagerSnapDistance.atMost] to define a maximum number of pages this [Pager] is allowed to
     * fling after scrolling is finished and fling has started.
     * @param lowVelocityAnimationSpec The animation spec used to approach the target offset. When
     * the fling velocity is not large enough. Large enough means large enough to naturally decay.
     * @param highVelocityAnimationSpec The animation spec used to approach the target offset. When
     * the fling velocity is large enough. Large enough means large enough to naturally decay.
     * @param snapAnimationSpec The animation spec used to finally snap to the position.
     *
     * @return An instance of [FlingBehavior] that will perform Snapping to the next page by
     * default. The animation will be governed by the post scroll velocity and we'll use either
     * [lowVelocityAnimationSpec] or [highVelocityAnimationSpec] to approach the snapped position
     * and the last step of the animation will be performed by [snapAnimationSpec].
     */
    @Composable
    fun flingBehavior(
        state: PagerState,
        pagerSnapDistance: PagerSnapDistance = PagerSnapDistance.atMost(1),
        lowVelocityAnimationSpec: AnimationSpec<Float> = tween(
            easing = LinearEasing,
            durationMillis = LowVelocityAnimationDefaultDuration
        ),
        highVelocityAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
        snapAnimationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    ): SnapFlingBehavior {
        val density = LocalDensity.current

        return remember(
            lowVelocityAnimationSpec,
            highVelocityAnimationSpec,
            snapAnimationSpec,
            pagerSnapDistance,
            density
        ) {
            val snapLayoutInfoProvider =
                SnapLayoutInfoProvider(state, pagerSnapDistance, highVelocityAnimationSpec)
            SnapFlingBehavior(
                snapLayoutInfoProvider = snapLayoutInfoProvider,
                lowVelocityAnimationSpec = lowVelocityAnimationSpec,
                highVelocityAnimationSpec = highVelocityAnimationSpec,
                snapAnimationSpec = snapAnimationSpec,
                density = density
            )
        }
    }

    /**
     * The default implementation of Pager's pageNestedScrollConnection. All fling scroll deltas
     * will be consumed by the Pager.
     *
     * @param orientation The orientation of the pager. This will be used to determine which
     * direction it will consume everything. The other direction will not be consumed.
     */
    fun pageNestedScrollConnection(orientation: Orientation): NestedScrollConnection {
        return if (orientation == Orientation.Horizontal) {
            ConsumeHorizontalFlingNestedScrollConnection
        } else {
            ConsumeVerticalFlingNestedScrollConnection
        }
    }
}

/**
 * [PagerSnapDistance] defines the way the [Pager] will treat the distance between the current
 * page and the page where it will settle.
 */
@ExperimentalFoundationApi
@Stable
interface PagerSnapDistance {

    /** Provides a chance to change where the [Pager] fling will settle.
     *
     * @param startPage The current page right before the fling starts.
     * @param suggestedTargetPage The proposed target page where this fling will stop. This target
     * will be the page that will be correctly positioned (snapped) after naturally decaying with
     * [velocity] using a [DecayAnimationSpec].
     * @param velocity The initial fling velocity.
     * @param pageSize The page size for this [Pager].
     * @param pageSpacing The spacing used between pages.
     *
     * @return An updated target page where to settle. Note that this value needs to be between 0
     * and the total count of pages in this pager. If an invalid value is passed, the pager will
     * coerce within the valid values.
     */
    fun calculateTargetPage(
        startPage: Int,
        suggestedTargetPage: Int,
        velocity: Float,
        pageSize: Int,
        pageSpacing: Int
    ): Int

    companion object {
        /**
         * Limits the maximum number of pages that can be flung per fling gesture.
         * @param pages The maximum number of extra pages that can be flung at once.
         */
        fun atMost(pages: Int): PagerSnapDistance {
            require(pages >= 0) {
                "pages should be greater than or equal to 0. You have used $pages."
            }
            return PagerSnapDistanceMaxPages(pages)
        }
    }
}

/**
 * Limits the maximum number of pages that can be flung per fling gesture.
 * @param pagesLimit The maximum number of extra pages that can be flung at once.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class PagerSnapDistanceMaxPages(private val pagesLimit: Int) : PagerSnapDistance {
    override fun calculateTargetPage(
        startPage: Int,
        suggestedTargetPage: Int,
        velocity: Float,
        pageSize: Int,
        pageSpacing: Int,
    ): Int {
        return suggestedTargetPage.coerceIn(startPage - pagesLimit, startPage + pagesLimit)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is PagerSnapDistanceMaxPages) {
            this.pagesLimit == other.pagesLimit
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return pagesLimit.hashCode()
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun SnapLayoutInfoProvider(
    pagerState: PagerState,
    pagerSnapDistance: PagerSnapDistance,
    decayAnimationSpec: DecayAnimationSpec<Float>
): SnapLayoutInfoProvider {
    return object : SnapLayoutInfoProvider {
        val layoutInfo: LazyListLayoutInfo
            get() = pagerState.layoutInfo

        override fun Density.calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
            var lowerBoundOffset = Float.NEGATIVE_INFINITY
            var upperBoundOffset = Float.POSITIVE_INFINITY

            layoutInfo.visibleItemsInfo.fastForEach { item ->
                val offset = calculateDistanceToDesiredSnapPosition(
                    layoutInfo,
                    item,
                    SnapAlignmentStartToStart
                )

                // Find item that is closest to the snap position, but before it
                if (offset <= 0 && offset > lowerBoundOffset) {
                    lowerBoundOffset = offset
                }

                // Find item that is closest to the snap position, but after it
                if (offset >= 0 && offset < upperBoundOffset) {
                    upperBoundOffset = offset
                }
            }

            return lowerBoundOffset.rangeTo(upperBoundOffset)
        }

        override fun Density.calculateSnapStepSize(): Float = with(layoutInfo) {
            if (visibleItemsInfo.isNotEmpty()) {
                visibleItemsInfo.fastSumBy { it.size } / visibleItemsInfo.size.toFloat()
            } else {
                0f
            }
        }

        override fun Density.calculateApproachOffset(initialVelocity: Float): Float {
            val effectivePageSizePx = pagerState.pageSize + pagerState.pageSpacing
            val animationOffsetPx =
                decayAnimationSpec.calculateTargetValue(0f, initialVelocity)
            val startPage = pagerState.firstVisiblePage?.let {
                if (initialVelocity < 0) it.index + 1 else it.index
            } ?: pagerState.currentPage

            val scrollOffset =
                layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == startPage }?.offset ?: 0

            debugLog {
                "Initial Offset=$scrollOffset " +
                    "\nAnimation Offset=$animationOffsetPx " +
                    "\nFling Start Page=$startPage " +
                    "\nEffective Page Size=$effectivePageSizePx"
            }

            val targetOffsetPx = startPage * effectivePageSizePx + animationOffsetPx

            val targetPageValue = targetOffsetPx / effectivePageSizePx
            val targetPage = if (initialVelocity > 0) {
                ceil(targetPageValue)
            } else {
                floor(targetPageValue)
            }.toInt().coerceIn(0, pagerState.pageCount)

            debugLog { "Fling Target Page=$targetPage" }

            val correctedTargetPage = pagerSnapDistance.calculateTargetPage(
                startPage,
                targetPage,
                initialVelocity,
                pagerState.pageSize,
                pagerState.pageSpacing
            ).coerceIn(0, pagerState.pageCount)

            debugLog { "Fling Corrected Target Page=$correctedTargetPage" }

            val proposedFlingOffset = (correctedTargetPage - startPage) * effectivePageSizePx

            debugLog { "Proposed Fling Approach Offset=$proposedFlingOffset" }

            val flingApproachOffsetPx =
                (proposedFlingOffset.absoluteValue - scrollOffset.absoluteValue).coerceAtLeast(0)

            return if (flingApproachOffsetPx == 0) {
                flingApproachOffsetPx.toFloat()
            } else {
                flingApproachOffsetPx * initialVelocity.sign
            }.also {
                debugLog { "Fling Approach Offset=$it" }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class PagerWrapperFlingBehavior(
    val originalFlingBehavior: SnapFlingBehavior,
    val pagerState: PagerState
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        return with(originalFlingBehavior) {
            performFling(initialVelocity) { remainingScrollOffset ->
                pagerState.snapRemainingScrollOffset = remainingScrollOffset
            }
        }
    }
}

private val ConsumeHorizontalFlingNestedScrollConnection =
    ConsumeAllFlingOnDirection(Orientation.Horizontal)
private val ConsumeVerticalFlingNestedScrollConnection =
    ConsumeAllFlingOnDirection(Orientation.Vertical)

private class ConsumeAllFlingOnDirection(val orientation: Orientation) : NestedScrollConnection {

    fun Velocity.consumeOnOrientation(orientation: Orientation): Velocity {
        return if (orientation == Orientation.Vertical) {
            copy(x = 0f)
        } else {
            copy(y = 0f)
        }
    }

    fun Offset.consumeOnOrientation(orientation: Orientation): Offset {
        return if (orientation == Orientation.Vertical) {
            copy(x = 0f)
        } else {
            copy(y = 0f)
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        return when (source) {
            NestedScrollSource.Fling -> available.consumeOnOrientation(orientation)
            else -> Offset.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return available.consumeOnOrientation(orientation)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("ComposableModifierFactory")
@Composable
private fun Modifier.pagerSemantics(state: PagerState, isVertical: Boolean): Modifier {
    val scope = rememberCoroutineScope()
    fun performForwardPaging(): Boolean {
        return if (state.canScrollForward) {
            scope.launch {
                state.animateToNextPage()
            }
            true
        } else {
            false
        }
    }

    fun performBackwardPaging(): Boolean {
        return if (state.canScrollBackward) {
            scope.launch {
                state.animateToPreviousPage()
            }
            true
        } else {
            false
        }
    }

    return this.then(Modifier.semantics {
        if (isVertical) {
            pageUp { performBackwardPaging() }
            pageDown { performForwardPaging() }
        } else {
            pageLeft { performBackwardPaging() }
            pageRight { performForwardPaging() }
        }
    })
}

private const val DEBUG = false
private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("Pager: ${generateMsg()}")
    }
}

private const val LowVelocityAnimationDefaultDuration = 500