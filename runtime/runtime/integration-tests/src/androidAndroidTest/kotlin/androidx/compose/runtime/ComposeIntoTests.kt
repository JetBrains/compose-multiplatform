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

import android.os.HandlerThread
import android.view.View
import androidx.core.os.HandlerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ComposeIntoTests : BaseComposeTest() {
    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    @MediumTest
    fun testMultipleSetContentCalls() {
        val activity = activityRule.activity

        var initializationCount = 0
        val composable = @Composable {
            DisposableEffect(Unit) {
                initializationCount++
                onDispose { }
            }
        }

        activity.show(composable)
        activity.waitForAFrame()

        assertEquals(1, initializationCount)

        activity.show(composable)
        activity.waitForAFrame()

        // if we call setContent multiple times, we want to ensure that it doesn't tear
        // down the whole hierarchy, so onActive should only get called once.
        assertEquals(1, initializationCount)
    }

    @Test // b/153355487
    @MediumTest
    fun testCommittingFromASeparateThread() {
        val model = mutableStateOf(0)
        var composed = 0
        var compositionLatch = CountDownLatch(1)
        val threadLatch = CountDownLatch(1)
        activity.show {
            composed = model.value
            compositionLatch.countDown()
        }
        val thread = HandlerThread("")
        try {
            compositionLatch.wait()
            thread.start()
            HandlerCompat.createAsync(thread.looper).post {
                model.value = 1
                threadLatch.countDown()
            }
            compositionLatch = CountDownLatch(1)
            threadLatch.wait()
            compositionLatch.wait()
            assertEquals(1, composed)
        } finally {
            activity.runOnUiThread {
                activity.setContentView(View(activity))
            }
            thread.quitSafely()
        }
    }

    @Test
    @MediumTest
    fun testCompositionCanBeCollectedWithPendingInvalidate() {
        val referenceQueue = ReferenceQueue<Composer>()
        var scope: RecomposeScope? = null
        var composer: Composer? = null
        var phantomReference: PhantomReference<Composer>? = null
        fun doShow() {
            val threadLatch = CountDownLatch(1)
            activity.show {
                composer = currentComposer
                scope = currentRecomposeScope
                threadLatch.countDown()
            }
            threadLatch.wait()
            phantomReference = PhantomReference<Composer>(composer, referenceQueue)
        }

        doShow()
        assertNotNull(scope)

        val threadLatch = CountDownLatch(1)
        activity.runOnUiThread {
            activity.setContentView(View(activity))
            composer = null
            threadLatch.countDown()
        }
        threadLatch.wait()
        assertNull(composer)
        assertNotNull(phantomReference)

        // Make sure that the composer was collected.
        repeat(100) {
            Runtime.getRuntime().gc()
            val value = referenceQueue.poll()
            if (value != null) return
        }
        error("Failed - composer leaked")
    }
}

fun CountDownLatch.wait() = assertTrue(await(1, TimeUnit.SECONDS))