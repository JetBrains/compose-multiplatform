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

import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Preview
@Composable
fun LoadingAnimationDemo() {
    val isLoading = remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize().background(BackgroundColor).clickable {
        isLoading.value = false
    }) {
        ActualContent()
        LoadingOverlay(isLoading = isLoading)
    }
    // Add a 10-second time out on the loading animation
    LaunchedEffect(Unit) {
        delay(10_000)
        isLoading.value = false
    }
}

@Composable
fun ActualContent() {
    Column(Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
        ) {
            val painter = painterResource(R.drawable.yt_profile)
            Image(
                painter,
                "Profile Picture",
            )
        }
        Text(
            text = "YT (油条)",
            fontFamily = FontFamily(typeface = Typeface.SANS_SERIF),
            fontSize = 40.sp,
            color = Color(0xff173d6e),
            modifier = Modifier.align(Alignment.CenterHorizontally)
                .padding(top = 10.dp, bottom = 5.dp)
        )
        Row {
            Text(
                "Age:",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(80.dp)
            )
            Text("10")
        }
        Row {
            Text(
                "Breed:",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(80.dp)
            )
            Text("Tabby")
        }
        Text(
            "About Me:",
            fontWeight = FontWeight.Bold,
        )
        Text(
            "I have been taking care of the humans in my household since 10 years ago." +
                " They like to stare at various sized glowing boxes for hours on end. I need" +
                " to remind them that life is more than that, by sitting between them and the" +
                " glowing boxes. They often take the hint and break out a toy, sometimes a laser" +
                " pointer, sometimes it's a bird attached to a fishing pole like contraption." +
                " Whatever it is, I pretend that I like chasing them until the humans are tired." +
                " It is my responsibility to make sure they have enough exercise. "
        )
    }
}

val GradientColor: Color = Color(0xff173d6e)
val BackgroundColor: Color = Color(0xffdbe5ef)

@Composable
fun LoadingOverlay(
    isLoading: State<Boolean>
) {
    val fraction = remember { Animatable(0f) }
    var reveal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (isLoading.value) {
            fraction.animateTo(1f, tween(2000))
            fraction.snapTo(0f)
        }
        reveal = true
        fraction.animateTo(1f, tween(1000))
    }

    if (!reveal) { // Draw a cover
        Box(Modifier.fillMaxSize().background(BackgroundColor)) {
            Text(
                "Tap anywhere to finish loading. \n Time out in 10 seconds.",
                Modifier.align(Alignment.Center)
            )
        }
    }

    // Draw gradient..
    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val gradient = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        GradientColor.copy(alpha = 0.2f),
                        BackgroundColor
                    ),
                    startY = size.height * (fraction.value - 0.1f),
                    endY = size.height * (fraction.value + 0.1f)
                )
                onDrawWithContent {
                    drawRect(gradient)
                }
            }
    )
}
