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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalFoundationApi
@MediumTest
@RunWith(AndroidJUnit4::class)
class InteractionSourceTest {

    @get:Rule
    val rule = createComposeRule()

    private object TestInteraction1 : Interaction
    private object TestInteraction2 : Interaction
    private object TestInteraction3 : Interaction

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun emittingInteractionsInOrder() {
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

        rule.runOnIdle {
            assertThat(interactions)
                .containsExactlyElementsIn(
                    listOf(TestInteraction1, TestInteraction2)
                )
                .inOrder()
        }

        scope.launch {
            interactionSource.emit(TestInteraction3)
        }

        rule.runOnIdle {
            assertThat(interactions)
                .containsExactlyElementsIn(
                    listOf(TestInteraction1, TestInteraction2, TestInteraction3)
                )
                .inOrder()
        }
    }

    @Test
    fun isDragged() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isDragged: State<Boolean>? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            isDragged = interactionSource.collectIsDraggedAsState()
        }

        rule.runOnIdle {
            assertThat(isDragged!!.value).isFalse()
        }

        var dragStart: DragInteraction.Start? = null

        scope!!.launch {
            dragStart = DragInteraction.Start()
            interactionSource.emit(dragStart!!)
        }

        rule.runOnIdle {
            assertThat(isDragged!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(DragInteraction.Stop(dragStart!!))
        }

        rule.runOnIdle {
            assertThat(isDragged!!.value).isFalse()
        }
    }

    @Test
    fun isDragged_multipleDrags() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isDragged: State<Boolean>? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            isDragged = interactionSource.collectIsDraggedAsState()
        }

        rule.runOnIdle {
            assertThat(isDragged!!.value).isFalse()
        }

        var dragStart: DragInteraction.Start? = null

        scope!!.launch {
            dragStart = DragInteraction.Start()
            interactionSource.emit(dragStart!!)
        }

        rule.runOnIdle {
            assertThat(isDragged!!.value).isTrue()
        }

        var dragStart2: DragInteraction.Start? = null

        scope!!.launch {
            dragStart2 = DragInteraction.Start()
            interactionSource.emit(dragStart2!!)
        }

        rule.runOnIdle {
            assertThat(isDragged!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(DragInteraction.Stop(dragStart!!))
        }

        rule.runOnIdle {
            assertThat(isDragged!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(DragInteraction.Cancel(dragStart2!!))
        }

        rule.runOnIdle {
            assertThat(isDragged!!.value).isFalse()
        }
    }

    @Test
    fun isFocused() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isFocused: State<Boolean>? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            isFocused = interactionSource.collectIsFocusedAsState()
        }

        rule.runOnIdle {
            assertThat(isFocused!!.value).isFalse()
        }

        var focus: FocusInteraction.Focus? = null

        scope!!.launch {
            focus = FocusInteraction.Focus()
            interactionSource.emit(focus!!)
        }

        rule.runOnIdle {
            assertThat(isFocused!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(FocusInteraction.Unfocus(focus!!))
        }

        rule.runOnIdle {
            assertThat(isFocused!!.value).isFalse()
        }
    }

    @Test
    fun isFocused_multipleFocuses() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isFocused: State<Boolean>? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            isFocused = interactionSource.collectIsFocusedAsState()
        }

        rule.runOnIdle {
            assertThat(isFocused!!.value).isFalse()
        }

        var focus: FocusInteraction.Focus? = null

        scope!!.launch {
            focus = FocusInteraction.Focus()
            interactionSource.emit(focus!!)
        }

        rule.runOnIdle {
            assertThat(isFocused!!.value).isTrue()
        }

        var focus2: FocusInteraction.Focus? = null

        scope!!.launch {
            focus2 = FocusInteraction.Focus()
            interactionSource.emit(focus2!!)
        }

        rule.runOnIdle {
            assertThat(isFocused!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(FocusInteraction.Unfocus(focus!!))
        }

        rule.runOnIdle {
            assertThat(isFocused!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(FocusInteraction.Unfocus(focus2!!))
        }

        rule.runOnIdle {
            assertThat(isFocused!!.value).isFalse()
        }
    }

    @Test
    fun isPressed() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isPressed: State<Boolean>? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            isPressed = interactionSource.collectIsPressedAsState()
        }

        rule.runOnIdle {
            assertThat(isPressed!!.value).isFalse()
        }

        var press: PressInteraction.Press? = null

        scope!!.launch {
            press = PressInteraction.Press(Offset.Zero)
            interactionSource.emit(press!!)
        }

        rule.runOnIdle {
            assertThat(isPressed!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(PressInteraction.Release(press!!))
        }

        rule.runOnIdle {
            assertThat(isPressed!!.value).isFalse()
        }
    }

    @Test
    fun isPressed_multiplePresses() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null
        var isPressed: State<Boolean>? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            isPressed = interactionSource.collectIsPressedAsState()
        }

        rule.runOnIdle {
            assertThat(isPressed!!.value).isFalse()
        }

        var press: PressInteraction.Press? = null

        scope!!.launch {
            press = PressInteraction.Press(Offset.Zero)
            interactionSource.emit(press!!)
        }

        rule.runOnIdle {
            assertThat(isPressed!!.value).isTrue()
        }

        var press2: PressInteraction.Press? = null

        scope!!.launch {
            press2 = PressInteraction.Press(Offset.Zero)
            interactionSource.emit(press2!!)
        }

        rule.runOnIdle {
            assertThat(isPressed!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(PressInteraction.Release(press!!))
        }

        rule.runOnIdle {
            assertThat(isPressed!!.value).isTrue()
        }

        scope!!.launch {
            interactionSource.emit(PressInteraction.Cancel(press2!!))
        }

        rule.runOnIdle {
            assertThat(isPressed!!.value).isFalse()
        }
    }
}
