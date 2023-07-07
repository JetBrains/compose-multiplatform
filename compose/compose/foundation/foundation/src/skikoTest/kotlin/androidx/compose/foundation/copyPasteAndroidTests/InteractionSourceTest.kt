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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.containsExactly
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isFalse
import androidx.compose.foundation.isTrue
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalTestApi::class)
@ExperimentalFoundationApi
class InteractionSourceTest {

    private object TestInteraction1 : Interaction
    private object TestInteraction2 : Interaction
    private object TestInteraction3 : Interaction

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun emittingInteractionsInOrder() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()

        val interactions = mutableListOf<Interaction>()

        val scope = TestScope(UnconfinedTestDispatcher())

        scope.launch {
            interactionSource.interactions.collect {
                interactions.add(it)
            }
        }

        scope.launch {
            interactionSource.emit(TestInteraction1)
            interactionSource.emit(TestInteraction2)
        }

        runOnIdle {
            assertThat(interactions).containsExactly(TestInteraction1, TestInteraction2)
        }

        scope.launch {
            interactionSource.emit(TestInteraction3)
        }

        runOnIdle {
            assertThat(interactions).containsExactly(TestInteraction1, TestInteraction2, TestInteraction3)
        }
    }

    @Test
    fun isDragged() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isDragged: State<Boolean>? = null

        setContent {
            scope = rememberCoroutineScope()
            isDragged = interactionSource.collectIsDraggedAsState()
        }

        runOnIdle {
            assertThat(isDragged!!.value).isFalse()
        }

        var dragStart: DragInteraction.Start? = null

        scope!!.launch {
            dragStart = DragInteraction.Start()
            interactionSource.emit(dragStart!!)
        }

        runOnIdle {
            assertThat(isDragged!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(DragInteraction.Stop(dragStart!!))
        }

        runOnIdle {
            assertThat(isDragged!!.value).isFalse()
        }
    }

    @Test
    fun isDragged_multipleDrags() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isDragged: State<Boolean>? = null

        setContent {
            scope = rememberCoroutineScope()
            isDragged = interactionSource.collectIsDraggedAsState()
        }

        runOnIdle {
            assertThat(isDragged!!.value).isFalse()
        }

        var dragStart: DragInteraction.Start? = null

        scope!!.launch {
            dragStart = DragInteraction.Start()
            interactionSource.emit(dragStart!!)
        }

        runOnIdle {
            assertThat(isDragged!!.value).isTrue()
        }

        var dragStart2: DragInteraction.Start? = null

        scope!!.launch {
            dragStart2 = DragInteraction.Start()
            interactionSource.emit(dragStart2!!)
        }

        runOnIdle {
            assertThat(isDragged!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(DragInteraction.Stop(dragStart!!))
        }

        runOnIdle {
            assertThat(isDragged!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(DragInteraction.Cancel(dragStart2!!))
        }

        runOnIdle {
            assertThat(isDragged!!.value).isFalse()
        }
    }

    @Test
    fun isFocused() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isFocused: State<Boolean>? = null

        setContent {
            scope = rememberCoroutineScope()
            isFocused = interactionSource.collectIsFocusedAsState()
        }

        runOnIdle {
            assertThat(isFocused!!.value).isFalse()
        }

        var focus: FocusInteraction.Focus? = null

        scope!!.launch {
            focus = FocusInteraction.Focus()
            interactionSource.emit(focus!!)
        }

        runOnIdle {
            assertThat(isFocused!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(FocusInteraction.Unfocus(focus!!))
        }

        runOnIdle {
            assertThat(isFocused!!.value).isFalse()
        }
    }

    @Test
    fun isFocused_multipleFocuses() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isFocused: State<Boolean>? = null

        setContent {
            scope = rememberCoroutineScope()
            isFocused = interactionSource.collectIsFocusedAsState()
        }

        runOnIdle {
            assertThat(isFocused!!.value).isFalse()
        }

        var focus: FocusInteraction.Focus? = null

        scope!!.launch {
            focus = FocusInteraction.Focus()
            interactionSource.emit(focus!!)
        }

        runOnIdle {
            assertThat(isFocused!!.value).isTrue()
        }

        var focus2: FocusInteraction.Focus? = null

        scope!!.launch {
            focus2 = FocusInteraction.Focus()
            interactionSource.emit(focus2!!)
        }

        runOnIdle {
            assertThat(isFocused!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(FocusInteraction.Unfocus(focus!!))
        }

        runOnIdle {
            assertThat(isFocused!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(FocusInteraction.Unfocus(focus2!!))
        }

        runOnIdle {
            assertThat(isFocused!!.value).isFalse()
        }
    }

    @Test
    fun isPressed() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isPressed: State<Boolean>? = null

        setContent {
            scope = rememberCoroutineScope()
            isPressed = interactionSource.collectIsPressedAsState()
        }

        runOnIdle {
            assertThat(isPressed!!.value).isFalse()
        }

        var press: PressInteraction.Press? = null

        scope!!.launch {
            press = PressInteraction.Press(Offset.Zero)
            interactionSource.emit(press!!)
        }

        runOnIdle {
            assertThat(isPressed!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(PressInteraction.Release(press!!))
        }

        runOnIdle {
            assertThat(isPressed!!.value).isFalse()
        }
    }

    @Test
    fun isPressed_multiplePresses() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isPressed: State<Boolean>? = null

        setContent {
            scope = rememberCoroutineScope()
            isPressed = interactionSource.collectIsPressedAsState()
        }

        runOnIdle {
            assertThat(isPressed!!.value).isFalse()
        }

        var press: PressInteraction.Press? = null

        scope!!.launch {
            press = PressInteraction.Press(Offset.Zero)
            interactionSource.emit(press!!)
        }

        runOnIdle {
            assertThat(isPressed!!.value).isTrue()
        }

        var press2: PressInteraction.Press? = null

        scope!!.launch {
            press2 = PressInteraction.Press(Offset.Zero)
            interactionSource.emit(press2!!)
        }

        runOnIdle {
            assertThat(isPressed!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(PressInteraction.Release(press!!))
        }

        runOnIdle {
            assertThat(isPressed!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(PressInteraction.Cancel(press2!!))
        }

        runOnIdle {
            assertThat(isPressed!!.value).isFalse()
        }
    }
}
