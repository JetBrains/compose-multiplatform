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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

/**
 * State of the persistent bottom sheet in [BottomSheetScaffold].
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@ExperimentalMaterialApi
@Stable
class BottomSheetState(
    initialValue: BottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmStateChange: (BottomSheetValue) -> Boolean = { true }
) : SwipeableState<BottomSheetValue>(
    initialValue = initialValue,
    animationSpec = animationSpec,
    confirmStateChange = confirmStateChange
) {
    /**
     * Whether the bottom sheet is expanded.
     */
    val isExpanded: Boolean
        get() = currentValue == Expanded

    /**
     * Whether the bottom sheet is collapsed.
     */
    val isCollapsed: Boolean
        get() = currentValue == BottomSheetValue.Collapsed

    /**
     * Expand the bottom sheet with animation and suspend until it if fully expanded or animation
     * has been cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the expand animation ended
     */
    suspend fun expand() = animateTo(Expanded)

    /**
     * Collapse the bottom sheet with animation and suspend until it if fully collapsed or animation
     * has been cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the collapse animation ended
     */
    suspend fun collapse() = animateTo(BottomSheetValue.Collapsed)

    companion object {
        /**
         * The default [Saver] implementation for [BottomSheetState].
         */
        fun Saver(
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (BottomSheetValue) -> Boolean
        ): Saver<BottomSheetState, *> = Saver(
            save = { it.currentValue },
            restore = {
                BottomSheetState(
                    initialValue = it,
                    animationSpec = animationSpec,
                    confirmStateChange = confirmStateChange
                )
            }
        )
    }

    internal val nestedScrollConnection = this.PreUpPostDownNestedScrollConnection
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
            confirmStateChange = confirmStateChange
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
    bottomSheetState: BottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed),
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
 * @param sheetPeekHeight The height of the bottom sheet when it is collapsed.
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
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier) {
        val fullHeight = constraints.maxHeight.toFloat()
        val peekHeightPx = with(LocalDensity.current) { sheetPeekHeight.toPx() }
        var bottomSheetHeight by remember { mutableStateOf(fullHeight) }

        val swipeable = Modifier
            .nestedScroll(scaffoldState.bottomSheetState.nestedScrollConnection)
            .swipeable(
                state = scaffoldState.bottomSheetState,
                anchors = mapOf(
                    fullHeight - peekHeightPx to BottomSheetValue.Collapsed,
                    fullHeight - bottomSheetHeight to Expanded
                ),
                orientation = Orientation.Vertical,
                enabled = sheetGesturesEnabled,
                resistance = null
            )
            .semantics {
                if (peekHeightPx != bottomSheetHeight) {
                    if (scaffoldState.bottomSheetState.isCollapsed) {
                        expand {
                            scope.launch {
                                if (scaffoldState.bottomSheetState.confirmStateChange(Expanded)) {
                                    scaffoldState.bottomSheetState.expand()
                                }
                            }
                            true
                        }
                    } else {
                        collapse {
                            scope.launch {
                                if (scaffoldState.bottomSheetState.confirmStateChange(Collapsed)) {
                                    scaffoldState.bottomSheetState.collapse()
                                }
                            }
                            true
                        }
                    }
                }
            }

        val child = @Composable {
            BottomSheetScaffoldStack(
                body = {
                    Surface(
                        color = backgroundColor,
                        contentColor = contentColor
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            topBar?.invoke()
                            content(PaddingValues(bottom = sheetPeekHeight))
                        }
                    }
                },
                bottomSheet = {
                    Surface(
                        swipeable
                            .fillMaxWidth()
                            .requiredHeightIn(min = sheetPeekHeight)
                            .onGloballyPositioned {
                                bottomSheetHeight = it.size.height.toFloat()
                            },
                        shape = sheetShape,
                        elevation = sheetElevation,
                        color = sheetBackgroundColor,
                        contentColor = sheetContentColor,
                        content = { Column(content = sheetContent) }
                    )
                },
                floatingActionButton = {
                    Box {
                        floatingActionButton?.invoke()
                    }
                },
                snackbarHost = {
                    Box {
                        snackbarHost(scaffoldState.snackbarHostState)
                    }
                },
                bottomSheetOffset = scaffoldState.bottomSheetState.offset,
                floatingActionButtonPosition = floatingActionButtonPosition
            )
        }
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

@Composable
private fun BottomSheetScaffoldStack(
    body: @Composable () -> Unit,
    bottomSheet: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    bottomSheetOffset: State<Float>,
    floatingActionButtonPosition: FabPosition
) {
    Layout(
        content = {
            body()
            bottomSheet()
            floatingActionButton()
            snackbarHost()
        }
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)

        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)

            val (sheetPlaceable, fabPlaceable, snackbarPlaceable) =
                measurables.drop(1).map {
                    it.measure(constraints.copy(minWidth = 0, minHeight = 0))
                }

            val sheetOffsetY = bottomSheetOffset.value.roundToInt()

            sheetPlaceable.placeRelative(0, sheetOffsetY)

            val fabOffsetX = when (floatingActionButtonPosition) {
                FabPosition.Center -> (placeable.width - fabPlaceable.width) / 2
                else -> placeable.width - fabPlaceable.width - FabEndSpacing.roundToPx()
            }
            val fabOffsetY = sheetOffsetY - fabPlaceable.height / 2

            fabPlaceable.placeRelative(fabOffsetX, fabOffsetY)

            val snackbarOffsetX = (placeable.width - snackbarPlaceable.width) / 2
            val snackbarOffsetY = placeable.height - snackbarPlaceable.height

            snackbarPlaceable.placeRelative(snackbarOffsetX, snackbarOffsetY)
        }
    }
}

private val FabEndSpacing = 16.dp

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
