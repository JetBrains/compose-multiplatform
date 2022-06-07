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
@file:OptIn(ExperimentalAnimationApi::class)

package androidx.compose.animation.demos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope.SlideDirection
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScreenTransitionDemo() {
    Column {
        Spacer(Modifier.size(40.dp))
        var targetScreen by remember { mutableStateOf(TestScreens.Screen1) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = {
                    targetScreen = when ((targetScreen.ordinal + 2) % 3) {
                        1 -> TestScreens.Screen2
                        2 -> TestScreens.Screen3
                        else -> TestScreens.Screen1
                    }
                },
                modifier = Modifier.align(Alignment.CenterVertically).padding(10.dp)
            ) {
                Text("Previous screen")
            }
            Button(
                onClick = {
                    targetScreen = when (targetScreen.ordinal + 1) {
                        1 -> TestScreens.Screen2
                        2 -> TestScreens.Screen3
                        else -> TestScreens.Screen1
                    }
                },
                modifier = Modifier.align(Alignment.CenterVertically).padding(10.dp)
            ) {
                Text("Next screen")
            }
        }
        val transition = updateTransition(targetScreen)
        transition.AnimatedContent(
            transitionSpec = {
                if (TestScreens.Screen1 isTransitioningTo TestScreens.Screen2 ||
                    TestScreens.Screen2 isTransitioningTo TestScreens.Screen1
                ) {
                    (expandHorizontally(animationSpec = tween(500)) + fadeIn()).with(
                        shrinkVertically(animationSpec = tween(500)) +
                            fadeOut(animationSpec = tween(500))
                    )
                } else if (TestScreens.Screen2 isTransitioningTo TestScreens.Screen3) {
                    slideIntoContainer(towards = SlideDirection.Left) with
                        slideOutOfContainer(towards = SlideDirection.Left)
                } else if (TestScreens.Screen3 isTransitioningTo TestScreens.Screen2) {
                    slideIntoContainer(towards = SlideDirection.Right) with
                        slideOutOfContainer(towards = SlideDirection.Right)
                } else {
                    // Material fade through
                    fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(
                            initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)
                        ) with fadeOut(animationSpec = tween(90))
                }
            }
        ) {
            when (it) {
                TestScreens.Screen1 -> Screen1()
                TestScreens.Screen2 -> Screen2()
                TestScreens.Screen3 -> Screen3()
            }
        }
    }
}

enum class TestScreens {
    Screen1,
    Screen2,
    Screen3
}

@Composable
fun Screen1() {
    Box(modifier = Modifier.fillMaxSize().padding(30.dp).background(Color(0xffff6f69))) {
        Text("Screen 1", modifier = Modifier.align(Center))
    }
}

@Composable
fun Screen2() {
    Box(modifier = Modifier.fillMaxSize().padding(30.dp).background(Color(0xffffcc5c))) {
        Text("Screen 2", modifier = Modifier.align(Center))
    }
}

@Composable
fun Screen3() {
    Box(modifier = Modifier.fillMaxSize().padding(30.dp).background(Color(0xff2a9d84))) {
        Text("Screen 3", modifier = Modifier.align(Center))
    }
}
