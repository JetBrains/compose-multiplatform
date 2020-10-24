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

package androidx.compose.ui.viewinterop

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.LifecycleOwnerAmbient
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.LifecycleOwner
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class ViewModelInAppCompatActivityTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<AppCompatActivity>(
        AppCompatActivity::class.java
    )
    private lateinit var activity: AppCompatActivity

    @Before
    fun setup() {
        activity = activityTestRule.activity
    }

    @Test
    fun lifecycleOwnerIsAvailable() {
        val latch = CountDownLatch(1)
        var owner: LifecycleOwner? = null

        activityTestRule.runOnUiThread {
            activity.setContent {
                owner = LifecycleOwnerAmbient.current
                latch.countDown()
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(activity, owner)
    }

    @Test
    fun lifecycleOwnerIsAvailableWhenComposedIntoViewGroup() {
        val latch = CountDownLatch(1)
        var owner: LifecycleOwner? = null

        activityTestRule.runOnUiThread {
            val view = FrameLayout(activity)
            activity.setContentView(view)
            view.setContent(Recomposer.current()) {
                owner = LifecycleOwnerAmbient.current
                latch.countDown()
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(activity, owner)
    }
}
