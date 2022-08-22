/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.compose.material
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material.BottomSheetValue.Collapsed
import androidx.compose.material.BottomSheetValue.Expanded
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Possible values of [BottomSheetState].
 */
@ExperimentalMaterialApi
enum class BottomSheetValue {
    /**
     * The bottom sheet is visible, but only showing its peek height.
     */
    Collapsed,
    /**
     * The bottom sheet is visible at its maximum height.
     */
    Expanded
}

@Deprecated(
    message = "This constructor is deprecated. confirmStateChange has been renamed to " +
        "confirmValueChange.",
    replaceWith = ReplaceWith("BottomSheetScaffoldState(initialValue, animationSpec, " +
        "confirmStateChange)")
)
@ExperimentalMaterialApi
fun BottomSheetScaffoldState(
    initialValue: BottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmStateChange: (BottomSheetValue) -> Boolean
) = BottomSheetState(
    initialValue = initialValue,
    animationSpec = animationSpec,
    confirmValueChange = confirmStateChange
)

/**
 * State of the persistent bottom sheet in [BottomSheetScaffold].
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@ExperimentalMaterialApi
@Stable
class BottomSheetState(
    initialValue: BottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmValueChange: (BottomSheetValue) -> Boolean = { true }
) {
    internal val swipeableState = SwipeableV2State(
        initialValue = initialValue,
        animationSpec = animationSpec,
        confirmValueChange = confirmValueChange
    )

    val currentValue: BottomSheetValue
        get() = swipeableState.currentValue

    /**
     * Whether the bottom sheet is expanded.
     */
    val isExpanded: Boolean
        get() = swipeableState.currentValue == Expanded

    /**
     * Whether the bottom sheet is collapsed.
     */
    val isCollapsed: Boolean
        get() = swipeableState.currentValue == Collapsed

    /**
     * The fraction of the progress going from [currentValue] to the targetValue, within [0f..1f]
     * bounds, or 1f if the sheet is in a settled state.
     */
    /*@FloatRange(from = 0f, to = 1f)*/
    val progress: Float
        get() = swipeableState.progress

    /**
     * Expand the bottom sheet with an animation and suspend until the animation finishes or is
     * cancelled.
     * Note: If the peek height is equal to the sheet height, this method will animate to the
     * [Collapsed] state.
     *
     * This method will throw [CancellationException] if the animation is interrupted.
     */
    suspend fun expand() {
        val target = if (swipeableState.hasAnchorForValue(Expanded)) Expanded else Collapsed
        swipeableState.animateTo(target)
    }

    /**
     * Collapse the bottom sheet with animation and suspend until it if fully collapsed or animation
     * has been cancelled. This method will throw [CancellationException] if the animation is
     * interrupted.
     */
    suspend fun collapse() = swipeableState.animateTo(Collapsed)

    @Deprecated(
        message = "Use requireOffset() to access the offset.",
        replaceWith = ReplaceWith("requireOffset()")
    )
    val offset: Float get() = error("Use requireOffset() to access the offset.")

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    fun requireOffset() = swipeableState.requireOffset()

    internal suspend fun animateTo(
        target: BottomSheetValue,
        velocity: Float = swipeableState.lastVelocity
    ) = swipeableState.animateTo(target, velocity)

    internal suspend fun snapTo(target: BottomSheetValue) = swipeableState.snapTo(target)

    internal val isAnimationRunning: Boolean get() = swipeableState.isAnimationRunning

    companion object {
        /**
         * The default [Saver] implementation for [BottomSheetState].
         */
        fun Saver(
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (BottomSheetValue) -> Boolean
        ): Saver<BottomSheetState, *> = Saver(
            save = { it.swipeableState.currentValue },
            restore = {
                BottomSheetState(
                    initialValue = it,
                    animationSpec = animationSpec,
                    confirmValueChange = confirmStateChange
                )
            }
        )
    }
}
/**
 * Create a [BottomSheetState] and [remember] it.
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterialApi
fun rememberBottomSheetState(
    initialValue: BottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmStateChange: (BottomSheetValue) -> Boolean = { true }
): BottomSheetState {
    return rememberSaveable(
        animationSpec,
        saver = BottomSheetState.Saver(
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange
        )
    ) {
        BottomSheetState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = confirmStateChange
        )
    }
}
/**
 * State of the [BottomSheetScaffold] composable.
 *
 * @param drawerState The state of the navigation drawer.
 * @param bottomSheetState The state of the persistent bottom sheet.
 * @param snackbarHostState The [SnackbarHostState] used to show snackbars inside the scaffold.
 */
