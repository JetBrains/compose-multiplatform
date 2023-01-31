/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import android.content.ComponentCallbacks2
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import junit.framework.TestCase.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class ModalBottomSheetTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val sheetHeight = 256.dp
    private val sheetTag = "sheetContentTag"
    private val BackTestTag = "Back"

    @Test
    fun modalBottomSheet_isDismissedOnTapOutside() {
        var showBottomSheet by mutableStateOf(true)

        rule.setContent {
            if (showBottomSheet) {
                ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
                    Box(
                        Modifier
                            .size(sheetHeight)
                            .testTag(sheetTag)
                    )
                }
            }
        }
        rule.onNodeWithTag(sheetTag).assertIsDisplayed()

        val outsideY = with(rule.density) {
            rule.onAllNodes(isPopup()).onFirst().getUnclippedBoundsInRoot().height.roundToPx() / 4
        }

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(0, outsideY)
        rule.waitForIdle()

        // Bottom sheet should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_fillsScreenWidth() {
        var boxWidth = 0
        var screenWidth by mutableStateOf(0)

        rule.setContent {
            val context = LocalContext.current
            val density = LocalDensity.current
            val resScreenWidth = context.resources.configuration.screenWidthDp
            with(density) { screenWidth = resScreenWidth.dp.roundToPx() }

            ModalBottomSheet(onDismissRequest = {}) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .onSizeChanged { boxWidth = it.width }
                )
            }
        }
        assertThat(boxWidth).isEqualTo(screenWidth)
    }

    @Test
    fun modalBottomSheet_wideScreen_sheetRespectsMaxWidthAndIsCentered() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val latch = CountDownLatch(1)

        rule.activity.application.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(p0: Configuration) {
                latch.countDown()
            }

            override fun onLowMemory() {
                // NO-OP
            }

            override fun onTrimMemory(p0: Int) {
                // NO-OP
            }
        })

        try {
            latch.await(1500, TimeUnit.MILLISECONDS)
            rule.setContent {
                ModalBottomSheet(onDismissRequest = {}) {
                    Box(
                        Modifier
                            .testTag(sheetTag)
                            .fillMaxHeight(0.4f)
                    )
                }
            }

            val simulatedRootWidth = rule.onNode(isPopup()).getUnclippedBoundsInRoot().width
            val maxSheetWidth = 640.dp
            val expectedSheetWidth = maxSheetWidth.coerceAtMost(simulatedRootWidth)
            // Our sheet should be max 640 dp but fill the width if the container is less wide
            val expectedSheetLeft = if (simulatedRootWidth <= expectedSheetWidth) {
                0.dp
            } else {
                (simulatedRootWidth - expectedSheetWidth) / 2
            }

            rule.onNodeWithTag(sheetTag)
                .onParent()
                .assertLeftPositionInRootIsEqualTo(
                    expectedLeft = expectedSheetLeft
                )
                .assertWidthIsEqualTo(expectedSheetWidth)
        } catch (e: InterruptedException) {
            fail("Unable to verify sheet width in landscape orientation")
        } finally {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @Test
    fun modalBottomSheet_defaultStateForSmallContentIsFullExpanded() {
        lateinit var sheetState: SheetState

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState, dragHandle = null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .testTag(sheetTag)
                        .height(sheetHeight)
                )
            }
        }

        val height = rule.onNode(isPopup()).getUnclippedBoundsInRoot().height
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        rule.onNodeWithTag(sheetTag).assertTopPositionInRootIsEqualTo(height - sheetHeight)
    }

    @Test
    fun modalBottomSheet_defaultStateForLargeContentIsHalfExpanded() {
        lateinit var sheetState: SheetState
        var screenHeightPx by mutableStateOf(0f)

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            val context = LocalContext.current
            val density = LocalDensity.current
            val resScreenHeight = context.resources.configuration.screenHeightDp
            with(density) {
                screenHeightPx = resScreenHeight.dp.roundToPx().toFloat()
            }
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag))
            }
        }
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        assertThat(sheetState.requireOffset())
            .isWithin(1f)
            .of(screenHeightPx / 2f)
    }

    @Test
    fun modalBottomSheet_isDismissedOnBackPress() {
        var showBottomSheet by mutableStateOf(true)
        rule.setContent {
            val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            if (showBottomSheet) {
                ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
                    Box(
                        Modifier
                            .size(sheetHeight)
                            .testTag(sheetTag)) {
                        Button(
                            onClick = { dispatcher.onBackPressed() },
                            modifier = Modifier.testTag(BackTestTag),
                            content = { Text("Content") },
                        )
                    }
                }
            }
        }

        // Popup should be visible
        rule.onNodeWithTag(sheetTag).assertIsDisplayed()

        rule.onNodeWithTag(BackTestTag).performClick()
        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()

        // Popup should not exist
        rule.onNodeWithTag(sheetTag).assertDoesNotExist()
    }

    @Test
    fun modalBottomSheet_shortSheet_sizeChanges_snapsToNewTarget() {
        lateinit var state: SheetState
        var size by mutableStateOf(56.dp)
        var screenHeight by mutableStateOf(0.dp)
        val expectedExpandedAnchor by derivedStateOf {
            with(rule.density) {
                (screenHeight - size).toPx()
            }
        }

        rule.setContent {
            val context = LocalContext.current
            screenHeight = context.resources.configuration.screenHeightDp.dp
            state = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null
            ) {
                Box(
                    Modifier
                        .height(size)
                        .fillMaxWidth()
                )
            }
        }
        assertThat(state.requireOffset()).isWithin(0.5f).of(expectedExpandedAnchor)

        size = 100.dp
        rule.waitForIdle()
        assertThat(state.requireOffset()).isWithin(0.5f).of(expectedExpandedAnchor)

        size = 30.dp
        rule.waitForIdle()
        assertThat(state.requireOffset()).isWithin(0.5f).of(expectedExpandedAnchor)
    }

    @Test
    fun modalBottomSheet_emptySheet_expandDoesNotAnimate() {
        lateinit var state: SheetState
        lateinit var scope: CoroutineScope
        rule.setContent {
            state = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()

            ModalBottomSheet(onDismissRequest = {}, sheetState = state, dragHandle = null) {}
        }
        assertThat(state.swipeableState.currentValue).isEqualTo(SheetValue.Hidden)
        val hiddenOffset = state.requireOffset()
        scope.launch { state.show() }
        rule.waitForIdle()

        assertThat(state.swipeableState.currentValue).isEqualTo(SheetValue.Expanded)
        val expandedOffset = state.requireOffset()

        assertThat(hiddenOffset).isEqualTo(expandedOffset)
    }

    @Test
    fun modalBottomSheet_anchorsChange_retainsCurrentValue() {
        lateinit var state: SheetState
        var amountOfItems by mutableStateOf(0)
        lateinit var scope: CoroutineScope
        rule.setContent {
            state = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
            ) {
                scope = rememberCoroutineScope()
                LazyColumn {
                    items(amountOfItems) {
                        ListItem(headlineContent = { Text("$it") })
                    }
                }
            }
        }

        assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)

        amountOfItems = 50
        rule.waitForIdle()
        scope.launch {
            state.show()
        }
        // The anchors should now be {Hidden, PartiallyExpanded, Expanded}

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        amountOfItems = 100 // The anchors should now be {Hidden, PartiallyExpanded, Expanded}

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded) // We should
        // retain the current value if possible
        assertThat(state.swipeableState.anchors).containsKey(SheetValue.Hidden)
        assertThat(state.swipeableState.anchors).containsKey(SheetValue.PartiallyExpanded)
        assertThat(state.swipeableState.anchors).containsKey(SheetValue.Expanded)

        amountOfItems = 0 // When the sheet height is 0, we should only have a hidden anchor
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)
        assertThat(state.swipeableState.anchors).containsKey(SheetValue.Hidden)
        assertThat(state.swipeableState.anchors)
            .doesNotContainKey(SheetValue.PartiallyExpanded)
        assertThat(state.swipeableState.anchors).doesNotContainKey(SheetValue.Expanded)
    }

    @Test
    fun modalBottomSheet_nestedScroll_consumesWithinBounds_scrollsOutsideBounds() {
        lateinit var sheetState: SheetState
        lateinit var scrollState: ScrollState
        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
            ) {
                scrollState = rememberScrollState()
                Column(
                    Modifier
                        .verticalScroll(scrollState)
                        .testTag(sheetTag)
                ) {
                    repeat(100) {
                        Text(it.toString(), Modifier.requiredHeight(50.dp))
                    }
                }
            }
        }

        rule.waitForIdle()

        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeUp(startY = bottom, endY = bottom / 2)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeUp(startY = bottom, endY = top)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isGreaterThan(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeDown(startY = top, endY = bottom)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeDown(startY = top, endY = bottom / 2)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        rule.onNodeWithTag(sheetTag)
            .performTouchInput {
                swipeDown(startY = bottom / 2, endY = bottom)
            }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
    }

    @Test
    fun modalBottomSheet_missingAnchors_findsClosest() {
        val topTag = "ModalBottomSheetLayout"
        var showShortContent by mutableStateOf(false)
        val sheetState = SheetState(skipPartiallyExpanded = false)
        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            ModalBottomSheet(
                onDismissRequest = {},
                modifier = Modifier.testTag(topTag),
                sheetState = sheetState,
            ) {
                if (showShortContent) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(sheetTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(topTag).performTouchInput {
            swipeDown()
            swipeDown()
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        }

        showShortContent = true
        scope.launch { sheetState.show() } // We can't use LaunchedEffect with Swipeable in tests
        // yet, so we're invoking this outside of composition. See b/254115946.

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }
    }

    @Test
    fun modalBottomSheet_expandBySwiping() {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeUp() }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }
    }

    @Test
    fun modalBottomSheet_respectsConfirmStateChange() {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState(
                confirmValueChange = { newState ->
                    newState != SheetValue.Hidden
                }
            )
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        rule.onNodeWithTag(sheetTag).onParent()
            .performSemanticsAction(SemanticsActions.Dismiss)

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }
    }

    @Test
    fun modalBottomSheet_hideBySwiping_tallBottomSheet() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope
        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

        scope.launch { sheetState.expand() }
        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        }
    }

    @Test
    fun modalBottomSheet_hideBySwiping_skipPartiallyExpanded() {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .testTag(sheetTag)
                )
            }
        }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        }

        rule.onNodeWithTag(sheetTag)
            .performTouchInput { swipeDown() }

        rule.runOnIdle {
            assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        }
    }

    @Test
    fun modalBottomSheet_hideManually_skipPartiallyExpanded(): Unit = runBlocking(
        AutoTestFrameClock()
    ) {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }
        assertThat(sheetState.currentValue == SheetValue.Expanded)

        sheetState.hide()

        assertThat(sheetState.currentValue == SheetValue.Hidden)
    }

    @Test
    fun modalBottomSheet_testDismissAction_tallBottomSheet_whenPartiallyExpanded() {
        rule.setContent {
            ModalBottomSheet(onDismissRequest = {}) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }

        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)
    }

    @Test
    fun modalBottomSheet_testExpandAction_tallBottomSheet_whenHalfExpanded() {
        lateinit var sheetState: SheetState
        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }

        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Expand)

        rule.runOnIdle {
            assertThat(sheetState.requireOffset()).isEqualTo(0f)
        }
    }

    @Test
    fun modalBottomSheet_testDismissAction_tallBottomSheet_whenExpanded() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope

        var screenHeightPx by mutableStateOf(0f)

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()
            val context = LocalContext.current
            val density = LocalDensity.current
            val resScreenHeight = context.resources.configuration.screenHeightDp
            with(density) {
                screenHeightPx = resScreenHeight.dp.roundToPx().toFloat()
            }

            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }
        scope.launch {
            sheetState.expand()
        }
        rule.waitForIdle()

        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)

        rule.runOnIdle {
            assertThat(sheetState.requireOffset()).isWithin(1f).of(screenHeightPx)
        }
    }

    @Test
    fun modalBottomSheet_testCollapseAction_tallBottomSheet_whenExpanded() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope

        var screenHeightPx by mutableStateOf(0f)

        rule.setContent {
            sheetState = rememberModalBottomSheetState()
            scope = rememberCoroutineScope()
            val context = LocalContext.current
            val density = LocalDensity.current
            val resScreenHeight = context.resources.configuration.screenHeightDp
            with(density) {
                screenHeightPx = resScreenHeight.dp.roundToPx().toFloat()
            }

            ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag(sheetTag)
                )
            }
        }
        scope.launch {
            sheetState.expand()
        }
        rule.waitForIdle()

        rule.onNodeWithTag(sheetTag).onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Collapse)

        rule.runOnIdle {
            assertThat(sheetState.requireOffset()).isWithin(1f).of(screenHeightPx / 2)
        }
    }

    @Test
    fun modalBottomSheet_shortSheet_anchorChangeHandler_previousTargetNotInAnchors_reconciles() {
        val sheetState = SheetState(skipPartiallyExpanded = false)
        var hasSheetContent by mutableStateOf(false) // Start out with empty sheet content
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = null,
            ) {
                if (hasSheetContent) {
                    Box(Modifier.fillMaxHeight(0.4f))
                }
            }
        }

        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        assertThat(sheetState.swipeableState.hasAnchorForValue(SheetValue.PartiallyExpanded))
            .isFalse()
        assertThat(sheetState.swipeableState.hasAnchorForValue(SheetValue.Expanded))
            .isFalse()

        scope.launch { sheetState.show() }
        rule.waitForIdle()

        assertThat(sheetState.isVisible).isTrue()
        assertThat(sheetState.currentValue).isEqualTo(sheetState.targetValue)

        hasSheetContent = true // Recompose with sheet content
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
    }

    @Test
    fun modalBottomSheet_tallSheet_anchorChangeHandler_previousTargetNotInAnchors_reconciles() {
        val sheetState = SheetState(skipPartiallyExpanded = false)
        var hasSheetContent by mutableStateOf(false) // Start out with empty sheet content
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState,
                dragHandle = null,
            ) {
                if (hasSheetContent) {
                    Box(Modifier.fillMaxHeight(0.6f))
                }
            }
        }

        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Hidden)
        assertThat(sheetState.swipeableState.hasAnchorForValue(SheetValue.PartiallyExpanded))
            .isFalse()
        assertThat(sheetState.swipeableState.hasAnchorForValue(SheetValue.Expanded))
            .isFalse()

        scope.launch { sheetState.show() }
        rule.waitForIdle()

        assertThat(sheetState.isVisible).isTrue()
        assertThat(sheetState.currentValue).isEqualTo(sheetState.targetValue)

        hasSheetContent = true // Recompose with sheet content
        rule.waitForIdle()
        assertThat(sheetState.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
    }

    @Test
    fun modalBottomSheet_callsOnDismissRequest_onNestedScrollFling() {
        var callCount by mutableStateOf(0)
        val expectedCallCount = 1
        val sheetState = SheetState(skipPartiallyExpanded = true)

        val nestedScrollDispatcher = NestedScrollDispatcher()
        val nestedScrollConnection = object : NestedScrollConnection {
            // No-Op
        }
        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            ModalBottomSheet(onDismissRequest = { callCount += 1 }, sheetState = sheetState) {
                Column(
                    Modifier
                        .testTag(sheetTag)
                        .nestedScroll(nestedScrollConnection, nestedScrollDispatcher)
                ) {
                    (0..50).forEach {
                        Text(text = "$it")
                    }
                }
            }
        }

        assertThat(sheetState.currentValue).isEqualTo(SheetValue.Expanded)
        val scrollableContentHeight = rule.onNodeWithTag(sheetTag).fetchSemanticsNode().size.height
        // Simulate a drag + fling
        nestedScrollDispatcher.dispatchPostScroll(
            consumed = Offset.Zero,
            available = Offset(x = 0f, y = scrollableContentHeight / 2f),
            source = NestedScrollSource.Drag
        )
        scope.launch {
            nestedScrollDispatcher.dispatchPostFling(
                consumed = Velocity.Zero,
                available = Velocity(x = 0f, y = with(rule.density) { 200.dp.toPx() })
            )
        }

        rule.waitForIdle()
        assertThat(sheetState.isVisible).isFalse()
        assertThat(callCount).isEqualTo(expectedCallCount)
    }
}