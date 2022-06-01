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

package androidx.compose.ui.test.junit4

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runEmptyComposeUiTest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test

/**
 * This test shows how you can test an application where the Activity that is launched from the
 * test does not contain any Compose content, but an Activity to which the test eventually
 * navigates does contain Compose content.
 *
 * In the test, Activity1 is launched, which contains an android.widget.Button.
 * When clicked, Activity2 is launched, which contains another android.widget.Button.
 * When that button is clicked, Activity3 is launched, which contains Compose content.
 * The Compose content is finally asserted to exist.
 */
@OptIn(ExperimentalTestApi::class)
class MultipleActivitiesWithoutComposeTest {

    // Because Activity1 does not use Compose, we do not have to guarantee that the
    // ComposeTestRule performs setup before the ActivityScenarioRule, so we can just define the
    // scenario rule as a sibling rule.
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(Activity1::class.java)

    @Test
    fun test() = runEmptyComposeUiTest {
        onView(withText("CLICK_1")).perform(click())
        onView(withText("CLICK_2")).perform(click())
        onNodeWithTag("compose-box").assertExists()
    }

    class Activity1 : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setViewContent {
                frameLayout {
                    button("CLICK_1") {
                        startActivity(Intent(this@Activity1, Activity2::class.java))
                    }
                }
            }
        }
    }

    class Activity2 : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setViewContent {
                frameLayout {
                    button("CLICK_2") {
                        startActivity(Intent(this@Activity2, Activity3::class.java))
                    }
                }
            }
        }
    }

    class Activity3 : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.testTag("compose-box"))
            }
        }
    }
}

private fun Activity.setViewContent(content: Activity.() -> View) {
    setContentView(content())
}

private fun Context.frameLayout(content: FrameLayout.() -> Unit): FrameLayout {
    return FrameLayout(this).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        content()
    }
}

private fun ViewGroup.button(text: String, onClick: (View) -> Unit): Button {
    return Button(context).also {
        it.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
        it.text = text
        it.setOnClickListener(onClick)
        addView(it)
    }
}
