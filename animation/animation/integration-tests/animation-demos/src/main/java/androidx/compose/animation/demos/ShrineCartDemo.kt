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

package androidx.compose.animation.demos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Preview
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ShrineCartDemo() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        var cartState by remember { mutableStateOf(CartState.Collapsed) }
        // Creates a transition here to animate the corner shape and content.
        val cartOpenTransition = updateTransition(cartState, "CartOpenTransition")
        val cornerSize by cartOpenTransition.animateDp(
            label = "cartCornerSize",
            transitionSpec = {
                when {
                    CartState.Expanded isTransitioningTo CartState.Collapsed ->
                        tween(durationMillis = 433, delayMillis = 67)
                    else ->
                        tween(durationMillis = 150)
                }
            }
        ) {
            if (it == CartState.Expanded) 0.dp else 24.dp
        }

        Surface(
            Modifier.shadow(8.dp, CutCornerShape(topStart = cornerSize))
                .clip(CutCornerShape(topStart = cornerSize)),
            color = ShrinePink300,
        ) {
            // Creates an AnimatedContent using the transition. This AnimatedContent will
            // derive its target state from cartOpenTransition.targetState. All the animations
            // created inside of AnimatedContent for size change, enter/exit will be added to the
            // Transition.
            cartOpenTransition.AnimatedContent(
                transitionSpec = {
                    fadeIn(animationSpec = tween(150, delayMillis = 150))
                        .with(fadeOut(animationSpec = tween(150)))
                        .using(
                            SizeTransform { initialSize, targetSize ->
                                if (CartState.Collapsed isTransitioningTo CartState.Expanded) {
                                    keyframes {
                                        durationMillis = 500
                                        IntSize(targetSize.width, initialSize.height + 200) at 150
                                    }
                                } else {
                                    keyframes {
                                        durationMillis = 500
                                        IntSize(
                                            initialSize.width,
                                            (initialSize.height + targetSize.height) / 2
                                        ) at 150
                                    }
                                }
                            }
                        ).apply {
                            targetContentZIndex = when (targetState) {
                                CartState.Collapsed -> 2f
                                CartState.Expanded -> 1f
                            }
                        }
                }
            ) {
                if (it == CartState.Expanded) {
                    ExpandedCart()
                } else {
                    CollapsedCart()
                }
            }
        }
        Box(
            Modifier.clickable {
                cartState =
                    if (cartState == CartState.Expanded) CartState.Collapsed else CartState.Expanded
            }.fillMaxSize()
        )
    }
}

@Composable
fun CollapsedCart() {
    Row(
        Modifier.padding(start = 24.dp, top = 12.dp, bottom = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Shopping cart icon",
            )
        }
        for (i in 0 until 3) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)).background(ShrinePink10)
            )
        }
    }
}

enum class CartState {
    Expanded,
    Collapsed
}

@Composable
fun ExpandedCart() {
    Box(Modifier.fillMaxSize().background(ShrinePink100))
}

private val ShrinePink10 = Color(0xfffffbfa)
private val ShrinePink100 = Color(0xfffedbd0)
private val ShrinePink300 = Color(0xfffff0ea)
