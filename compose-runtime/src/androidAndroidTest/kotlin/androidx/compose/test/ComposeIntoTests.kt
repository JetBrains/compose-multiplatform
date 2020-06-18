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

package androidx.compose.test

import android.os.HandlerThread
import androidx.compose.Composable
import androidx.compose.ExperimentalComposeApi
import androidx.compose.FrameManager
import androidx.compose.Handler
import androidx.compose.Untracked
import androidx.compose.clearRoots
import androidx.compose.mutableStateOf
import androidx.compose.onActive
import androidx.compose.onCommit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ComposeIntoTests : BaseComposeTest() {
    @After
    fun teardown() {
        clearRoots()
    }

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    @MediumTest
    fun testMultipleSetContentCalls() {
        val activity = activityRule.activity

        var initializationCount = 0
        var commitCount = 0
        @OptIn(ExperimentalComposeApi::class)
        val composable = @Composable @Untracked {
            onActive { initializationCount++ }
            onCommit { commitCount++ }
        }

        activity.show(composable)
        activity.waitForAFrame()

        assertEquals(1, initializationCount)
        assertEquals(1, commitCount)

        activity.show(composable)
        activity.waitForAFrame()

        // if we call setContent multiple times, we want to ensure that it doesn't tear
        // down the whole hierarchy, so onActive should only get called once.
        assertEquals(1, initializationCount)
        assertEquals(2, commitCount)
    }

    @Test // b/153355487
    @MediumTest
    fun testCommittingFromASeparateThread() {
        val model = mutableStateOf(0)
        var composed = 0
        var compositionLatch = CountDownLatch(1)
        val threadLatch = CountDownLatch(1)
        val composition = activity.show {
            composed = model.value
            compositionLatch.countDown()
        }
        try {
            compositionLatch.wait()
            val thread = HandlerThread("")
            thread.start()
            Handler(thread.looper).post {
                FrameManager.framed {
                    model.value = 1
                }
                threadLatch.countDown()
            }
            compositionLatch = CountDownLatch(1)
            threadLatch.wait()
            compositionLatch.wait()
            assertEquals(1, composed)
        } finally {
            activity.runOnUiThread { composition.dispose() }
        }
    }
}

fun CountDownLatch.wait() = assertTrue(await(1, TimeUnit.SECONDS))