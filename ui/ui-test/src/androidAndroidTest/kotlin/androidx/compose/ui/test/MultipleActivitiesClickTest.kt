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

package androidx.compose.ui.test

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.platform.testTag
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class MultipleActivitiesClickTest {

    @get:Rule
    val rule = createAndroidComposeRule<Activity1>()

    @Test
    fun test() {
        lateinit var activity1: Activity1
        rule.activityRule.scenario.onActivity { activity1 = it }
        activity1.startNewActivity()
        rule.onNodeWithTag("activity2").performGesture { click() }
        val activity2 = getCurrentActivity() as Activity2

        assertThat(activity1.recorder.events).isEmpty()
        assertThat(activity2.recorder.events).isNotEmpty()
    }

    // In general this method to retrieve the current activity may fail, because the presence of
    // an ActivityLifecycleMonitorRegistry is dependent on the instrumentation used. The
    // instrumentation we use in our test setup supports this though, so it is safe to do here.
    private fun getCurrentActivity(): Activity {
        var currentActivity: Activity? = null
        rule.runOnUiThread {
            currentActivity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).first()
        }
        return currentActivity!!
    }

    class Activity1 : ClickRecordingActivity("activity1")
    class Activity2 : ClickRecordingActivity("activity2")

    open class ClickRecordingActivity(private val tag: String) : ComponentActivity() {
        val recorder = SinglePointerInputRecorder()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                Box(modifier = Modifier.testTag(tag).fillMaxSize().then(recorder))
            }
        }

        fun startNewActivity() {
            startActivity(Intent(this, Activity2::class.java))
        }
    }
}
