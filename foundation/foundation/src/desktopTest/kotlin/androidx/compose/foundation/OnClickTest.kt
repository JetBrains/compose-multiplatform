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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.DefaultViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
class OnClickTest {

    private fun testClick(
        pointerMatcher: PointerMatcher,
        button: PointerButton
    ) = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var clicksCount = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .onClick(matcher = pointerMatcher) {
                        clicksCount++
                    }.size(10.dp, 20.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), button = button)
        scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), button = button)
        assertThat(clicksCount).isEqualTo(1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), button = button)
        scene.sendPointerEvent(PointerEventType.Release, Offset(5f, 5f), button = button)
        assertThat(clicksCount).isEqualTo(2)
    }

    @Test
    fun primaryClicks() = testClick(
        button = PointerButton.Primary,
        pointerMatcher = PointerMatcher.mouse(PointerButton.Primary)
    )

    @Test
    fun secondaryClicks() = testClick(
        button = PointerButton.Secondary,
        pointerMatcher = PointerMatcher.mouse(PointerButton.Secondary)
    )

    @Test
    fun tertiaryClicks() = testClick(
        button = PointerButton.Tertiary,
        pointerMatcher = PointerMatcher.mouse(PointerButton.Tertiary)
    )

    @Test
    fun backClicks() = testClick(
        button = PointerButton.Back,
        pointerMatcher = PointerMatcher.mouse(PointerButton.Back)
    )

    @Test
    fun forwardClicks() = testClick(
        button = PointerButton.Forward,
        pointerMatcher = PointerMatcher.mouse(PointerButton.Forward)
    )

    @Test
    fun clickWithTouch() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var clicksCount = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .onClick(matcher = PointerMatcher.touch) {
                        clicksCount++
                    }.size(10.dp, 20.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f), type = PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), type = PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), type = PointerType.Touch)
        assertThat(clicksCount).isEqualTo(1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f), type = PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), type = PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release, Offset(5f, 5f), type = PointerType.Touch)
        assertThat(clicksCount).isEqualTo(2)
    }

    private fun testDoubleClick(
        pointerMatcher: PointerMatcher,
        button: PointerButton
    ) = runBlocking {
        val density = Density(1f)
        val viewConfiguration = DefaultViewConfiguration(density)
        ImageComposeScene(
            width = 100,
            height = 100,
            density = density
        ).use { scene ->
            var clicksCount = 0
            var doubleClickCount = 0

            scene.setContent {
                Box(
                    modifier = Modifier
                        .onClick(
                            matcher = pointerMatcher,
                            onDoubleClick = {
                                doubleClickCount++
                            }
                        ) {
                            clicksCount++
                        }.size(10.dp, 20.dp)
                )
            }

            scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), button = button)
            scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), button = button)
            delay(viewConfiguration.doubleTapTimeoutMillis * 2)
            assertThat(clicksCount).isEqualTo(1)
            assertThat(doubleClickCount).isEqualTo(0)

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), button = button)
            scene.sendPointerEvent(PointerEventType.Release, Offset(5f, 5f), button = button)
            delay(viewConfiguration.doubleTapTimeoutMillis / 2)
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), button = button)
            scene.sendPointerEvent(PointerEventType.Release, Offset(5f, 5f), button = button)
            assertThat(clicksCount).isEqualTo(1)
            assertThat(doubleClickCount).isEqualTo(1)
        }
    }

    @Test
    fun primaryDoubleClick() = testDoubleClick(
        button = PointerButton.Primary,
        pointerMatcher = PointerMatcher.mouse(PointerButton.Primary)
    )

    @Test
    fun secondaryDoubleClick() = testDoubleClick(
        button = PointerButton.Secondary,
        pointerMatcher = PointerMatcher.mouse(PointerButton.Secondary)
    )

    @Test
    fun tertiaryDoubleClick() = testDoubleClick(
        button = PointerButton.Tertiary,
        pointerMatcher = PointerMatcher.mouse(PointerButton.Tertiary)
    )

    private fun testLongClick(
        pointerMatcher: PointerMatcher,
        button: PointerButton
    ) = runBlocking {
        val density = Density(1f)
        val viewConfiguration = DefaultViewConfiguration(density)
        ImageComposeScene(
            width = 100,
            height = 100,
            density = density
        ).use { scene ->
            var clicksCount = 0
            var longClickCount = 0

            scene.setContent {
                Box(
                    modifier = Modifier
                        .onClick(
                            matcher = pointerMatcher,
                            onLongClick = {
                                longClickCount++
                            }) {
                            clicksCount++
                        }.size(10.dp, 20.dp)
                )
            }

            scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), button = button)
            scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), button = button)
            assertThat(clicksCount).isEqualTo(1)
            assertThat(longClickCount).isEqualTo(0)

            scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 5f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(5f, 5f), button = button)
            delay(viewConfiguration.longPressTimeoutMillis * 2)
            assertThat(clicksCount).isEqualTo(1)
            assertThat(longClickCount).isEqualTo(1)
        }
    }

    @Test
    fun primaryLongClick() = testLongClick(
        button = PointerButton.Primary,
        pointerMatcher = PointerMatcher.mouse(PointerButton.Primary)
    )

    @Test
    fun secondaryLongClick() = testLongClick(
        button = PointerButton.Secondary,
        pointerMatcher = PointerMatcher.mouse(PointerButton.Secondary)
    )

    @Test
    fun tertiaryLongClick() =
        testLongClick(
            button = PointerButton.Tertiary,
            pointerMatcher = PointerMatcher.mouse(PointerButton.Tertiary)
        )

    @Test
    fun `handles primary and secondary clicks`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var primaryClicks = 0
        var secondaryClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .onClick(matcher = PointerMatcher.mouse(PointerButton.Primary)) {
                        primaryClicks++
                    }
                    .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                        secondaryClicks++
                    }
                    .size(40.dp, 40.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(
            PointerEventType.Press, Offset(0f, 0f),
            button = PointerButton.Primary
        )
        scene.sendPointerEvent(
            PointerEventType.Release, Offset(0f, 0f),
            button = PointerButton.Primary
        )

        assertThat(primaryClicks).isEqualTo(1)
        assertThat(secondaryClicks).isEqualTo(0)

        scene.sendPointerEvent(
            PointerEventType.Press, Offset(0f, 0f),
            button = PointerButton.Secondary
        )
        scene.sendPointerEvent(
            PointerEventType.Release, Offset(0f, 0f),
            button = PointerButton.Secondary
        )

        assertThat(primaryClicks).isEqualTo(1)
        assertThat(secondaryClicks).isEqualTo(1)
    }

    @Test
    fun `handles primary click with alt keyModifier`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var genericClicks = 0
        var withAltClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .onClick(matcher = PointerMatcher.mouse(PointerButton.Primary)) {
                        genericClicks++
                    }
                    .onClick(
                        matcher = PointerMatcher.mouse(PointerButton.Primary),
                        keyboardModifiers = { isAltPressed },
                    ) {
                        withAltClicks++
                    }
                    .size(40.dp, 40.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        // With Alt pressed
        scene.sendPointerEvent(
            PointerEventType.Press, Offset(0f, 0f),
            button = PointerButton.Primary,
            keyboardModifiers = PointerKeyboardModifiers(isAltPressed = true)
        )
        scene.sendPointerEvent(
            PointerEventType.Release, Offset(0f, 0f),
            keyboardModifiers = PointerKeyboardModifiers(isAltPressed = true),
            button = PointerButton.Primary
        )

        assertThat(withAltClicks).isEqualTo(1)
        assertThat(genericClicks).isEqualTo(0)

        // Without Alt pressed (for generic click handler)
        scene.sendPointerEvent(
            PointerEventType.Press, Offset(0f, 0f),
            button = PointerButton.Primary,
            keyboardModifiers = PointerKeyboardModifiers(isAltPressed = false)
        )
        scene.sendPointerEvent(
            PointerEventType.Release, Offset(0f, 0f),
            keyboardModifiers = PointerKeyboardModifiers(isAltPressed = false),
            button = PointerButton.Primary
        )
        assertThat(withAltClicks).isEqualTo(1)
        assertThat(genericClicks).isEqualTo(1)
    }

    @Test
    fun `consume click`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var outerBoxClicks = 0
        var innerBoxClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .onClick {
                        outerBoxClicks++
                    }
                    .size(40.dp, 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .onClick {
                            innerBoxClicks++
                        }
                        .size(10.dp, 20.dp)
                )
            }
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), button = PointerButton.Primary)
        assertThat(outerBoxClicks).isEqualTo(0)
        assertThat(innerBoxClicks).isEqualTo(1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(30f, 30f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(30f, 30f), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(30f, 30f), button = PointerButton.Primary)
        assertThat(outerBoxClicks).isEqualTo(1)
        assertThat(innerBoxClicks).isEqualTo(1)
    }

    @Test
    fun `don't handle consumed click by another click`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var outerBoxClicks = 0
        var innerBoxClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .onClick {
                        outerBoxClicks++
                    }
                    .size(40.dp, 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .onClick {
                            innerBoxClicks++
                        }
                        .size(10.dp, 20.dp)
                )
            }
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(0f, 0f), button = PointerButton.Primary)
        assertThat(outerBoxClicks).isEqualTo(0)
        assertThat(innerBoxClicks).isEqualTo(1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(30f, 30f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(30f, 30f), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(30f, 30f), button = PointerButton.Primary)
        assertThat(outerBoxClicks).isEqualTo(1)
        assertThat(innerBoxClicks).isEqualTo(1)
    }

    @Test
    fun `don't handle consumed click by pan`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var outerBoxTotalPan = Offset.Zero
        var innerBoxClicks = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, _, _ ->
                            outerBoxTotalPan += pan
                        }
                    }
                    .size(80.dp, 80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .onClick {
                            innerBoxClicks++
                        }
                        .size(50.dp, 50.dp)
                )
            }
        }

        scene.sendPointerEvent(PointerEventType.Move, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(0f, 0f), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Move, Offset(20f, 0f))
        scene.sendPointerEvent(PointerEventType.Release, Offset(20f, 0f), button = PointerButton.Primary)
        assertThat(outerBoxTotalPan).isEqualTo(Offset(20f, 0f))
        assertThat(innerBoxClicks).isEqualTo(0)

        scene.sendPointerEvent(PointerEventType.Press, Offset(20f, 0f), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(20f, 0f), button = PointerButton.Primary)
        assertThat(outerBoxTotalPan).isEqualTo(Offset(20f, 0f))
        assertThat(innerBoxClicks).isEqualTo(1)
    }
}
