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

package androidx.compose.runtime

import android.app.Activity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Text
import androidx.compose.runtime.benchmark.ComposeActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Test the hot reload with sub-composition (specifically BoxWithConstraints).
 *
 * It is a bit odd for this to be in the benchmark project but, for one test, it seemed overkill
 * to create a separate integration test project.
 *
 * If we end up adding more tests a new project should be created.
 *
 * Regression test for b/148818582
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HotReloadIntegrationTests {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(ComposeActivity::class.java)

    @Test
    fun testSubComposition() {
        val activity = activityRule.activity
        activity.uiThread {
            activity.setContent {
                Column {
                    BoxWithConstraints {
                        ModalDrawer(
                            drawerContent = { },
                            content = { Text(text = "Hello") }
                        )
                    }
                }
            }
        }

        activity.onNextFrame {
            simulateHotReload(activity)
        }
    }
}

fun Activity.uiThread(block: () -> Unit) {
    val latch = CountDownLatch(1)
    var exception: Throwable? = null
    runOnUiThread {
        try {
            block()
        } catch (e: Throwable) {
            exception = e
        }
        latch.countDown()
    }
    latch.await(5, TimeUnit.SECONDS) || error("UI thread work didn't complete in 5 secs")
    exception?.let { throw it }
}

fun Activity.onNextFrame(block: () -> Unit) {
    uiThread {
        android.view.Choreographer.getInstance().postFrameCallback(
            object : android.view.Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    block()
                }
            })
    }
}