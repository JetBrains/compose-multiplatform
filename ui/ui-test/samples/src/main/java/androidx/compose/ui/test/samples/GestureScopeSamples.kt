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
import androidx.compose.ui.test.center
import androidx.compose.ui.test.centerLeft
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.topLeft
import androidx.compose.ui.test.up

@Suppress("DEPRECATION")
@Sampled
fun gestureClick() {
    composeTestRule.onNodeWithTag("myComponent")
        .performGesture { click() }
}

@Suppress("DEPRECATION")
@Sampled
fun gestureSwipeUp() {
    composeTestRule.onNodeWithTag("myComponent")
        .performGesture { swipeUp() }
}

@Suppress("DEPRECATION")
@Sampled
fun gestureLShape() {
    composeTestRule.onNodeWithTag("myComponent")
        .performGesture {
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