@ExperimentalMaterialApi
@Stable
class BottomSheetScaffoldState(
    val drawerState: DrawerState,
    val bottomSheetState: BottomSheetState,
    val snackbarHostState: SnackbarHostState
)
/**
 * Create and [remember] a [BottomSheetScaffoldState].
 *
 * @param drawerState The state of the navigation drawer.
 * @param bottomSheetState The state of the persistent bottom sheet.
 * @param snackbarHostState The [SnackbarHostState] used to show snackbars inside the scaffold.
 */
@Composable
@ExperimentalMaterialApi
fun rememberBottomSheetScaffoldState(
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    bottomSheetState: BottomSheetState = rememberBottomSheetState(Collapsed),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): BottomSheetScaffoldState {
    return remember(drawerState, bottomSheetState, snackbarHostState) {
        BottomSheetScaffoldState(
            drawerState = drawerState,
            bottomSheetState = bottomSheetState,
            snackbarHostState = snackbarHostState
        )
    }
}
/**
 * <a href="https://material.io/components/sheets-bottom#standard-bottom-sheet" class="external" target="_blank">Material Design standard bottom sheet</a>.
 *
 * Standard bottom sheets co-exist with the screen’s main UI region and allow for simultaneously
 * viewing and interacting with both regions. They are commonly used to keep a feature or
 * secondary content visible on screen when content in main UI region is frequently scrolled or
 * panned.
 *
 * ![Standard bottom sheet image](https://developer.android.com/images/reference/androidx/compose/material/standard-bottom-sheet.png)
 *
 * This component provides an API to put together several material components to construct your
 * screen. For a similar component which implements the basic material design layout strategy
 * with app bars, floating action buttons and navigation drawers, use the standard [Scaffold].
 * For similar component that uses a backdrop as the centerpiece of the screen, use
 * [BackdropScaffold].
 *
 * A simple example of a bottom sheet scaffold looks like this:
 *
 * @sample androidx.compose.material.samples.BottomSheetScaffoldSample
 *
 * @param sheetContent The content of the bottom sheet.
 * @param modifier An optional [Modifier] for the root of the scaffold.
 * @param scaffoldState The state of the scaffold.
 * @param topBar An optional top app bar.
 * @param snackbarHost The composable hosting the snackbars shown inside the scaffold.
 * @param floatingActionButton An optional floating action button.
 * @param floatingActionButtonPosition The position of the floating action button.
 * @param sheetGesturesEnabled Whether the bottom sheet can be interacted with by gestures.
 * @param sheetShape The shape of the bottom sheet.
 * @param sheetElevation The elevation of the bottom sheet.
 * @param sheetBackgroundColor The background color of the bottom sheet.
 * @param sheetContentColor The preferred content color provided by the bottom sheet to its
 * children. Defaults to the matching content color for [sheetBackgroundColor], or if that is
 * not a color from the theme, this will keep the same content color set above the bottom sheet.
 * @param sheetPeekHeight The height of the bottom sheet when it is collapsed. If the peek height
 * equals the sheet's full height, the sheet will only have a collapsed state.
 * @param drawerContent The content of the drawer sheet.
 * @param drawerGesturesEnabled Whether the drawer sheet can be interacted with by gestures.
 * @param drawerShape The shape of the drawer sheet.
 * @param drawerElevation The elevation of the drawer sheet.
 * @param drawerBackgroundColor The background color of the drawer sheet.
 * @param drawerContentColor The preferred content color provided by the drawer sheet to its
 * children. Defaults to the matching content color for [drawerBackgroundColor], or if that is
 * not a color from the theme, this will keep the same content color set above the drawer sheet.
 * @param drawerScrimColor The color of the scrim that is applied when the drawer is open.
 * @param content The main content of the screen. You should use the provided [PaddingValues]
 * to properly offset the content, so that it is not obstructed by the bottom sheet when collapsed.
 */
