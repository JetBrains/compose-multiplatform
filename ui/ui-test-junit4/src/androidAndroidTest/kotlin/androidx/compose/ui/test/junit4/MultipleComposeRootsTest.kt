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

package androidx.compose.ui.test.junit4

import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.TriStateCheckbox
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith

fun MutableState<ToggleableState>.toggle() {
    value =
        if (value == ToggleableState.On) {
            ToggleableState.Off
        } else {
            ToggleableState.On
        }
}

/**
 * These are tests but also demonstration of our capability to test Compose as part of legacy
 * Android hierarchy. This also includes showcase of multiple Compose roots.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class MultipleComposeRootsTest {
    /**
     * In this setup we have the following configuration:
     *
     * Title 1            < Android TextView
     * [ ] Checkbox 1     < Compose root
     * Title 2            < Android TextView
     * [ ] Checkbox 2     < Compose root
     *
     * Both checkboxes and titles share the same data model. However the titles are regular Android
     * Text Views updated imperatively via listeners on the checkboxes. This test seamlessly
     * modifies and asserts state of the checkboxes and titles using a mix of Espresso and Compose
     * testing APIs.
     */
    @Test
    fun twoHierarchiesSharingTheSameModel() = runAndroidComposeUiTest<ComponentActivity> {
        runOnUiThread {
            val activity = activity!!
            val state1 = mutableStateOf(value = ToggleableState.Off)
            val state2 = mutableStateOf(value = ToggleableState.On)

            val linearLayout = LinearLayout(activity)
                .apply { orientation = LinearLayout.VERTICAL }

            val textView1 = TextView(activity).apply { text = "Compose 1" }
            val composeView1 = ComposeView(activity)

            val textView2 = TextView(activity).apply { text = "Compose 2" }
            val composeView2 = ComposeView(activity)

            activity.setContentView(linearLayout)
            linearLayout.addView(textView1)
            linearLayout.addView(composeView1)
            linearLayout.addView(textView2)
            linearLayout.addView(composeView2)

            fun updateTitle1() {
                textView1.text = "Compose 1 - ${state1.value}"
            }

            fun updateTitle2() {
                textView2.text = "Compose 2 - ${state2.value}"
            }

            composeView1.setContent {
                MaterialTheme {
                    Surface {
                        TriStateCheckbox(
                            modifier = Modifier.testTag("checkbox1"),
                            state = state1.value,
                            onClick = {
                                state1.toggle()
                                state2.toggle()
                                updateTitle1()
                                updateTitle2()
                            }
                        )
                    }
                }
            }

            composeView2.setContent {
                MaterialTheme {
                    Surface {
                        TriStateCheckbox(
                            modifier = Modifier.testTag("checkbox2"),
                            state = state2.value,
                            onClick = {
                                state1.toggle()
                                state2.toggle()
                                updateTitle1()
                                updateTitle2()
                            }
                        )
                    }
                }
            }
        }

        Espresso.onView(withText("Compose 1")).check(matches(isDisplayed()))
        Espresso.onView(withText("Compose 2")).check(matches(isDisplayed()))

        onNodeWithTag("checkbox1")
            .performClick()
            .assertIsOn()

        onNodeWithTag("checkbox2")
            .assertIsOff()

        Espresso.onView(withText("Compose 1 - On")).check(matches(isDisplayed()))
        Espresso.onView(withText("Compose 2 - Off")).check(matches(isDisplayed()))

        onNodeWithTag("checkbox2")
            .performClick()
            .assertIsOn()

        onNodeWithTag("checkbox1")
            .assertIsOff()

        Espresso.onView(withText("Compose 1 - Off")).check(matches(isDisplayed()))
        Espresso.onView(withText("Compose 2 - On")).check(matches(isDisplayed()))
    }
}
