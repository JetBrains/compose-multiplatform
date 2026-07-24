/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.heavyshader

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.isActive

private const val ITEM_COUNT = 800

@Composable
fun HeavyShader() {
    val listState = rememberLazyListState()
    var scrollForward by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis { }
            val currentItem = listState.firstVisibleItemIndex
            if (currentItem == 0) scrollForward = true
            if (currentItem > ITEM_COUNT - 100) scrollForward = false
            listState.scrollBy(if (scrollForward) 33f else -33f)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        items(ITEM_COUNT) {
            HeavyShaderItem()
        }
    }
}

@Composable
private fun HeavyShaderItem() {
    val time by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(8.dp)
    ) {
        val layers = 60

        repeat(layers) { layer ->
            val offset = layer * 10f
            val alpha = 1f - (layer.toFloat() / layers)

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00FF00).copy(alpha = alpha),
                        Color(0xFF0000FF).copy(alpha = alpha * 0.7f),
                        Color(0xFFFF0000).copy(alpha = alpha * 0.5f),
                        Color(0xFFFFFF00).copy(alpha = alpha * 0.3f),
                        Color(0xFF00FFFF).copy(alpha = alpha * 0.1f)
                    ),
                    center = Offset(
                        size.width / 2 + cos(time + layer).toFloat() * 150f,
                        size.height / 2 + sin(time + layer).toFloat() * 150f
                    ),
                    radius = size.minDimension / 2 + offset
                ),
                blendMode = when (layer % 3) {
                    0 -> BlendMode.Screen
                    1 -> BlendMode.Overlay
                    else -> BlendMode.Multiply
                }
            )
        }

        repeat(60) { i ->
            val angle = i * 7.2f + time * 10
            val distance = size.minDimension / 2
            val x = size.width / 2 + cos(angle * PI / 180.0).toFloat() * distance
            val y = size.height / 2 + sin(angle * PI / 180.0).toFloat() * distance

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = 30f
                ),
                radius = 30f,
                center = Offset(x, y),
                blendMode = BlendMode.Plus
            )
        }
    }
}
