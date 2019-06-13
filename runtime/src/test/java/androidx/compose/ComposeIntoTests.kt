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

package androidx.compose

import android.app.Activity
import android.os.Bundle
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config


@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class ComposeIntoTests : TestCase() {

    @Test
    fun testMultipleSetContentCalls() {
        val controller = Robolectric.buildActivity(ComposeIntoTestActivity::class.java)
        val activity = controller.create().get() as ComposeIntoTestActivity
        assertEquals(1, activity.initializationCount)
        assertEquals(1, activity.commitCount)
        activity.run()
        // if we call setContent multiple times, we want to ensure that it doesn't tear
        // down the whole hierarchy, so onActive should only get called once.
        assertEquals(1, activity.initializationCount)
        assertEquals(2, activity.commitCount)
    }
}

private class ComposeIntoTestActivity : Activity() {
    var initializationCount = 0
    var commitCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        run()
    }
    fun run() {
        setContent {
            +onActive { initializationCount++ }
            +onCommit { commitCount++ }
        }
    }
}