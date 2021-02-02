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

package androidx.compose.ui

import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.createAndroidComposeBenchmarkRunner
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.NumberFormat
import java.util.Locale

@LargeTest
@RunWith(AndroidJUnit4::class)
class MemoryLeakTest {

    @get:Rule
    @Suppress("DEPRECATION")
    val activityTestRule = androidx.test.rule.ActivityTestRule(ComponentActivity::class.java)

    @Test
    fun disposeAndRemoveOwnerView_assertViewWasGarbageCollected() = runBlocking {
        class SimpleTestCase : ComposeTestCase {
            @Composable
            override fun Content() {
                // The following line adds coverage for delayed coroutine memory leaks.
                LaunchedEffect(Unit) { delay(10000) }

                Column {
                    repeat(3) {
                        Box {
                            BasicText("Hello")
                        }
                    }
                }
            }
        }
        val testCaseFactory = { SimpleTestCase() }

        // NOTE: When fixing / debugging issues caused by this test it does not necessary has to be
        // a real memory leak. It can happen that you've scheduled resource clean up in a delayed
        // callback. But because we take over the main thread no callbacks get dispatched. This is
        // still issue for benchmarks though, as they need to fully occupy the main thread. You can
        // add check on main looper and perform clean asap if you are on main thread.
        withContext(AndroidUiDispatcher.Main) {
            val runner = createAndroidComposeBenchmarkRunner(
                testCaseFactory,
                activityTestRule.activity
            )

            // Unfortunately we have to ignore the first run as it seems that even though the view
            // gets properly garbage collected there are some data that remain allocated. Not sure
            // what is causing this but could be some static variables.
            loopAndVerifyMemory(iterations = 400, gcFrequency = 40, ignoreFirstRun = true) {
                try {
                    runner.createTestCase()
                    runner.emitContent()
                } finally {
                    // This will remove the owner view from the hierarchy
                    runner.disposeContent()
                }
            }
        }
    }

    @Test
    fun disposeContent_assertNoLeak() = runBlocking(AndroidUiDispatcher.Main) {
        // We have to ignore the first run because `dispose` leaves the OwnerView in the
        // View hierarchy to reuse it next time. That is probably not the final desired behavior
        val emptyView = View(activityTestRule.activity)
        loopAndVerifyMemory(iterations = 400, gcFrequency = 40) {
            activityTestRule.activity.setContent {
                Column {
                    repeat(3) {
                        Box {
                            BasicText("Hello")
                        }
                    }
                }
            }

            // This replaces the Compose view, disposing its composition.
            activityTestRule.activity.setContentView(emptyView)
        }
    }

    @Test
    fun memoryCheckerTest_noAllocationsExpected() = runBlocking {
        // This smoke test checks that we don't give false alert and run all the iterations
        var i = 0
        loopAndVerifyMemory(200, 10) {
            i++
        }
        assertThat(i).isEqualTo(200)
    }

    @Test(expected = AssertionError::class)
    fun memoryCheckerTest_errorExpected(): Unit = runBlocking {
        // This smoke test simulates memory leak and verifies that it was found
        val data = mutableListOf<IntArray>()
        loopAndVerifyMemory(10, 2) {
            val array = IntArray(256 * 1024) // Allocate 4 * 256 KiB => 1 MiB
            data.add(array)
        }

        // Just to avoid code being stripped away as unused.
        val totalSum = data.map { array -> array.sum() }.sum()
        Log.d("memoryCheckerTest", totalSum.toString())
    }

    /**
     * Runs the given code in a loop for exactly [iterations] times and every [gcFrequency] it will
     * force garbage collection and check the allocated heap size.
     * Suspending so that we can briefly yield() to the dispatcher before collecting garbage
     * so that event loop driven cleanup processes can run before we take measurements.
     */
    suspend fun loopAndVerifyMemory(
        iterations: Int,
        gcFrequency: Int,
        ignoreFirstRun: Boolean = false,
        operationToPerform: () -> Unit
    ) {
        val rawStats = ArrayList<Long>(iterations / gcFrequency)

        // Collect data
        repeat(iterations) { i ->
            if (i % gcFrequency == 0) {
                // Let any scheduled cleanup processes run before we take measurements
                yield()
                Runtime.getRuntime().let {
                    it.gc() // Run gc
                    rawStats.add(it.totalMemory() - it.freeMemory()) // Collect memory info
                }
            }
            operationToPerform()
        }

        fun Long.formatMemory(): String {
            return NumberFormat.getNumberInstance(Locale.US).format(this / 1024) + " KiB"
        }

        // Throw away the first run if needed
        val memoryStats = if (ignoreFirstRun) rawStats.drop(1) else rawStats
        val formattedStats = memoryStats.joinToString(", ") { it.formatMemory() }

        // Verify that memory did not grow
        val min = memoryStats.minOrNull()
        val max = memoryStats.maxOrNull()

        if (min == null || max == null) {
            throw AssertionError("Collected memory data are corrupted")
        }

        // Check if every iteration the memory grew => that's a bad sign
        val diffs = memoryStats
            .zipWithNext()
            .map { (it.second - it.first) / 1024 }
        val areAllDiffsGrowing = diffs.all { it > 0 }
        if (areAllDiffsGrowing) {
            throw AssertionError(
                "Possible memory leak detected!. Memory kept " +
                    "increasing every step. Min: ${min.formatMemory()}, max: " +
                    "${max.formatMemory()}\nData: [$formattedStats]"
            )
        }

        // Check if we have a significant diff across all the data
        val diff = max - min
        if (diff > 1024 * 1024) { // 1 MiB tolerance
            throw AssertionError(
                "Possible memory leak detected! Min: " +
                    "${min.formatMemory()}, max: ${max.formatMemory()}\n" +
                    "Data: [$formattedStats]"
            )
        }

        Log.i("MemoryTest", "Measured memory data: $formattedStats")
    }
}
