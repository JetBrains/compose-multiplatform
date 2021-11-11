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

package androidx.compose.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.junit4.DesktopScreenshotTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@OptIn(
    ExperimentalTime::class,
    ExperimentalCoroutinesApi::class,
    InternalTestApi::class
)
class ImageComposeSceneTest {
    @get:Rule
    val screenshotRule = DesktopScreenshotTestRule("compose/ui/ui-desktop")

    @Ignore("enable when we make a fork of golden repo")
    @Test
    fun `render static ui`() {
        val density = 2f
        val image = renderComposeScene(
            width = 100,
            height = 200,
            density = Density(density)
        ) {
            Box(Modifier.fillMaxSize().padding(1.dp).background(Color.Red))
        }
        screenshotRule.write(image)
    }

    @Ignore("enable when we make a fork of golden repo")
    @Test
    fun `render animated ui`() {
        ImageComposeScene(
            width = 100,
            height = 200
        ).use { scene ->
            var targetSize by mutableStateOf(0f)

            scene.setContent {
                val size by animateFloatAsState(
                    targetSize,
                    tween(durationMillis = 100_000, easing = LinearEasing)
                )
                Box(Modifier.size(size.dp, size.dp * 2).background(Color.Red))
            }

            targetSize = 100f
            scene.render() // start animation

            screenshotRule.write(scene.render(0.seconds), "frame1")
            screenshotRule.write(scene.render(10.seconds), "frame2")
            screenshotRule.write(scene.render(50.seconds), "frame3")
            screenshotRule.write(scene.render(100.seconds), "frame4")
        }
    }

    @Ignore("See https://github.com/JetBrains/compose-jb/issues/1866")
    @Test
    fun `run multiple ImageComposeScene`() {
        for (i in 1..300) {
            ImageComposeScene(800, 800).use {
                it.setContent {
                    Box(Modifier.fillMaxWidth()) {
                        ExtendedFloatingActionButton(
                            icon = { Icon(Icons.Filled.AccountBox, "") },
                            text = {},
                            onClick = {},
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    @Test(timeout = 5000)
    fun `closing ImageComposeScene should not cancel coroutineContext's Job`() {
        runBlocking(Dispatchers.Swing) {
            val scene = ImageComposeScene(100, 100, coroutineContext = coroutineContext)
            scene.close()
        }
    }
}