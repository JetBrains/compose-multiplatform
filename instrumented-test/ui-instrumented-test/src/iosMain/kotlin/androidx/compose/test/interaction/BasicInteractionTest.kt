/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.test.interaction

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.test.utils.assertVisibleInContainer
import androidx.compose.test.utils.findNodeWithLabel
import androidx.compose.test.utils.findNodeWithTag
import androidx.compose.test.utils.runUIKitInstrumentedTest
import androidx.compose.test.utils.toDpRect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BasicInteractionTest {
    /**
     *  Distance in pixels a touch can wander before we think the user is scrolling.
     *  https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/ui/ui/src/uikitMain/kotlin/androidx/compose/ui/platform/Constants.uikit.kt#L22
     */
    private val CUPERTINO_TOUCH_SLOP = 10.dp

    @Test
    fun testButtonClick() = runUIKitInstrumentedTest {
        var clicks = 0
        setContentWithAccessibilityEnabled {
            Box(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = { clicks++ },
                    modifier = Modifier
                        .testTag("Button")
                        .align(Alignment.Center)
                ) {
                    Text("Click me")
                }
            }
        }

        assertEquals(0, clicks)
        findNodeWithLabel(label = "Click me")
            .tap()
        assertEquals(1, clicks)
        findNodeWithLabel(label = "Click me")
            .tap()
        assertEquals(2, clicks)
        findNodeWithLabel(label = "Click me")
            .tap()
        assertEquals(3, clicks)
    }

    @Test
    fun testScroll() = runUIKitInstrumentedTest {
        val state = ScrollState(0)
        var boxRect = DpRect(DpOffset.Zero, DpSize.Zero)
        setContentWithAccessibilityEnabled {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.Red)
                        .testTag("Hidden after scroll box")
                )
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Green)
                    .testTag("Box")
                    .onGloballyPositioned { boxRect = it.boundsInWindow().toDpRect(density) }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenSize.height)
                        .background(Color.White)
                )
            }
        }

        touchDown(screenSize.center)
            .dragBy(dy = -(100.dp + CUPERTINO_TOUCH_SLOP))

        waitForIdle()

        assertEquals(100 * density.density, state.value.toFloat())
        assertEquals(DpRect(DpOffset.Zero, DpSize(screenSize.width, 100.dp)), boxRect)
    }

    @Test
    fun testDoubleTap() = runUIKitInstrumentedTest {
        var doubleClicked = false
        setContentWithAccessibilityEnabled {
            Column(modifier = Modifier.safeDrawingPadding()) {
                Box(modifier = Modifier.size(100.dp).testTag("Clickable").combinedClickable(
                    onDoubleClick = { doubleClicked = true }
                ) {})
                TextField("Hello Long Text", {}, modifier = Modifier.testTag("TextField"))
            }
        }

        findNodeWithTag("Clickable").doubleTap()

        assertTrue(doubleClicked)
    }

    @Test
    fun testTextFieldCallout() = runUIKitInstrumentedTest {
        setContentWithAccessibilityEnabled {
            Column(modifier = Modifier.safeDrawingPadding()) {
                TextField("Hello-long-long-long-long-long-text", {}, modifier = Modifier.testTag("TextField"))
            }
        }

        findNodeWithTag("TextField").doubleTap()

        waitForIdle()

        // Verify elements from context menu present
        findNodeWithLabel("Cut").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }
        findNodeWithLabel("Copy").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }
        findNodeWithLabel("Paste").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }
        findNodeWithLabel("Select All").let {
            it.assertVisibleInContainer()
            assertTrue(it.isAccessibilityElement ?: false)
        }
    }
}