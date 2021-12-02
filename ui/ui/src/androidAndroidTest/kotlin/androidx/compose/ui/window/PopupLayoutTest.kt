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

package androidx.compose.ui.window

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Tests for the internal [PopupLayout] view used by [Popup].
 * When adding new tests, consider writing the tests against the [Popup] composable directly first,
 * since that's the public API, and only adding tests here if the tests need to interact in ways
 * that aren't easily supported by the compose test APIs.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class PopupLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun canCalculatePosition_onlyWhenSizeAndCoordinatesAreAvailable() {
        val layout = createPopupLayout()
        assertThat(layout.canCalculatePosition).isFalse()

        // Only size available.
        layout.popupContentSize = IntSize.Zero
        assertThat(layout.canCalculatePosition).isFalse()

        // Only coordinates available.
        layout.popupContentSize = null
        layout.updateParentLayoutCoordinates(NoopLayoutCoordinates)
        assertThat(layout.canCalculatePosition).isFalse()

        // Everything available.
        layout.popupContentSize = IntSize.Zero
        assertThat(layout.canCalculatePosition).isTrue()
    }

    @Test
    fun positionUpdated_whenCoordinatesUpdated() {
        val coordinates = MutableLayoutCoordinates()
        val layout = createPopupLayout(
            positionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset = anchorBounds.topLeft
            },
        )
        layout.popupContentSize = IntSize.Zero

        assertThat(layout.params.x).isEqualTo(0)
        assertThat(layout.params.y).isEqualTo(0)

        coordinates.windowOffset = Offset(50f, 50f)
        layout.updateParentLayoutCoordinates(coordinates)

        assertThat(layout.params.x).isEqualTo(50)
        assertThat(layout.params.y).isEqualTo(50)
    }

    @Test
    fun positionNotUpdated_whenCoordinatesUpdated_withSameParentBounds() {
        var paramUpdateCount = 0
        val layout = createPopupLayout(
            positionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset = anchorBounds.topLeft
            },
            popupLayoutHelper = object : NoopPopupLayoutHelper() {
                override fun updateViewLayout(
                    windowManager: WindowManager,
                    popupView: View,
                    params: ViewGroup.LayoutParams
                ) {
                    paramUpdateCount++
                }
            }
        )

        // Set size before coordinates to match the order that the compose runtime uses.
        layout.popupContentSize = IntSize.Zero
        layout.updateParentLayoutCoordinates(MutableLayoutCoordinates())

        assertThat(paramUpdateCount).isEqualTo(1)

        // Different coordinates object but with the same values, so shouldn't trigger a position
        // update.
        layout.updateParentLayoutCoordinates(MutableLayoutCoordinates())

        assertThat(paramUpdateCount).isEqualTo(1)
    }

    @Test
    fun positionNotUpdated_onParentBoundsUpdateRequested_withSameParentBounds() {
        var paramUpdateCount = 0
        val layout = createPopupLayout(
            positionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset = anchorBounds.topLeft
            },
            popupLayoutHelper = object : NoopPopupLayoutHelper() {
                override fun updateViewLayout(
                    windowManager: WindowManager,
                    popupView: View,
                    params: ViewGroup.LayoutParams
                ) {
                    paramUpdateCount++
                }
            }
        )

        // Set size before coordinates to match the order that the compose runtime uses.
        layout.popupContentSize = IntSize.Zero
        layout.updateParentLayoutCoordinates(MutableLayoutCoordinates())

        assertThat(paramUpdateCount).isEqualTo(1)

        layout.updateParentBounds()

        assertThat(paramUpdateCount).isEqualTo(1)
    }

    @Test
    fun positionUpdated_onParentBoundsUpdateRequested_withDifferentParentBounds() {
        var paramUpdateCount = 0
        val coordinates = MutableLayoutCoordinates()
        val layout = createPopupLayout(
            positionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset = anchorBounds.topLeft
            },
            popupLayoutHelper = object : NoopPopupLayoutHelper() {
                override fun updateViewLayout(
                    windowManager: WindowManager,
                    popupView: View,
                    params: ViewGroup.LayoutParams
                ) {
                    paramUpdateCount++
                }
            }
        )

        // Set size before coordinates to match the order that the compose runtime uses.
        layout.popupContentSize = IntSize.Zero
        layout.updateParentLayoutCoordinates(coordinates)

        assertThat(layout.params.x).isEqualTo(0)
        assertThat(layout.params.y).isEqualTo(0)

        coordinates.windowOffset = Offset(50f, 50f)
        layout.updateParentBounds()

        assertThat(layout.params.x).isEqualTo(50)
        assertThat(layout.params.y).isEqualTo(50)
    }

    private fun createPopupLayout(
        onDismissRequest: (() -> Unit)? = null,
        properties: PopupProperties = PopupProperties(),
        density: Density = rule.density,
        positionProvider: PopupPositionProvider = ZeroPositionProvider,
        popupLayoutHelper: PopupLayoutHelper = NoopPopupLayoutHelper()
    ): PopupLayout {
        lateinit var layout: PopupLayout
        rule.setContent {
            val view = LocalView.current
            remember {
                PopupLayout(
                    onDismissRequest = onDismissRequest,
                    properties = properties,
                    testTag = "test popup",
                    composeView = view,
                    density = density,
                    initialPositionProvider = positionProvider,
                    popupId = UUID.randomUUID(),
                    popupLayoutHelper = popupLayoutHelper
                ).also { layout = it }
            }
        }
        return rule.runOnIdle { layout }
    }

    private companion object {
        val ZeroPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset.Zero
        }

        val NoopLayoutCoordinates: LayoutCoordinates = MutableLayoutCoordinates()

        /**
         * An implementation of [LayoutCoordinates] that allows explicitly setting values but only
         * supports the minimum required subset of operations that [PopupLayout] uses.
         */
        private class MutableLayoutCoordinates : LayoutCoordinates {
            override var size: IntSize = IntSize.Zero
            override val providedAlignmentLines: Set<AlignmentLine> = emptySet()
            override var parentLayoutCoordinates: LayoutCoordinates? = null
            override var parentCoordinates: LayoutCoordinates? = null
            override var isAttached: Boolean = false

            var windowOffset: Offset = Offset.Zero

            override fun windowToLocal(relativeToWindow: Offset): Offset =
                relativeToWindow - windowOffset

            override fun localToWindow(relativeToLocal: Offset): Offset =
                windowOffset + relativeToLocal

            override fun localToRoot(relativeToLocal: Offset): Offset =
                throw UnsupportedOperationException()

            override fun localPositionOf(
                sourceCoordinates: LayoutCoordinates,
                relativeToSource: Offset
            ): Offset = throw UnsupportedOperationException()

            override fun localBoundingBoxOf(
                sourceCoordinates: LayoutCoordinates,
                clipBounds: Boolean
            ): Rect = throw UnsupportedOperationException()

            override fun get(alignmentLine: AlignmentLine): Int =
                throw UnsupportedOperationException()
        }

        private open class NoopPopupLayoutHelper : PopupLayoutHelper {
            override fun getWindowVisibleDisplayFrame(
                composeView: View,
                outRect: android.graphics.Rect
            ) {
                // do nothing
            }

            override fun setGestureExclusionRects(composeView: View, width: Int, height: Int) {
                // do nothing
            }

            override fun updateViewLayout(
                windowManager: WindowManager,
                popupView: View,
                params: ViewGroup.LayoutParams
            ) {
                // do nothing
            }
        }
    }
}
