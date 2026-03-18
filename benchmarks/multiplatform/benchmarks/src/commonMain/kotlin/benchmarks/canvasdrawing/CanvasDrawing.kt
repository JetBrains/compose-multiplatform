/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.canvasdrawing

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.isActive

private const val ITEM_COUNT = 1200

@Composable
fun CanvasDrawing() {
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
        items(ITEM_COUNT) { index ->
            CanvasDrawingItem(index)
        }
    }
}

@Composable
private fun CanvasDrawingItem(index: Int) {
    val animatedValue by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(8.dp)
    ) {
        val seed = index * 12345L
        val random = Random(seed)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(random.nextInt(0xFFFFFF) or 0xFF000000.toInt()),
                    Color(random.nextInt(0xFFFFFF) or 0xFF000000.toInt())
                )
            )
        )

        repeat(200) { i ->
            val angle = animatedValue + i * 7.2f
            val radius = size.minDimension / 4f
            val x = size.width / 2 + cos(angle * PI / 180.0).toFloat() * radius * random.nextFloat()
            val y = size.height / 2 + sin(angle * PI / 180.0).toFloat() * radius * random.nextFloat()

            val path = Path().apply {
                moveTo(x, y)
                repeat(40) { j ->
                    val pointAngle = angle + j * 45f + animatedValue
                    val pointRadius = 20f + random.nextFloat() * 30f
                    val px = x + cos(pointAngle * PI / 180.0).toFloat() * pointRadius
                    val py = y + sin(pointAngle * PI / 180.0).toFloat() * pointRadius
                    if (j % 2 == 0) {
                        lineTo(px, py)
                    } else {
                        quadraticBezierTo(
                            x + random.nextFloat() * 50f - 25f,
                            y + random.nextFloat() * 50f - 25f,
                            px, py
                        )
                    }
                }
                close()
            }

            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(random.nextInt(0xFFFFFF) or 0x80000000.toInt()),
                        Color(random.nextInt(0xFFFFFF) or 0x40000000.toInt())
                    ),
                    center = Offset(x, y)
                )
            )
        }

        repeat(70) { i ->
            val lineY = i * (size.height / 30)
            drawLine(
                color = Color(random.nextInt(0xFFFFFF) or 0xFF000000.toInt()),
                start = Offset(0f, lineY),
                end = Offset(size.width, lineY + sin(animatedValue * i).toFloat() * 20f),
                strokeWidth = 2f
            )
        }

        repeat(70) { i ->
            val circleX = (i % 8) * (size.width / 8) + (size.width / 16)
            val circleY = (i / 8) * (size.height / 5) + (size.height / 10)
            val circleRadius = 15f + sin(animatedValue + i).toFloat() * 10f

            drawCircle(
                color = Color(random.nextInt(0xFFFFFF) or 0xFF000000.toInt()),
                radius = circleRadius,
                center = Offset(circleX, circleY),
                alpha = 0.7f
            )
        }
    }
}
