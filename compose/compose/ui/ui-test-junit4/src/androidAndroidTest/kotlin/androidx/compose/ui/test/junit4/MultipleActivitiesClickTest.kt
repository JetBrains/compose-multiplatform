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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class MultipleActivitiesClickTest {

    @Test
    fun test() = runAndroidComposeUiTest<Activity1> {
        val activity1 = activity!!

        activity1.startNewActivity()
        waitUntil {
            onAllNodesWithTag("activity2").isNotEmpty()
        }

        onNodeWithTag("activity2").performTouchInput { click() }
        val activity2 = getCurrentActivity() as Activity2

        runOnIdle {
            assertThat(activity1.clickCounter).isEqualTo(0)
            assertThat(activity2.clickCounter).isEqualTo(1)
        }
    }

    private fun SemanticsNodeInteractionCollection.isNotEmpty(): Boolean {
        return fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }

    // In general this method to retrieve the current activity may fail, because the presence of
    // an ActivityLifecycleMonitorRegistry is dependent on the instrumentation used. The
    // instrumentation we use in our test setup supports this though, so it is safe to do here.
    private fun ComposeUiTest.getCurrentActivity(): Activity {
        var currentActivity: Activity? = null
        runOnUiThread {
            currentActivity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).first()
        }
        return currentActivity!!
    }

    class Activity1 : ClickRecordingActivity("activity1")
    class Activity2 : ClickRecordingActivity("activity2")

    open class ClickRecordingActivity(private val tag: String) : ComponentActivity() {
        var clickCounter: Int = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                Button(
                    modifier = Modifier.testTag(tag).fillMaxSize(),
                    onClick = { clickCounter++ }
                ) {}
            }
        }

        fun startNewActivity() {
            startActivity(Intent(this, Activity2::class.java))
        }
    }
}
