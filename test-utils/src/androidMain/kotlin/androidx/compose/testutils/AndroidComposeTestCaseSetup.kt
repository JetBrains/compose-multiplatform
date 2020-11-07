/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.testutils

import androidx.activity.ComponentActivity
import java.util.concurrent.CountDownLatch

class AndroidComposeTestCaseSetup(
    private val testCase: ComposeTestCase,
    private val activity: ComponentActivity
) : ComposeTestCaseSetup {
    override fun performTestWithEventsControl(block: ComposeExecutionControl.() -> Unit) {
        var error: Throwable? = null
        val latch = CountDownLatch(1)
        activity.runOnUiThread {
            // TODO: Ensure that no composition exists at this stage!
            val runner = AndroidComposeTestCaseRunner({ testCase }, activity)
            try {
                runner.setupContent()
                block.invoke(runner)
            } catch (t: Throwable) {
                error = t
            } finally {
                runner.disposeContent()
                latch.countDown()
            }
        }
        latch.await()
        error?.let { throw it }
    }
}