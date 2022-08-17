/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.relocation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(Parameterized::class)
class BringIntoViewScrollableInteractionTest(private val orientation: Orientation) {

    @get:Rule
    val rule = createComposeRule()

    private val parentBox = "parent box"
    private val childBox = "child box"

    /**
     * Captures a scope from inside the composition for [runBlockingAndAwaitIdle].
     * Make sure to call [setContentAndInitialize] instead of calling `rule.setContent` to initialize this.
     */
    private lateinit var testScope: CoroutineScope

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Orientation> = arrayOf(Horizontal, Vertical)
    }

    @Test
    fun noScrollableParent_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.size(100.toDp(), 50.toDp())
                            Vertical -> Modifier.size(50.toDp(), 100.toDp())
                        }
                    )
                    .testTag(parentBox)
                    .background(LightGray)
            ) {
                Box(
                    Modifier
                        .size(50.toDp())
                        .background(Blue)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                )
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun noScrollableParent_itemNotVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.size(100.toDp(), 50.toDp())
                            Vertical -> Modifier.size(50.toDp(), 100.toDp())
                        }
                    )
                    .testTag(parentBox)
                    .background(LightGray)
            ) {
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.offset(x = 150.toDp())
                                Vertical -> Modifier.offset(y = 150.toDp())
                            }
                        )
                        .size(50.toDp())
                        .background(Blue)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                )
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemAtLeadingEdge_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(rememberScrollState())
                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                Box(
                    Modifier
                        .size(50.toDp())
                        .background(Blue)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                )
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemAtTrailingEdge_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(rememberScrollState())
                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.offset(x = 50.toDp())
                                Vertical -> Modifier.offset(y = 50.toDp())
                            }
                        )
                        .size(50.toDp())
                        .background(Blue)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                )
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemAtCenter_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(rememberScrollState())
                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.offset(x = 25.toDp())
                                Vertical -> Modifier.offset(y = 25.toDp())
                            }
                        )
                        .size(50.toDp())
                        .background(Blue)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                )
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemBiggerThanParentAtLeadingEdge_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .size(50.toDp())
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScroll(rememberScrollState())
                            Vertical -> Modifier.verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                // Using a multi-colored item to make sure we can assert that the right part of
                // the item is visible.
                RowOrColumn(
                    Modifier
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                ) {
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Blue)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Green)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Red)
                    )
                }
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemBiggerThanParentAtTrailingEdge_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .size(50.toDp())
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScroll(scrollState)
                            Vertical -> Modifier.verticalScroll(scrollState)
                        }
                    )
            ) {
                // Using a multi-colored item to make sure we can assert that the right part of
                // the item is visible.
                RowOrColumn(
                    Modifier
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                ) {
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Red)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Green)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Blue)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemBiggerThanParentAtCenter_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .size(50.toDp())
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScroll(scrollState)
                            Vertical -> Modifier.verticalScroll(scrollState)
                        }
                    )
            ) {
                // Using a multi-colored item to make sure we can assert that the right part of
                // the item is visible.
                RowOrColumn(
                    Modifier
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                ) {
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Green)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Blue)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Red)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue / 2) }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun childBeforeVisibleBounds_parentIsScrolledSoThatLeadingEdgeOfChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(scrollState)
                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(scrollState)
                        }
                    )
            ) {
                Box(
                    when (orientation) {
                        Horizontal -> Modifier.size(200.toDp(), 50.toDp())
                        Vertical -> Modifier.size(50.toDp(), 200.toDp())
                    }
                ) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 50.toDp())
                                    Vertical -> Modifier.offset(y = 50.toDp())
                                }
                            )
                            .size(50.toDp())
                            .background(Blue)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .testTag(childBox)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(0.toDp(), 0.toDp())
        assertChildMaxInView()
    }

    @Test
    fun childAfterVisibleBounds_parentIsScrolledSoThatTrailingEdgeOfChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(scrollState)
                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(scrollState)
                        }
                    )
            ) {
                Box(
                    when (orientation) {
                        Horizontal -> Modifier.size(200.toDp(), 50.toDp())
                        Vertical -> Modifier.size(50.toDp(), 200.toDp())
                    }
                ) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 150.toDp())
                                    Vertical -> Modifier.offset(y = 150.toDp())
                                }
                            )
                            .size(50.toDp())
                            .background(Blue)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .testTag(childBox)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(
            expectedLeft = if (orientation == Horizontal) 50.toDp() else 0.toDp(),
            expectedTop = if (orientation == Horizontal) 0.toDp() else 50.toDp()
        )
        assertChildMaxInView()
    }

    @Test
    fun childPartiallyVisible_parentIsScrolledSoThatLeadingEdgeOfChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(scrollState)
                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(scrollState)
                        }
                    )
            ) {
                Box(Modifier.size(200.toDp())) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 25.toDp())
                                    Vertical -> Modifier.offset(y = 25.toDp())
                                }
                            )
                            .size(50.toDp())
                            .background(Blue)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .testTag(childBox)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue / 2) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(0.toDp(), 0.toDp())
        assertChildMaxInView()
    }

    @Test
    fun childPartiallyVisible_parentIsScrolledSoThatTrailingEdgeOfChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(scrollState)
                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(scrollState)
                        }
                    )
            ) {
                Box(
                    when (orientation) {
                        Horizontal -> Modifier.size(200.toDp(), 50.toDp())
                        Vertical -> Modifier.size(50.toDp(), 200.toDp())
                    }
                ) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 150.toDp())
                                    Vertical -> Modifier.offset(y = 150.toDp())
                                }
                            )
                            .size(50.toDp())
                            .background(Blue)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .testTag(childBox)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(
            expectedLeft = if (orientation == Horizontal) 50.toDp() else 0.toDp(),
            expectedTop = if (orientation == Horizontal) 0.toDp() else 50.toDp()
        )
        assertChildMaxInView()
    }

    @Test
    fun multipleParentsAreScrolledSoThatChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var parentScrollState: ScrollState
        lateinit var grandParentScrollState: ScrollState
        setContentAndInitialize {
            parentScrollState = rememberScrollState()
            grandParentScrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(grandParentScrollState)
                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(grandParentScrollState)
                        }
                    )
            ) {
                Box(
                    Modifier
                        .background(LightGray)
                        .then(
                            when (orientation) {
                                Horizontal ->
                                    Modifier
                                        .size(200.toDp(), 50.toDp())
                                        .horizontalScroll(parentScrollState)
                                Vertical ->
                                    Modifier
                                        .size(50.toDp(), 200.toDp())
                                        .verticalScroll(parentScrollState)
                            }
                        )
                ) {
                    Box(
                        when (orientation) {
                            Horizontal -> Modifier.size(400.toDp(), 50.toDp())
                            Vertical -> Modifier.size(50.toDp(), 400.toDp())
                        }
                    ) {
                        Box(
                            Modifier
                                .then(
                                    when (orientation) {
                                        Horizontal -> Modifier.offset(x = 25.toDp())
                                        Vertical -> Modifier.offset(y = 25.toDp())
                                    }
                                )
                                .size(50.toDp())
                                .background(Blue)
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .testTag(childBox)
                        )
                    }
                }
            }
        }
        runBlockingAndAwaitIdle { parentScrollState.scrollTo(parentScrollState.maxValue) }
        runBlockingAndAwaitIdle { grandParentScrollState.scrollTo(grandParentScrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(0.toDp(), 0.toDp())
        assertChildMaxInView()
    }

    @Test
    fun multipleParentsAreScrolledInDifferentDirectionsSoThatChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var parentScrollState: ScrollState
        lateinit var grandParentScrollState: ScrollState
        setContentAndInitialize {
            parentScrollState = rememberScrollState()
            grandParentScrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .verticalScroll(grandParentScrollState)
                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .horizontalScroll(grandParentScrollState)
                        }
                    )
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(LightGray)
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.horizontalScroll(parentScrollState)
                                Vertical -> Modifier.verticalScroll(parentScrollState)
                            }
                        )
                ) {
                    Box(Modifier.size(200.toDp())) {
                        Box(
                            Modifier
                                .offset(x = 25.toDp(), y = 25.toDp())
                                .size(50.toDp())
                                .background(Blue)
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .testTag(childBox)
                        )
                    }
                }
            }
        }
        runBlockingAndAwaitIdle { parentScrollState.scrollTo(parentScrollState.maxValue) }
        runBlockingAndAwaitIdle { grandParentScrollState.scrollTo(grandParentScrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(0.toDp(), 0.toDp())
        assertChildMaxInView()
    }

    @Test
    fun specifiedPartOfComponentBroughtOnScreen() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var density: Density
        setContentAndInitialize {
            density = LocalDensity.current
            Box(
                Modifier
                    .testTag(parentBox)
                    .size(50.toDp())
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScroll(rememberScrollState())
                            Vertical -> Modifier.verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.size(150.toDp(), 50.toDp())
                                Vertical -> Modifier.size(50.toDp(), 150.toDp())
                            }
                        )
                        .bringIntoViewRequester(bringIntoViewRequester)
                ) {
                    Box(
                        Modifier
                            .size(50.toDp())
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(50.toDp(), 0.toDp())
                                    Vertical -> Modifier.offset(0.toDp(), 50.toDp())
                                }
                            )
                            .background(Blue)
                            .testTag(childBox)
                    )
                }
            }
        }

        // Act.
        runBlockingAndAwaitIdle {
            val rect = with(density) {
                when (orientation) {
                    Horizontal -> DpRect(50.toDp(), 0.toDp(), 100.toDp(), 50.toDp()).toRect()
                    Vertical -> DpRect(0.toDp(), 50.toDp(), 50.toDp(), 100.toDp()).toRect()
                }
            }
            bringIntoViewRequester.bringIntoView(rect)
        }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(0.toDp(), 0.toDp())
        assertChildMaxInView()
    }

    private fun setContentAndInitialize(content: @Composable () -> Unit) {
        rule.setContent {
            testScope = rememberCoroutineScope()
            content()
        }
    }

    /**
     * Sizes and offsets of the composables in these tests must be specified using this function.
     * If they're specified using `xx.dp` syntax, a rounding error somewhere in the layout system
     * will cause the pixel values to be off-by-one.
     */
    private fun Int.toDp(): Dp = with(rule.density) { this@toDp.toDp() }

    /**
     * Returns the bounds of the node with [tag], without performing any clipping by any parents.
     */
    @Suppress("SameParameterValue")
    private fun getUnclippedBoundsInRoot(tag: String): Rect {
        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        return Rect(node.positionInRoot, node.size.toSize())
    }

    @Composable
    private fun RowOrColumn(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        when (orientation) {
            Horizontal -> Row(modifier) { content() }
            Vertical -> Column(modifier) { content() }
        }
    }

    private fun runBlockingAndAwaitIdle(block: suspend CoroutineScope.() -> Unit) {
        val job = testScope.launch(block = block)
        rule.waitForIdle()
        runBlocking {
            job.join()
        }
    }

    /**
     * Asserts that as much of the child (identified by [childBox]) as can fit in the viewport
     * (identified by [parentBox]) is visible. This is the min of the child size and the viewport
     * size.
     */
    private fun assertChildMaxInView() {
        val parentNode = rule.onNodeWithTag(parentBox).fetchSemanticsNode()
        val childNode = rule.onNodeWithTag(childBox).fetchSemanticsNode()

        // BoundsInRoot returns the clipped bounds.
        val visibleBounds: IntSize = childNode.boundsInRoot.size.run {
            IntSize(width.roundToInt(), height.roundToInt())
        }
        val expectedVisibleBounds = IntSize(
            width = minOf(parentNode.size.width, childNode.size.width),
            height = minOf(parentNode.size.height, childNode.size.height)
        )

        assertThat(visibleBounds).isEqualTo(expectedVisibleBounds)
    }
}