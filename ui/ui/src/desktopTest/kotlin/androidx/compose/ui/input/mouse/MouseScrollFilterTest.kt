/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("DEPRECATION") // https://github.com/JetBrains/compose-jb/issues/1514

package androidx.compose.ui.input.mouse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.TestComposeWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(JUnit4::class)
@Ignore // TODO(b/217238066) remove after migration to ImageComposeScene (it will be upstreamed from Compose MPP 1.0.0)
class MouseScrollFilterTest {
    private val window = TestComposeWindow(width = 100, height = 100, density = Density(2f))

    @Test
    fun `inside box`() {
        var actualEvent: MouseScrollEvent? = null
        var actualBounds: IntSize? = null

        window.setContent {
            Box(
                Modifier
                    .mouseScrollFilter { event, bounds ->
                        actualEvent = event
                        actualBounds = bounds
                        true
                    }
                    .size(10.dp, 20.dp)
            )
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(3f), MouseScrollOrientation.Vertical)
        )

        assertThat(actualEvent?.delta).isEqualTo(MouseScrollUnit.Line(3f))
        assertThat(actualEvent?.orientation).isEqualTo(MouseScrollOrientation.Vertical)
        assertThat(actualBounds).isEqualTo(IntSize(20, 40))
    }

    @Test
    fun `outside box`() {
        var actualEvent: MouseScrollEvent? = null
        var actualBounds: IntSize? = null

        window.setContent {
            Box(
                Modifier
                    .mouseScrollFilter { event, bounds ->
                        actualEvent = event
                        actualBounds = bounds
                        true
                    }
                    .size(10.dp, 20.dp)
            )
        }

        window.onMouseScroll(
            x = 20,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(3f), MouseScrollOrientation.Vertical)
        )

        assertThat(actualEvent).isEqualTo(null)
        assertThat(actualBounds).isEqualTo(null)
    }

    @Test
    fun `inside two overlapping boxes`() {
        var actualEvent1: MouseScrollEvent? = null
        var actualBounds1: IntSize? = null
        var actualEvent2: MouseScrollEvent? = null
        var actualBounds2: IntSize? = null

        window.setContent {
            Box(
                Modifier
                    .mouseScrollFilter { event, bounds ->
                        actualEvent1 = event
                        actualBounds1 = bounds
                        true
                    }
                    .size(10.dp, 20.dp)
            )
            Box(
                Modifier
                    .mouseScrollFilter { event, bounds ->
                        actualEvent2 = event
                        actualBounds2 = bounds
                        true
                    }
                    .size(5.dp, 10.dp)
            )
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(3f), MouseScrollOrientation.Horizontal)
        )

        assertThat(actualEvent1).isEqualTo(null)
        assertThat(actualBounds1).isEqualTo(null)
        assertThat(actualEvent2?.delta).isEqualTo(MouseScrollUnit.Line(3f))
        assertThat(actualEvent2?.orientation).isEqualTo(MouseScrollOrientation.Horizontal)
        assertThat(actualBounds2).isEqualTo(IntSize(10, 20))
    }

    @Test
    fun `inside two overlapping boxes, top box doesn't handle scroll`() {
        var actualEvent: MouseScrollEvent? = null
        var actualBounds: IntSize? = null

        window.setContent {
            Box(
                Modifier
                    .mouseScrollFilter { event, bounds ->
                        actualEvent = event
                        actualBounds = bounds
                        true
                    }
                    .size(10.dp, 20.dp)
            )
            Box(
                Modifier
                    .mouseScrollFilter { _, _ ->
                        false
                    }
                    .size(5.dp, 10.dp)
            )
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(3f), MouseScrollOrientation.Horizontal)
        )

        assertThat(actualEvent).isEqualTo(null)
        assertThat(actualBounds).isEqualTo(null)
    }

    @Test
    fun `inside two overlapping boxes, top box doesn't have mouseScrollFilter`() {
        var actualEvent: MouseScrollEvent? = null
        var actualBounds: IntSize? = null

        window.setContent {
            Box(
                Modifier
                    .mouseScrollFilter { event, bounds ->
                        actualEvent = event
                        actualBounds = bounds
                        true
                    }
                    .size(10.dp, 20.dp)
            )
            Box(
                Modifier
                    .size(5.dp, 10.dp)
            )
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(3f), MouseScrollOrientation.Horizontal)
        )

        assertThat(actualEvent?.delta).isEqualTo(MouseScrollUnit.Line(3f))
        assertThat(actualEvent?.orientation).isEqualTo(MouseScrollOrientation.Horizontal)
        assertThat(actualBounds).isEqualTo(IntSize(20, 40))
    }

    @Test
    fun `inside two nested boxes`() {
        var actualEvent1: MouseScrollEvent? = null
        var actualBounds1: IntSize? = null
        var actualEvent2: MouseScrollEvent? = null
        var actualBounds2: IntSize? = null

        window.setContent {
            Box(
                Modifier
                    .mouseScrollFilter { event, bounds ->
                        actualEvent1 = event
                        actualBounds1 = bounds
                        true
                    }
                    .size(10.dp, 20.dp)
            ) {
                Box(
                    Modifier
                        .mouseScrollFilter { event, bounds ->
                            actualEvent2 = event
                            actualBounds2 = bounds
                            true
                        }
                        .size(5.dp, 10.dp)
                )
            }
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Line(-1f), MouseScrollOrientation.Horizontal)
        )

        assertThat(actualEvent1).isEqualTo(null)
        assertThat(actualBounds1).isEqualTo(null)
        assertThat(actualEvent2?.delta).isEqualTo(MouseScrollUnit.Line(-1f))
        assertThat(actualEvent2?.orientation).isEqualTo(MouseScrollOrientation.Horizontal)
        assertThat(actualBounds2).isEqualTo(IntSize(10, 20))
    }

    @Test
    fun `inside two nested boxes, nested box doesn't handle scroll`() {
        var actualEvent: MouseScrollEvent? = null
        var actualBounds: IntSize? = null

        window.setContent {
            Box(
                Modifier
                    .mouseScrollFilter { event, bounds ->
                        actualEvent = event
                        actualBounds = bounds
                        true
                    }
                    .size(10.dp, 20.dp)
            ) {
                Box(
                    Modifier
                        .mouseScrollFilter { _, _ ->
                            false
                        }
                        .size(5.dp, 10.dp)
                )
            }
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Page(1f), MouseScrollOrientation.Horizontal)
        )

        assertThat(actualEvent?.delta).isEqualTo(MouseScrollUnit.Page(1f))
        assertThat(actualEvent?.orientation).isEqualTo(MouseScrollOrientation.Horizontal)
        assertThat(actualBounds).isEqualTo(IntSize(20, 40))
    }

    @Test
    fun `inside two nested boxes, nested box doesn't have mouseScrollFilter`() {
        var actualEvent: MouseScrollEvent? = null
        var actualBounds: IntSize? = null

        window.setContent {
            Box(
                Modifier
                    .mouseScrollFilter { event, bounds ->
                        actualEvent = event
                        actualBounds = bounds
                        true
                    }
                    .size(10.dp, 20.dp)
            ) {
                Box(
                    Modifier
                        .size(5.dp, 10.dp)
                )
            }
        }

        window.onMouseScroll(
            x = 0,
            y = 0,
            event = MouseScrollEvent(MouseScrollUnit.Page(1f), MouseScrollOrientation.Horizontal)
        )

        assertThat(actualEvent?.delta).isEqualTo(MouseScrollUnit.Page(1f))
        assertThat(actualEvent?.orientation).isEqualTo(MouseScrollOrientation.Horizontal)
        assertThat(actualBounds).isEqualTo(IntSize(20, 40))
    }
}