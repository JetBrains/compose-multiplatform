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

package androidx.compose.material

import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class ModalBottomSheetStateTest {

    @get:Rule
    val rule = createComposeRule()
    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun test_stateSavedAndRestored() {
        val initialValue = ModalBottomSheetValue.Hidden
        val skipHalfExpanded = true
        val animationSpec = SpringSpec<Float>(visibilityThreshold = 10F)
        lateinit var state: ModalBottomSheetState
        restorationTester.setContent {
            state = rememberModalBottomSheetState(
                initialValue = initialValue,
                skipHalfExpanded = skipHalfExpanded,
                animationSpec = animationSpec
            )
        }

        assertThat(state.animationSpec).isEqualTo(animationSpec)
        assertThat(state.currentValue).isEqualTo(initialValue)
        assertThat(state.isSkipHalfExpanded).isEqualTo(skipHalfExpanded)

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(state.animationSpec).isEqualTo(animationSpec)
        assertThat(state.currentValue).isEqualTo(initialValue)
        assertThat(state.isSkipHalfExpanded).isEqualTo(skipHalfExpanded)
    }

    @Test
    fun test_halfExpandDisabled_initialValueHalfExpanded_throws() {
        try {
            ModalBottomSheetState(
                initialValue = ModalBottomSheetValue.HalfExpanded,
                isSkipHalfExpanded = true
            )
            fail("ModalBottomSheetState didn't throw an exception")
        } catch (exception: IllegalArgumentException) {
            assertThat(exception)
                .hasMessageThat()
                .isNotEmpty()
        }
    }

    @Test
    fun test_halfExpandDisabled_initialValueHidden_doesntThrow() {
        ModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            isSkipHalfExpanded = true
        )
    }

    @Test
    fun test_halfExpandDisabled_initialValueExpanded_doesntThrow() {
        ModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Expanded,
            isSkipHalfExpanded = true
        )
    }
}
