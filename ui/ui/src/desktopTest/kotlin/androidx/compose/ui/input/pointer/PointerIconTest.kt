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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalPointerIconService
import androidx.compose.ui.platform.Platform
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
        val component = IconPlatform()
        val surface = Surface.makeRasterN32Premul(100, 100)
        val scene = ComposeScene(platform = component)

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
            assertThat(component._pointerIcon).isEqualTo(PointerIconDefaults.Text)
        } finally {
            scene.close()
        }
    }

    @Test
    fun preservedIfSameEventDispatchedTwice() {
        val component = IconPlatform()
        val surface = Surface.makeRasterN32Premul(100, 100)
        val scene = ComposeScene(platform = component)

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
            assertThat(component._pointerIcon).isEqualTo(PointerIconDefaults.Text)
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

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun whenHoveredShouldCommitWithoutMoveWhenIconChanges() = runTest(UnconfinedTestDispatcher()) {
        val component = IconPlatform()
        val surface = Surface.makeRasterN32Premul(100, 100)
        var scene: ComposeScene? = null

        scene = ComposeScene(platform = component, invalidate = {
            scene?.render(surface.canvas, 1)
        }, coroutineContext = coroutineContext)

        val iconState = mutableStateOf(PointerIconDefaults.Text)

        val recomposeChannel = Channel<Int>(Channel.CONFLATED) // helps with waiting for recomposition
        var count = 0
        try {
            scene.constraints = Constraints(maxWidth = surface.width, maxHeight = surface.height)
            scene.setContent {
                Box(
                    modifier = Modifier.pointerHoverIcon(iconState.value).size(30.dp, 30.dp)
                )
                recomposeChannel.trySend(++count)
            }
            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            assertThat(recomposeChannel.receive()).isEqualTo(1)
            assertThat(component._pointerIcon).isEqualTo(PointerIconDefaults.Text)

            // No move, but change should be applied anyway
            iconState.value = PointerIconDefaults.Crosshair
            assertThat(recomposeChannel.receive()).isEqualTo(2)
            assertThat(component._pointerIcon).isEqualTo(PointerIconDefaults.Crosshair)
        } finally {
            scene.close()
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun whenNotHoveredShouldNeverCommit() = runTest(UnconfinedTestDispatcher()) {
        val component = IconPlatform()
        val surface = Surface.makeRasterN32Premul(100, 100)
        var scene: ComposeScene? = null

        scene = ComposeScene(platform = component, invalidate = {
            scene?.render(surface.canvas, 1)
        }, coroutineContext = coroutineContext)

        val iconState = mutableStateOf(PointerIconDefaults.Text)

        val recomposeChannel = Channel<Int>(Channel.CONFLATED) // helps with waiting for recomposition
        var count = 0
        try {
            scene.constraints = Constraints(maxWidth = surface.width, maxHeight = surface.height)
            scene.setContent {
                Box(modifier = Modifier.size(100.dp, 100.dp).pointerInput(Unit) { }) {
                    Box(
                        modifier = Modifier.pointerHoverIcon(iconState.value).size(30.dp, 30.dp)
                    )
                }
                recomposeChannel.trySend(++count)
            }
            assertThat(recomposeChannel.receive()).isEqualTo(1)
            assertThat(component._pointerIcon).isEqualTo(null)

            // No move, not hovered. No pointer icon change expected
            iconState.value = PointerIconDefaults.Crosshair
            assertThat(recomposeChannel.receive()).isEqualTo(2)
            assertThat(component._pointerIcon).isEqualTo(null)

            // Move, but not hovered. Pointer Icon should be Default
            scene.sendPointerEvent(PointerEventType.Move, Offset(90f, 95f))
            assertThat(component._pointerIcon).isEqualTo(PointerIconDefaults.Default)
        } finally {
            scene.close()
        }
    }

    private class IconPlatform : Platform by Platform.Empty {
        var _pointerIcon: PointerIcon? = null

        override fun setPointerIcon(pointerIcon: PointerIcon) {
            this._pointerIcon = pointerIcon
        }
    }
}
