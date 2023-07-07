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

package androidx.compose.foundation.demos

import android.content.Context
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect.Companion.dashPathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView

@Preview
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusedBoundsDemo() {
    // This demo demonstrates multiple observers with two separate observers:
    // 1. A pair of eyeballs that look at the focused child.
    FocusedBoundsObserver(
        // 2. A "marching ants" highlight around the focused child.
        Modifier.highlightFocusedBounds()
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = spacedBy(4.dp)
        ) {
            Text(
                "Click in the various text fields below, or the eyeballs above, to see the focus " +
                    "area animate between them."
            )
            Divider()

            FocusableDemoContent()

            // TODO(b/220030968) This won't work until the API can be moved to the UI module.
            Text("Android view (broken: b/220030968):")
            AndroidView(
                ::FocusableAndroidViewDemo,
                Modifier.padding(4.dp).border(2.dp, Color.Green)
            ) {
                it.setContent {
                    Column(Modifier.padding(4.dp).border(2.dp, Color.Blue)) {
                        Text("Compose again")
                        FocusableDemoContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusableDemoContent() {
    Column(verticalArrangement = spacedBy(4.dp)) {
        val focusManager = LocalFocusManager.current
        Button(onClick = { focusManager.clearFocus() }) {
            Text("Clear focus")
        }
        TextField("", {}, Modifier.fillMaxWidth())
        Text("Lazy row:")
        LazyRow(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .border(2.dp, Color.Black),
            horizontalArrangement = spacedBy(8.dp)
        ) {
            items(50) { index ->
                TextField(index.toString(), {}, Modifier.width(64.dp))
            }
        }
    }
}

private class FocusableAndroidViewDemo(context: Context) : LinearLayout(context) {
    private val composeView = ComposeView(context)

    init {
        orientation = VERTICAL
        val fields = LinearLayout(context).apply {
            orientation = HORIZONTAL
            repeat(50) { index ->
                addView(EditText(context).apply {
                    setText(index.toString())
                })
            }
        }
        val fieldRow = HorizontalScrollView(context).apply {
            addView(fields)
        }
        addView(fieldRow)
        addView(composeView)
    }

    fun setContent(content: @Composable () -> Unit) {
        composeView.setContent(content)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FocusedBoundsObserver(modifier: Modifier, content: @Composable () -> Unit) {
    var coordinates: LayoutCoordinates? by remember { mutableStateOf(null) }
    var focusedBounds: LayoutCoordinates? by remember { mutableStateOf(null) }
    var myBounds by remember { mutableStateOf(Rect.Zero) }
    var focalPoint by remember { mutableStateOf(Offset.Unspecified) }

    fun update() {
        if (coordinates == null || !coordinates!!.isAttached) {
            myBounds = Rect.Zero
            focalPoint = Offset.Unspecified
            return
        }
        if (focusedBounds == null) {
            focalPoint = Offset.Unspecified
            return
        }
        val rootCoordinates = generateSequence(coordinates) { it.parentCoordinates }.last()
        myBounds = coordinates!!.boundsInRoot()
        focalPoint = rootCoordinates.localBoundingBoxOf(focusedBounds!!, clipBounds = false).center
    }

    Column(
        modifier
            .onGloballyPositioned {
                coordinates = it
                update()
            }
            .onFocusedBoundsChanged {
                focusedBounds = it
                update()
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Eyeball(focalPoint, myBounds)
            Spacer(Modifier.width(36.dp))
            Eyeball(focalPoint, myBounds)
        }
        Box(propagateMinConstraints = true) {
            content()
        }
    }
}

@Composable
private fun Eyeball(focalPoint: Offset, parentBounds: Rect) {
    var myCenter by remember { mutableStateOf(Offset.Unspecified) }
    var mySize by remember { mutableStateOf(Size.Unspecified) }
    val targetPoint = if (focalPoint.isSpecified && myCenter.isSpecified && mySize.isSpecified) {
        val foo = focalPoint.minus(myCenter)
        val maxDistanceX = maxOf(
            myCenter.x - parentBounds.left,
            parentBounds.width - myCenter.x
        )
        val maxDistanceY = maxOf(
            myCenter.y - parentBounds.top,
            parentBounds.height - myCenter.y
        )
        val maxDistance = maxOf(maxDistanceX, maxDistanceY)
        val scaleFactor = (mySize.minDimension / 2) / maxDistance
        foo.times(scaleFactor)
    } else {
        Offset.Zero
    }
    val animatedTargetPoint by animateOffsetAsState(targetPoint)
    val focusRequester = remember { FocusRequester() }

    Canvas(
        Modifier
            .size(24.dp)
            .onGloballyPositioned {
                myCenter = it.boundsInRoot().center
                mySize = it.size.toSize()
            }
            .clip(CircleShape)
            // Make the eyeballs focusable, just for fun.
            .clickable { focusRequester.requestFocus() }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        drawCircle(Color.White)
        drawCircle(Color.Black, style = Stroke(1.dp.toPx()))

        val pupilCenter = center + animatedTargetPoint
        val pupilRadius = size.minDimension / 4f
        drawCircle(
            Color.Black,
            center = pupilCenter,
            radius = pupilRadius
        )
        drawCircle(
            Color.White,
            center = pupilCenter - (Offset(pupilRadius / 2, pupilRadius / 2)),
            radius = pupilRadius / 3
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.highlightFocusedBounds() = composed {
    var coordinates: LayoutCoordinates? by remember { mutableStateOf(null) }
    var focusedChild: LayoutCoordinates? by remember { mutableStateOf(null) }
    var focusedBounds by remember { mutableStateOf(Rect.Zero) }
    var focusedBoundsClipped by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current

    fun update() {
        with(density) {
            focusedBounds = calculateHighlightBounds(focusedChild, coordinates, clipBounds = false)
                .inflate(1.dp.toPx())
            focusedBoundsClipped =
                calculateHighlightBounds(focusedChild, coordinates, clipBounds = true)
                    .inflate(1.dp.toPx())
        }
    }

    Modifier
        .onGloballyPositioned {
            coordinates = it
            update()
        }
        .onFocusedBoundsChanged {
            focusedChild = it
            update()
        }
        .drawAnimatedFocusHighlight(focusedBoundsClipped, focusedBounds)
}

@OptIn(ExperimentalFoundationApi::class)
private fun calculateHighlightBounds(
    child: LayoutCoordinates?,
    coordinates: LayoutCoordinates?,
    clipBounds: Boolean,
): Rect {
    if (coordinates == null || !coordinates.isAttached) return Rect.Zero
    return child?.let { coordinates.localBoundingBoxOf(it, clipBounds) }
        ?: coordinates.localBoundingBoxOf(coordinates)
}

private fun Modifier.drawAnimatedFocusHighlight(
    primaryBounds: Rect,
    secondaryBounds: Rect
): Modifier = composed {
    val animatedPrimaryBounds by animateRectAsState(primaryBounds)
    val animatedSecondaryBounds by animateRectAsState(secondaryBounds)
    val strokeDashes = remember { floatArrayOf(10f, 10f) }
    val strokeDashPhase by rememberInfiniteTransition()
        .animateFloat(0f, 20f, infiniteRepeatable(tween(500, easing = LinearEasing)))

    drawWithContent {
        drawContent()

        if (animatedSecondaryBounds != Rect.Zero &&
            animatedSecondaryBounds != animatedPrimaryBounds
        ) {
            drawRoundRect(
                color = Color.LightGray,
                alpha = 0.5f,
                topLeft = animatedSecondaryBounds.topLeft,
                size = animatedSecondaryBounds.size,
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(
                    width = 3.dp.toPx(),
                    pathEffect = dashPathEffect(strokeDashes, strokeDashPhase)
                )
            )
        }

        // Draw the primary bounds on top so it's always visible.
        if (animatedPrimaryBounds != Rect.Zero) {
            drawRoundRect(
                color = Color.Blue,
                alpha = 0.5f,
                topLeft = animatedPrimaryBounds.topLeft,
                size = animatedPrimaryBounds.size,
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(
                    width = 3.dp.toPx(),
                    pathEffect = dashPathEffect(strokeDashes, strokeDashPhase)
                )
            )
        }
    }
}