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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runAndroidComposeUiTest
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class MultipleActivitiesFindTest {

    @Test
    fun test() = runAndroidComposeUiTest<Activity1> {
        activity!!.startNewActivity()
        waitUntil {
            onAllNodesWithTag("activity2").isNotEmpty()
        }

        onNodeWithTag("activity1").assertDoesNotExist()
        onNodeWithTag("activity2").assertExists()
    }

    private fun SemanticsNodeInteractionCollection.isNotEmpty(): Boolean {
        return fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }

    class Activity1 : TaggedActivity("activity1")
    class Activity2 : TaggedActivity("activity2")

    open class TaggedActivity(private val tag: String) : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.testTag(tag))
            }
        }

        fun startNewActivity() {
            startActivity(Intent(this, Activity2::class.java))
        }
    }
}
