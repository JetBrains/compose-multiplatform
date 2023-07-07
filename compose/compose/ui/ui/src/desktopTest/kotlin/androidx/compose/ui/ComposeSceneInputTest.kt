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

package androidx.compose.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.IntRect
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class)
class ComposeSceneInputTest {
    @Test
    fun move() = ImageComposeScene(100, 100).use { scene ->
        val background = FillBox()

        scene.setContent {
            background.Content()
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(10f, 10f))
        background.events.assertReceivedLast(PointerEventType.Enter, Offset(10f, 10f))

        scene.sendPointerEvent(PointerEventType.Move, Offset(20f, 10f))
        background.events.assertReceivedLast(PointerEventType.Move, Offset(20f, 10f))

        scene.sendPointerEvent(PointerEventType.Move, Offset(2000f, 10f))
        background.events.assertReceivedLast(PointerEventType.Exit, Offset(2000f, 10f))

        scene.sendPointerEvent(PointerEventType.Move, Offset(50f, 10f))
        background.events.assertReceivedLast(PointerEventType.Enter, Offset(50f, 10f))

        scene.sendPointerEvent(PointerEventType.Exit, Offset(50f, 10f))
        background.events.assertReceivedLast(PointerEventType.Exit, Offset(50f, 10f))

        scene.sendPointerEvent(PointerEventType.Enter, Offset(-10f, 10f))
        background.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Enter, Offset(0f, 0f))
        background.events.assertReceivedLast(PointerEventType.Enter, Offset(0f, 0f))
    }

    @Test
    fun `move to popup`() = ImageComposeScene(100, 100).use { scene ->
        val background = FillBox()
        val cutPopup = PopupState(IntRect(-20, -20, 40, 40))
        val overlappedPopup = PopupState(IntRect(20, 20, 60, 60))
        val independentPopup = PopupState(IntRect(80, 80, 90, 90))

        scene.setContent {
            background.Content()
            cutPopup.Content()
            overlappedPopup.Content()
            independentPopup.Content()
        }

        // Popup takes two iterations to complete its layout, so we need to run render an extra time
        // TODO(maryanovsky): Remove this when https://github.com/JetBrains/compose-jb/issues/2726
        //                    is completed.
        while (scene.hasInvalidations())
            scene.render()

        scene.sendPointerEvent(PointerEventType.Enter, Offset(-10f, -10f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Enter, Offset(-10f, -10f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 10f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Move, Offset(10f, 10f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Move, Offset(20f, 20f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Exit, Offset(20f, 20f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedLast(
            PointerEventType.Enter, Offset(20f, 20f) - overlappedPopup.origin)
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Move, Offset(80f, 80f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedNoEvents()
        overlappedPopup.events.assertReceivedLast(
            PointerEventType.Exit, Offset(80f, 80f) - overlappedPopup.origin)
        independentPopup.events.assertReceivedLast(
            PointerEventType.Enter, Offset(80f, 80f) - independentPopup.origin)

        scene.sendPointerEvent(PointerEventType.Move, Offset(90f, 90f))
        background.events.assertReceivedLast(PointerEventType.Enter, Offset(90f, 90f))
        cutPopup.events.assertReceivedNoEvents()
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedLast(
            PointerEventType.Exit, Offset(90f, 90f) - independentPopup.origin)

        scene.sendPointerEvent(PointerEventType.Move, Offset(100f, 100f))
        background.events.assertReceivedLast(PointerEventType.Exit, Offset(100f, 100f))
        cutPopup.events.assertReceivedNoEvents()
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()
    }

    @Test
    fun click() = ImageComposeScene(100, 100).use { scene ->
        val background = FillBox()

        scene.setContent {
            background.Content()
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(10f, 10f))
        background.events.assertReceivedLast(PointerEventType.Enter, Offset(10f, 10f))

        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
        background.events.assertReceivedLast(PointerEventType.Press, Offset(10f, 10f))

        scene.sendPointerEvent(PointerEventType.Release, Offset(20f, 10f))
        background.events.assertReceived(PointerEventType.Move, Offset(20f, 10f))
        background.events.assertReceivedLast(PointerEventType.Release, Offset(20f, 10f))

        scene.sendPointerEvent(PointerEventType.Press, Offset(20f, 10f))
        background.events.assertReceivedLast(PointerEventType.Press, Offset(20f, 10f))

        scene.sendPointerEvent(PointerEventType.Move, Offset(30f, 10f))
        background.events.assertReceivedLast(PointerEventType.Move, Offset(30f, 10f))

        scene.sendPointerEvent(PointerEventType.Release, Offset(30f, 10f))
        background.events.assertReceivedLast(PointerEventType.Release, Offset(30f, 10f))
    }

    @Test
    fun `pressed popup should own received moves outside popup`() = ImageComposeScene(
        100,
        100
    ).use { scene ->
        val background = FillBox()
        val cutPopup = PopupState(IntRect(-20, -20, 40, 40))
        val overlappedPopup = PopupState(IntRect(20, 20, 60, 60))
        val independentPopup = PopupState(IntRect(80, 80, 90, 90))

        scene.setContent {
            background.Content()
            cutPopup.Content()
            overlappedPopup.Content()
            independentPopup.Content()
        }

        // Popup takes two iterations to complete its layout, so we need to run render an extra time
        // TODO(maryanovsky): Remove this when https://github.com/JetBrains/compose-jb/issues/2726
        //                    is completed.
        while (scene.hasInvalidations())
            scene.render()

        scene.sendPointerEvent(PointerEventType.Enter, Offset(-10f, -10f))
        scene.sendPointerEvent(PointerEventType.Press, Offset(-10f, -10f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceived(PointerEventType.Enter, Offset(-10f, -10f) - cutPopup.origin)
        cutPopup.events.assertReceivedLast(
            PointerEventType.Press, Offset(-10f, -10f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Enter, Offset(10f, 10f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Move, Offset(10f, 10f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Move, Offset(20f, 20f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Move, Offset(20f, 20f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Move, Offset(80f, 80f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Exit, Offset(80f, 80f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Move, Offset(90f, 90f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Move, Offset(90f, 90f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Move, Offset(100f, 100f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Move, Offset(100f, 100f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Move, Offset(110f, 110f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Move, Offset(110f, 110f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Move, Offset(20f, 20f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Enter, Offset(20f, 20f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Release, Offset(20f, 20f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Release, Offset(20f, 20f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Move, Offset(20f, 20f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedLast(
            PointerEventType.Exit, Offset(20f, 20f) - cutPopup.origin)
        overlappedPopup.events.assertReceivedLast(
            PointerEventType.Enter, Offset(20f, 20f) - overlappedPopup.origin)
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Press, Offset(20f, 20f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedNoEvents()
        overlappedPopup.events.assertReceivedLast(
            PointerEventType.Press, Offset(20f, 20f) - overlappedPopup.origin)
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Move, Offset(-10f, -10f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedNoEvents()
        overlappedPopup.events.assertReceivedLast(
            PointerEventType.Exit, Offset(-10f, -10f) - overlappedPopup.origin)
        independentPopup.events.assertReceivedNoEvents()

        scene.sendPointerEvent(PointerEventType.Release, Offset(-10f, -10f))
        background.events.assertReceivedNoEvents()
        cutPopup.events.assertReceivedNoEvents()
        overlappedPopup.events.assertReceivedLast(
            PointerEventType.Release, Offset(-10f, -10f) - overlappedPopup.origin
        )
        overlappedPopup.events.assertReceivedNoEvents()
        independentPopup.events.assertReceivedNoEvents()
    }

    @Test
    fun scroll() = ImageComposeScene(100, 100).use { scene ->
        val background = FillBox()

        scene.setContent {
            background.Content()
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(20f, 10f))
        background.events.assertReceivedLast(PointerEventType.Enter, Offset(20f, 10f))

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(10f, 10f))
        background.events.assertReceived(PointerEventType.Move, Offset(10f, 10f))
        background.events.assertReceivedLast(PointerEventType.Scroll, Offset(10f, 10f))

        scene.sendPointerEvent(PointerEventType.Move, Offset(20f, 10f))
        background.events.assertReceivedLast(PointerEventType.Move, Offset(20f, 10f))

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(20f, 10f))
        background.events.assertReceivedLast(PointerEventType.Scroll, Offset(20f, 10f))

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(30f, 10f))
        background.events.assertReceived(PointerEventType.Move, Offset(30f, 10f))
        background.events.assertReceivedLast(PointerEventType.Scroll, Offset(30f, 10f))
    }

    @Test
    fun touch() = ImageComposeScene(100, 100).use { scene ->
        val background = FillBox()

        scene.setContent {
            background.Content()
        }

        scene.sendPointerEvent(
            PointerEventType.Press,
            touch(10f, 20f, pressed = true, id = 1)
        )
        background.events.assertReceivedLast(
            PointerEventType.Press,
            touch(10f, 20f, pressed = true, id = 1)
        )

        scene.sendPointerEvent(
            PointerEventType.Move,
            touch(10f, 30f, pressed = true, id = 1)
        )
        background.events.assertReceivedLast(
            PointerEventType.Move,
            touch(10f, 30f, pressed = true, id = 1)
        )

        scene.sendPointerEvent(
            PointerEventType.Release,
            touch(10f, 30f, pressed = false, id = 1)
        )
        background.events.assertReceivedLast(
            PointerEventType.Release,
            touch(10f, 30f, pressed = false, id = 1)
        )
    }

    @Test
    fun `multitouch, send multiple touch changes as multiple events`() = ImageComposeScene(
        100, 100
    ).use { scene ->
        val background = FillBox()

        scene.setContent {
            background.Content()
        }

        scene.sendPointerEvent(
            PointerEventType.Press,
            touch(10f, 20f, pressed = true, id = 1)
        )
        scene.sendPointerEvent(
            PointerEventType.Press,
            touch(10f, 20f, pressed = true, id = 1),
            touch(1f, 20f, pressed = true, id = 2),
        )
        background.events.assertReceived(
            PointerEventType.Press,
            touch(10f, 20f, pressed = true, id = 1)
        )
        background.events.assertReceivedLast(
            PointerEventType.Press,
            touch(10f, 20f, pressed = true, id = 1),
            touch(1f, 20f, pressed = true, id = 2),
        )

        scene.sendPointerEvent(
            PointerEventType.Move,
            touch(10f, 55f, pressed = true, id = 1),
            touch(1f, 20f, pressed = true, id = 2),
        )
        background.events.assertReceivedLast(
            PointerEventType.Move,
            touch(10f, 55f, pressed = true, id = 1),
            touch(1f, 20f, pressed = true, id = 2),
        )

        scene.sendPointerEvent(
            PointerEventType.Move,
            touch(10f, 55f, pressed = true, id = 1),
            touch(1f, 55f, pressed = true, id = 2),
        )
        background.events.assertReceivedLast(
            PointerEventType.Move,
            touch(10f, 55f, pressed = true, id = 1),
            touch(1f, 55f, pressed = true, id = 2),
        )

        scene.sendPointerEvent(
            PointerEventType.Release,
            touch(10f, 55f, pressed = false, id = 1),
            touch(1f, 55f, pressed = true, id = 2),
        )
        background.events.assertReceivedLast(
            PointerEventType.Release,
            touch(10f, 55f, pressed = false, id = 1),
            touch(1f, 55f, pressed = true, id = 2),
        )

        scene.sendPointerEvent(
            PointerEventType.Release,
            touch(1f, 55f, pressed = false, id = 2)
        )
        background.events.assertReceivedLast(
            PointerEventType.Release,
            touch(1f, 55f, pressed = false, id = 2)
        )
    }

    @Test
    fun `multitouch, send multiple touch changes in a single event`() = ImageComposeScene(
        100, 100
    ).use { scene ->
        val background = FillBox()

        scene.setContent {
            background.Content()
        }

        println("Q1")
        scene.sendPointerEvent(
            PointerEventType.Press,
            touch(10f, 20f, pressed = true, id = 1),
            touch(1f, 20f, pressed = true, id = 2),
        )
        // Simulate sequential touch presses, not all touches at once
        background.events.assertReceived(
            PointerEventType.Press,
            touch(10f, 20f, pressed = true, id = 1),
        )
        background.events.assertReceivedLast(
            PointerEventType.Press,
            touch(10f, 20f, pressed = true, id = 1),
            touch(1f, 20f, pressed = true, id = 2),
        )

        scene.sendPointerEvent(
            PointerEventType.Move,
            touch(10f, 55f, pressed = true, id = 1),
            touch(1f, 55f, pressed = true, id = 2),
        )
        background.events.assertReceivedLast(
            PointerEventType.Move,
            touch(10f, 55f, pressed = true, id = 1),
            touch(1f, 55f, pressed = true, id = 2),
        )

        scene.sendPointerEvent(
            PointerEventType.Release,
            touch(1f, 1f, pressed = false, id = 1),
            touch(1f, 2f, pressed = false, id = 2),
        )
        // Position is changed, we need to generate a synthetic Move for this position
        background.events.assertReceived(
            PointerEventType.Move,
            touch(1f, 1f, pressed = true, id = 1),
            touch(1f, 2f, pressed = true, id = 2),
        )
        background.events.assertReceived(
            PointerEventType.Release,
            touch(1f, 1f, pressed = false, id = 1),
            touch(1f, 2f, pressed = true, id = 2),
        )
        background.events.assertReceivedLast(
            PointerEventType.Release,
            touch(1f, 2f, pressed = false, id = 2),
        )
    }
}
