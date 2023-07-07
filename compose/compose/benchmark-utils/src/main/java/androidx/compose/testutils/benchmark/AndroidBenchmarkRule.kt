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

package androidx.compose.testutils.benchmark

import androidx.activity.ComponentActivity
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.android.AndroidTestCase
import androidx.compose.testutils.benchmark.android.AndroidTestCaseRunner
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Rule to be used to run Android benchmarks.
 */
class AndroidBenchmarkRule : TestRule {

    @Suppress("DEPRECATION")
    private val activityTestRule =
        androidx.test.rule.ActivityTestRule<ComponentActivity>(
            ComponentActivity::class.java
        )

    val benchmarkRule = BenchmarkRule()

    override fun apply(base: Statement, description: Description?): Statement {
        return benchmarkRule.apply(
            activityTestRule.apply(base, description), description!!
        )
    }

    /**
     * Runs benchmark for the given [AndroidTestCase].
     *
     * Note that benchmark by default runs on the ui thread.
     *
     * @param givenTestCase The test case to be executed
     * @param block The benchmark instruction to be performed over the given test case
     */
    fun <T : AndroidTestCase> runBenchmarkFor(
        givenTestCase: () -> T,
        block: AndroidTestCaseRunner<T>.() -> Unit
    ) {
        require(givenTestCase !is ComposeTestCase) {
            "Expected ${AndroidTestCase::class.simpleName}!"
        }

        activityTestRule.runOnUiThread {
            val runner =
                AndroidTestCaseRunner(
                    givenTestCase,
                    activityTestRule.activity
                )
            block(runner)
        }
    }

    /**
     * Convenience proxy for [BenchmarkRule.measureRepeated].
     */
    fun measureRepeated(block: BenchmarkRule.Scope.() -> Unit) {
        benchmarkRule.measureRepeated(block)
    }
}