@Composable
@ExperimentalMaterialApi
fun BottomSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    topBar: (@Composable () -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    floatingActionButton: (@Composable () -> Unit)? = null,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    sheetGesturesEnabled: Boolean = true,
    sheetShape: Shape = MaterialTheme.shapes.large,
    sheetElevation: Dp = BottomSheetScaffoldDefaults.SheetElevation,
    sheetBackgroundColor: Color = MaterialTheme.colors.surface,
    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
    sheetPeekHeight: Dp = BottomSheetScaffoldDefaults.SheetPeekHeight,
    drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    drawerGesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    drawerScrimColor: Color = DrawerDefaults.scrimColor,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    content: @Composable (PaddingValues) -> Unit
) {
    val peekHeightPx = with(LocalDensity.current) { sheetPeekHeight.toPx() }
    val child = @Composable {
        BottomSheetScaffoldLayout(
            topBar = topBar,
            body = content,
            bottomSheet = { layoutHeight ->
                BottomSheet(
                    state = scaffoldState.bottomSheetState,
                    modifier = Modifier
                        .nestedScroll(
                            remember(scaffoldState.bottomSheetState.swipeableState) {
                                ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                                    state = scaffoldState.bottomSheetState.swipeableState,
                                    orientation = Orientation.Vertical
                                )
                            }
                        )
                        .fillMaxWidth()
                        .requiredHeightIn(min = sheetPeekHeight),
                    anchors = { state, sheetSize ->
                        when (state) {
                            Collapsed -> layoutHeight - peekHeightPx
                            Expanded -> if (sheetSize.height == peekHeightPx.roundToInt()) {
                                null
                            } else {
                                layoutHeight - sheetSize.height.toFloat()
                            }
                        }
                    },
                    sheetBackgroundColor = sheetBackgroundColor,
                    sheetContentColor = sheetContentColor,
                    sheetElevation = sheetElevation,
                    sheetGesturesEnabled = sheetGesturesEnabled,
                    sheetShape = sheetShape,
                    content = sheetContent
                )
            },
            floatingActionButton = floatingActionButton,
            snackbarHost = {
                snackbarHost(scaffoldState.snackbarHostState)
            },
            sheetOffset = { scaffoldState.bottomSheetState.requireOffset() },
            sheetPeekHeight = sheetPeekHeight,
            sheetState = scaffoldState.bottomSheetState,
            floatingActionButtonPosition = floatingActionButtonPosition
        )
    }
    Surface(
        modifier
            .fillMaxSize(),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        if (drawerContent == null) {
            child()
        } else {
            ModalDrawer(
                drawerContent = drawerContent,
                drawerState = scaffoldState.drawerState,
                gesturesEnabled = drawerGesturesEnabled,
                drawerShape = drawerShape,
                drawerElevation = drawerElevation,
                drawerBackgroundColor = drawerBackgroundColor,
                drawerContentColor = drawerContentColor,
                scrimColor = drawerScrimColor,
                content = child
            )
        }
    }
}
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheet(
    state: BottomSheetState,
    sheetGesturesEnabled: Boolean,
    anchors: (state: BottomSheetValue, sheetSize: IntSize) -> Float?,
    sheetShape: Shape,
    sheetElevation: Dp,
    sheetBackgroundColor: Color,
    sheetContentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val anchorChangeHandler = remember(state, scope) {
        BottomSheetScaffoldAnchorChangeHandler(
            state = state,
            animateTo = { target -> scope.launch { state.animateTo(target) } },
            snapTo = { target -> scope.launch { state.snapTo(target) } }
        )
    }
    Surface(
        modifier
            .swipeableV2(
                state = state.swipeableState,
                orientation = Orientation.Vertical,
                enabled = sheetGesturesEnabled,
            )
            .swipeAnchors(
                state = state.swipeableState,
                possibleValues = setOf(Collapsed, Expanded),
                calculateAnchor = anchors,
                anchorChangeHandler = anchorChangeHandler
            )
            .semantics {
                // If we don't have anchors yet, or have only one anchor we don't want any
                // accessibility actions
                if (state.swipeableState.anchors.size > 1) {
                    if (state.isCollapsed) {
                        expand {
                            if (state.swipeableState.confirmValueChange(Expanded)) {
                                scope.launch { state.expand() }
                            }
                            true
                        }
                    } else {
                        collapse {
                            if (state.swipeableState.confirmValueChange(Collapsed)) {
                                scope.launch { state.collapse() }
                            }
                            true
                        }
                    }
                }
            },
        shape = sheetShape,
        elevation = sheetElevation,
        color = sheetBackgroundColor,
        contentColor = sheetContentColor,
        content = { Column(content = content) }
    )
}

/**
 * Contains useful defaults for [BottomSheetScaffold].
 */
