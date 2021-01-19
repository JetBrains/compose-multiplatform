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

package androidx.compose.animation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun HorizontalTransitionSample() {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(
            // Set the start width to 20 (pixels), 0 by default
            initialWidth = { 20 }
        ),
        exit = shrinkHorizontally(
            // Shrink towards the end (i.e. right edge for LTR, left edge for RTL). The default
            // direction for the shrink is towards [Alignment.Start]
            shrinkTowards = Alignment.End,
            // Set the end width for the shrink animation to a quarter of the full width.
            targetWidth = { fullWidth -> fullWidth / 4 },
            // Overwrites the default animation with tween for this shrink animation.
            animSpec = tween()
        )
    ) {
        // Content that needs to appear/disappear goes here:
        Box(Modifier.fillMaxWidth().height(200.dp))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun SlideTransition() {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            // Offsets the content by 1/3 of its width to the left, and slide towards right
            initialOffsetX = { fullWidth -> -fullWidth / 3 },
            // Overwrites the default animation with tween for this slide animation.
            animSpec = tween(durationMillis = 200)
        ) + fadeIn(
            // Overwrites the default animation with tween
            animSpec = tween(durationMillis = 200)
        ),
        exit = slideOutHorizontally(
            // Overwrites the ending position of the slide-out to 200 (pixels) to the right
            targetOffsetX = { 200 },
            animSpec = spring(stiffness = Spring.StiffnessHigh)
        ) + fadeOut()
    ) {
        // Content that needs to appear/disappear goes here:
        Box(Modifier.fillMaxWidth().height(200.dp)) {}
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun FadeTransition() {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            // Overwrites the initial value of alpha to 0.4f for fade in, 0 by default
            initialAlpha = 0.4f
        ),
        exit = fadeOut(
            // Overwrites the default animation with tween
            animSpec = tween(durationMillis = 250)
        )
    ) {
        // Content that needs to appear/disappear goes here:
        Text("Content to appear/disappear", Modifier.fillMaxWidth().height(200.dp))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun FullyLoadedTransition() {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            // Start the slide from 40 (pixels) above where the content is supposed to go, to
            // produce a parallax effect
            initialOffsetY = { -40 }
        ) + expandVertically(
            expandFrom = Alignment.Top
        ) + fadeIn(initialAlpha = 0.3f),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        // Content that needs to appear/disappear goes here:
        Text("Content to appear/disappear", Modifier.fillMaxWidth().height(200.dp))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun AnimatedFloatingActionButton() {
    var expanded by remember { mutableStateOf(true) }
    FloatingActionButton(
        onClick = { expanded = !expanded },
        modifier = with(ColumnScope) { Modifier.align(Alignment.CenterHorizontally) }
    ) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp)) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Favorite",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            AnimatedVisibility(
                expanded,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(modifier = Modifier.padding(start = 12.dp), text = "Favorite")
            }
        }
    }
    Spacer(Modifier.height(20.dp))
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun SlideInOutSample() {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible,
        enter = slideIn(
            // Specifies the starting offset of the slide-in to be 1/4 of the width to the right,
            // 100 (pixels) below the content position, which results in a simultaneous slide up
            // and slide left.
            { fullSize -> IntOffset(fullSize.width / 4, 100) },
            tween(100, easing = LinearOutSlowInEasing)
        ),
        exit = slideOut(
            // The offset can be entirely independent of the size of the content. This specifies
            // a target offset 180 pixels to the left of the content, and 50 pixels below. This will
            // produce a slide-left combined with a slide-down.
            { IntOffset(-180, 50) },
            tween(100, easing = FastOutSlowInEasing)
        )
    ) {
        // Content that needs to appear/disappear goes here:
        Text("Content to appear/disappear", Modifier.fillMaxWidth().height(200.dp))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun ExpandShrinkVerticallySample() {
    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible,
        // Sets the initial height of the content to 20, revealing only the top of the content at
        // the beginning of the expanding animation.
        enter = expandVertically(
            expandFrom = Alignment.Top,
            initialHeight = { 20 }
        ),
        // Shrinks the content to half of its full height via an animation.
        exit = shrinkVertically(
            targetHeight = { fullHeight -> fullHeight / 2 },
            animSpec = tween()
        )
    ) {
        // Content that needs to appear/disappear goes here:
        Text("Content to appear/disappear", Modifier.fillMaxWidth().height(200.dp))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun ExpandInShrinkOutSample() {
    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible,
        enter = expandIn(
            // Overwrites the corner of the content that is first revealed
            expandFrom = Alignment.BottomStart,
            // Overwrites the initial size to 50 pixels by 50 pixels
            initialSize = { IntSize(50, 50) },
            // Overwrites the default spring animation with tween
            animSpec = tween(100, easing = LinearOutSlowInEasing)
        ),
        exit = shrinkOut(
            // Overwrites the area of the content that the shrink animation will end on. The
            // following parameters will shrink the content's clip bounds from the full size of the
            // content to 1/10 of the width and 1/5 of the height. The shrinking clip bounds will
            // always be aligned to the CenterStart of the full-content bounds.
            shrinkTowards = Alignment.CenterStart,
            // Overwrites the target size of the shrinking animation.
            targetSize = { fullSize -> IntSize(fullSize.width / 10, fullSize.height / 5) },
            animSpec = tween(100, easing = FastOutSlowInEasing)
        )
    ) {
        // Content that needs to appear/disappear goes here:
        Text("Content to appear/disappear", Modifier.fillMaxWidth().height(200.dp))
    }
}

@Sampled
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ColumnAnimatedVisibilitySample() {
    var itemIndex by remember { mutableStateOf(0) }
    val colors = listOf(Color.Red, Color.Green, Color.Blue)
    Column(
        Modifier.fillMaxWidth().clickable {
            itemIndex = (itemIndex + 1) % colors.size
        }
    ) {
        colors.forEachIndexed { i, color ->
            // By default ColumnScope.AnimatedVisibility expands and shrinks new content while
            // fading.
            AnimatedVisibility(i <= itemIndex) {
                Box(Modifier.height(40.dp).fillMaxWidth().background(color))
            }
        }
    }
}
