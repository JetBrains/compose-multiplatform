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

package androidx.compose.ui.input.pointer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.DummyPlatformComponent
import androidx.compose.ui.platform.LocalPointerIconService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.google.common.truth.Truth.assertThat
import java.awt.Cursor
import org.jetbrains.skia.Surface
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
class PointerIconTest {
    private val iconService = object : PointerIconService {
        override var current: PointerIcon = PointerIconDefaults.Default
    }

    @Test
    fun basicTest() = ImageComposeScene(
        width = 100, height = 100
    ).use { scene ->
        scene.setContent {
            CompositionLocalProvider(
                LocalPointerIconService provides iconService
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIconDefaults.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
        assertThat(iconService.current).isEqualTo(PointerIconDefaults.Text)
    }

    @Test
    fun commitsToComponent() {
        val component = DummyPlatformComponent
        val surface = Surface.makeRasterN32Premul(100, 100)
        val scene = ComposeScene(component = component)

        try {
            scene.constraints = Constraints(maxWidth = surface.width, maxHeight = surface.height)
            scene.setContent {
                Box(
                    modifier = Modifier
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIconDefaults.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            assertThat(component.desiredCursor.type).isEqualTo(Cursor.TEXT_CURSOR)
        } finally {
            scene.close()
        }
    }

    @Test
    fun preservedIfSameEventDispatchedTwice() {
        val component = DummyPlatformComponent
        val surface = Surface.makeRasterN32Premul(100, 100)
        val scene = ComposeScene(component = component)

        try {
            scene.constraints = Constraints(maxWidth = surface.width, maxHeight = surface.height)
            scene.setContent {
                Box(
                    modifier = Modifier
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIconDefaults.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            assertThat(component.desiredCursor.type).isEqualTo(Cursor.TEXT_CURSOR)
        } finally {
            scene.close()
        }
    }

    @Test
    fun parentWins() = ImageComposeScene(
        width = 100, height = 100
    ).use { scene ->
        scene.setContent {
            CompositionLocalProvider(
                LocalPointerIconService provides iconService
            ) {
                Box(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIconDefaults.Hand, true)
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIconDefaults.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
        assertThat(iconService.current).isEqualTo(PointerIconDefaults.Hand)

        scene.sendPointerEvent(PointerEventType.Move, Offset(15f, 15f))
        assertThat(iconService.current).isEqualTo(PointerIconDefaults.Hand)
    }

    @Test
    fun childWins() = ImageComposeScene(
        width = 100, height = 100
    ).use { scene ->
        scene.setContent {
            CompositionLocalProvider(
                LocalPointerIconService provides iconService
            ) {
                Box(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIconDefaults.Hand)
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIconDefaults.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
        assertThat(iconService.current).isEqualTo(PointerIconDefaults.Text)

        scene.sendPointerEvent(PointerEventType.Move, Offset(15f, 15f))
        assertThat(iconService.current).isEqualTo(PointerIconDefaults.Hand)
    }
}