object BottomSheetScaffoldDefaults {
    /**
     * The default elevation used by [BottomSheetScaffold].
     */
    val SheetElevation = 8.dp
    /**
     * The default peek height used by [BottomSheetScaffold].
     */
    val SheetPeekHeight = 56.dp
}
private enum class BottomSheetScaffoldLayoutSlot { TopBar, Body, Sheet, Fab, Snackbar }
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheetScaffoldLayout(
    topBar: @Composable (() -> Unit)?,
    body: @Composable (innerPadding: PaddingValues) -> Unit,
    bottomSheet: @Composable (layoutHeight: Int) -> Unit,
    floatingActionButton: (@Composable () -> Unit)?,
    snackbarHost: @Composable () -> Unit,
    sheetPeekHeight: Dp,
    floatingActionButtonPosition: FabPosition,
    sheetOffset: () -> Float,
    sheetState: BottomSheetState,
) {
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val sheetPlaceable = subcompose(BottomSheetScaffoldLayoutSlot.Sheet) {
            bottomSheet(layoutHeight)
        }[0].measure(looseConstraints)
        val sheetOffsetY = sheetOffset().roundToInt()
        val topBarPlaceable = topBar?.let {
            subcompose(BottomSheetScaffoldLayoutSlot.TopBar) { topBar() }[0]
                .measure(looseConstraints)
        }
        val topBarHeight = topBarPlaceable?.height ?: 0
        val bodyConstraints = looseConstraints.copy(maxHeight = layoutHeight - topBarHeight)
        val bodyPlaceable = subcompose(BottomSheetScaffoldLayoutSlot.Body) {
            body(PaddingValues(bottom = sheetPeekHeight))
        }[0].measure(bodyConstraints)
        val fabPlaceable = floatingActionButton?.let { fab ->
            subcompose(BottomSheetScaffoldLayoutSlot.Fab, fab)[0].measure(looseConstraints)
        }
        val fabWidth = fabPlaceable?.width ?: 0
        val fabHeight = fabPlaceable?.height ?: 0
        val fabOffsetX = when (floatingActionButtonPosition) {
            FabPosition.Center -> (layoutWidth - fabWidth) / 2
            else -> layoutWidth - fabWidth - FabSpacing.roundToPx()
        }
        // In case sheet peek height < (FAB height / 2), give the FAB some minimum space
        val fabOffsetY = if (sheetPeekHeight.toPx() < fabHeight / 2) {
            sheetOffsetY - fabHeight - FabSpacing.roundToPx()
        } else sheetOffsetY - (fabHeight / 2)
        val snackbarPlaceable = subcompose(BottomSheetScaffoldLayoutSlot.Snackbar, snackbarHost)[0]
            .measure(looseConstraints)
        val snackbarOffsetX = (layoutWidth - snackbarPlaceable.width) / 2
        val snackbarOffsetY = when (sheetState.currentValue) {
            Collapsed -> fabOffsetY - snackbarPlaceable.height
            Expanded -> layoutHeight - snackbarPlaceable.height
        }
        layout(layoutWidth, layoutHeight) {
            // Placement order is important for elevation
            bodyPlaceable.placeRelative(0, topBarHeight)
            topBarPlaceable?.placeRelative(0, 0)
            sheetPlaceable.placeRelative(0, sheetOffsetY)
            fabPlaceable?.placeRelative(fabOffsetX, fabOffsetY)
            snackbarPlaceable.placeRelative(snackbarOffsetX, snackbarOffsetY)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
private fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    state: SwipeableV2State<*>,
    orientation: Orientation
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.toFloat()
        return if (delta < 0 && source == NestedScrollSource.Drag) {
            state.dispatchRawDelta(delta).toOffset()
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        return if (source == NestedScrollSource.Drag) {
            state.dispatchRawDelta(available.toFloat()).toOffset()
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val toFling = available.toFloat()
        val currentOffset = state.requireOffset()
        return if (toFling < 0 && currentOffset > state.minOffset) {
            state.settle(velocity = toFling)
            // since we go to the anchor with tween settling, consume all for the best UX
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        state.settle(velocity = available.toFloat())
        return available
    }

    private fun Float.toOffset(): Offset = Offset(
        x = if (orientation == Orientation.Horizontal) this else 0f,
        y = if (orientation == Orientation.Vertical) this else 0f
    )

    @JvmName("velocityToFloat")
    private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

    @JvmName("offsetToFloat")
    private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
}

@OptIn(ExperimentalMaterialApi::class)
private fun BottomSheetScaffoldAnchorChangeHandler(
    state: BottomSheetState,
    animateTo: (target: BottomSheetValue) -> Unit,
    snapTo: (target: BottomSheetValue) -> Unit,
) = AnchorChangeHandler<BottomSheetValue> { previousTarget, previousAnchors, newAnchors ->
    val previousTargetOffset = previousAnchors[previousTarget]
    val newTarget = when (previousTarget) {
        Collapsed -> Collapsed
        Expanded -> if (newAnchors.containsKey(Expanded)) Expanded else Collapsed
    }
    val newTargetOffset = newAnchors.getValue(newTarget)
    if (newTargetOffset != previousTargetOffset) {
        if (state.isAnimationRunning) {
            // Re-target the animation to the new offset if it changed
            animateTo(newTarget)
        } else {
            // Snap to the new offset value of the target if no animation was running
            snapTo(newTarget)
        }
    }
}

private val FabSpacing = 16.dp