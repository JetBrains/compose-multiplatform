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

package androidx.compose.foundation.demos.text

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextAnimationDemo() {
    LazyColumn {
        item {
            TagLine(tag = "Scale Animation")
            TextScaleAnimation()
        }
        item {
            TagLine(tag = "Translation Animation")
            TextTranslationAnimation()
        }
        item {
            TagLine(tag = "Rotation Animation")
            TextRotationAnimation()
        }
    }
}

class TextMotionState(initialTextStyle: TextStyle) {
    var isStatic by mutableStateOf(true)

    @OptIn(ExperimentalTextApi::class)
    val textStyle by derivedStateOf {
        if (isStatic) {
            initialTextStyle.copy(textMotion = TextMotion.Static)
        } else {
            initialTextStyle.copy(textMotion = TextMotion.Animated)
        }
    }

    @Composable
    fun TextMotionPanel() {
        Row(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .weight(1f)
                    .clickable { isStatic = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = isStatic, onClick = { isStatic = true })
                Text(text = "Static")
            }

            Row(
                Modifier
                    .weight(1f)
                    .clickable { isStatic = false },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = !isStatic, onClick = { isStatic = false })
                Text(text = "Animated")
            }
        }
    }
}

@Composable
fun rememberTextMotionState(): TextMotionState {
    val textStyle = LocalTextStyle.current
    return remember(textStyle) { TextMotionState(textStyle) }
}

@Composable
fun TextScaleAnimation() {
    val textMotionState = rememberTextMotionState()

    textMotionState.TextMotionPanel()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(tween(3500), RepeatMode.Reverse)
        )
        Text(
            text = "Lorem Ipsum\ndolor sit amet",
            fontSize = 24.sp,
            style = textMotionState.textStyle,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

@Composable
fun TextTranslationAnimation() {
    val textMotionState = rememberTextMotionState()

    textMotionState.TextMotionPanel()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val translation by infiniteTransition.animateFloat(
            initialValue = -100f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(tween(3500), RepeatMode.Reverse)
        )
        Text(
            text = "Lorem Ipsum\ndolor sit amet",
            fontSize = 24.sp,
            style = textMotionState.textStyle,
            modifier = Modifier.graphicsLayer {
                translationX = translation
                translationY = translation
            }
        )
    }
}

@Composable
fun TextRotationAnimation() {
    val textMotionState = rememberTextMotionState()

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3500), RepeatMode.Reverse)
    )
    textMotionState.TextMotionPanel()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Lorem Ipsum\ndolor sit amet",
            fontSize = 24.sp,
            style = textMotionState.textStyle,
            modifier = Modifier.graphicsLayer { rotationX = rotation }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Lorem Ipsum\ndolor sit amet",
            fontSize = 24.sp,
            style = textMotionState.textStyle,
            modifier = Modifier.graphicsLayer { rotationY = rotation }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "Lorem Ipsum\ndolor sit amet",
            fontSize = 24.sp,
            style = textMotionState.textStyle,
            modifier = Modifier.graphicsLayer { rotationZ = rotation }
        )
    }
}
