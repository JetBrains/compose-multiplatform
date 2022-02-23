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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp

@Sampled
fun touchInputClick() {
    composeTestRule.onNodeWithTag("myComponent")
        .performTouchInput { click() }
}

@Sampled
fun touchInputSwipeUp() {
    composeTestRule.onNodeWithTag("myComponent")
        .performTouchInput { swipeUp() }
}

@Sampled
fun touchInputClickOffCenter() {
    composeTestRule.onNodeWithTag("myComponent")
        .performTouchInput { click(percentOffset(.2f, .5f)) }
}

@Sampled
fun touchInputAssertDuringClick() {
    composeTestRule.onNodeWithTag("myComponent")
        .performTouchInput { down(topLeft) }
        .assertHasClickAction()
        .performTouchInput { up() }
}

@Sampled
fun touchInputClickAndDrag() {
    composeTestRule.onNodeWithTag("myComponent").performTouchInput {
        click()
        advanceEventTime(100)
        swipeUp()
    }
}

@Sampled
fun touchInputLShapedGesture() {
    composeTestRule.onNodeWithTag("myComponent")
        .performTouchInput {
            down(topLeft)
            moveTo(topLeft + percentOffset(0f, .1f))
            moveTo(topLeft + percentOffset(0f, .2f))
            moveTo(topLeft + percentOffset(0f, .3f))
            moveTo(topLeft + percentOffset(0f, .4f))
            moveTo(centerLeft)
            moveTo(centerLeft + percentOffset(.1f, 0f))
            moveTo(centerLeft + percentOffset(.2f, 0f))
            moveTo(centerLeft + percentOffset(.3f, 0f))
            moveTo(centerLeft + percentOffset(.4f, 0f))
            moveTo(center)
            up()
        }
}
