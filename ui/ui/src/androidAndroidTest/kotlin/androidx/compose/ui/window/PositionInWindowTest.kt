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
package androidx.compose.ui.window

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PositionInWindowTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    lateinit var activity: ComponentActivity

    @Before
    fun setup() {
        rule.activityRule.scenario.onActivity { activity = it }
    }

    // Make sure that the position in the window doesn't change when the window position changes.
    @Test
    fun positionInWindow() {
        var coordinates: LayoutCoordinates? = null
        var size by mutableStateOf(10)
        rule.runOnUiThread {
            val window = activity.window
            val layoutParams = window.attributes
            layoutParams.x = 0
            layoutParams.y = 0
            layoutParams.width = 100
            layoutParams.height = 100
            window.attributes = layoutParams
        }
        rule.setContent {
            with(AmbientDensity.current) {
                Box(Modifier.size(size.toDp()).onGloballyPositioned { coordinates = it })
            }
        }

        var position = Offset.Zero
        rule.runOnIdle {
            position = coordinates!!.positionInWindow()
            size = 12
            val window = activity.window
            val layoutParams = window.attributes
            layoutParams.x = 10
            layoutParams.y = 10
            layoutParams.width = 100
            layoutParams.height = 100
            window.attributes = layoutParams
        }

        rule.runOnIdle {
            val newPosition = coordinates!!.positionInWindow()
            assertThat(newPosition).isEqualTo(position)
        }
    }
}
