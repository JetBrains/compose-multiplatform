/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals


@OptIn(ExperimentalTestApi::class)
class GraphicsLayerWithInputTest {

    @Test
    fun testClickOnScaledBox() = runSkikoComposeUiTest {
        var clickCounter = 0
        var scale by mutableStateOf(1f)
        var containerSize = IntSize.Zero

        setContent {
            Box(
                modifier = Modifier.fillMaxSize()
                    .testTag("container")
                    .onGloballyPositioned {
                        containerSize = it.size
                    }
            ) {
                Box(modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                    .align(Alignment.Center)
                    .size(20.dp)
                    .background(Color.Green)
                    .clickable {
                        clickCounter++
                    })
            }
        }

        assertEquals(0, clickCounter)
        println("Size = $containerSize")
        assertNotEquals(IntSize.Zero, containerSize)

        this.onNodeWithTag("container").performMouseInput {
            click(Offset(containerSize.width / 2f, containerSize.height / 2f))
        }
        waitForIdle()
        assertEquals(1, clickCounter)


        this.onNodeWithTag("container").performMouseInput {
            // top left corner
            click(Offset(containerSize.width / 2f - 9, containerSize.height / 2f - 9))
        }
        waitForIdle()
        assertEquals(2, clickCounter)

        this.onNodeWithTag("container").performMouseInput {
            // bottom right corner
            click(Offset(containerSize.width / 2f + 9, containerSize.height / 2f + 9))
        }
        waitForIdle()
        assertEquals(3, clickCounter)


        // Now we try to click beyond the box before scaling

        this.onNodeWithTag("container").performMouseInput {
            click(Offset(containerSize.width / 2f - 18, containerSize.height / 2f - 18))
        }
        waitForIdle()
        assertEquals(3, clickCounter)

        this.onNodeWithTag("container").performMouseInput {
            // bottom right corner
            click(Offset(containerSize.width / 2f + 18, containerSize.height / 2f + 18))
        }
        waitForIdle()
        assertEquals(3, clickCounter)

        scale = 2f
        waitForIdle()


        // After scaling: Now clicking beyond the old box rectangle

        this.onNodeWithTag("container").performMouseInput {
            click(Offset(containerSize.width / 2f - 18, containerSize.height / 2f - 18))
        }
        waitForIdle()
        assertEquals(4, clickCounter)

        this.onNodeWithTag("container").performMouseInput {
            // bottom right corner
            click(Offset(containerSize.width / 2f + 18, containerSize.height / 2f + 18))
        }
        waitForIdle()
        assertEquals(5, clickCounter)
    }
}
