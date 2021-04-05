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
import androidx.compose.testutils.ComposeBenchmarkScope
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.android.AndroidTestCase
import androidx.compose.testutils.createAndroidComposeBenchmarkRunner
import androidx.compose.ui.test.InternalTestApi
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Rule to be used to run Compose / Android benchmarks.
 */
class ComposeBenchmarkRule : TestRule {
    @Suppress("DEPRECATION")
    private val activityTestRule =
        androidx.test.rule.ActivityTestRule(ComponentActivity::class.java)

    val benchmarkRule = BenchmarkRule()

    override fun apply(base: Statement, description: Description?): Statement {
        @OptIn(InternalTestApi::class)
        return RuleChain
            .outerRule(benchmarkRule)
            .around(activityTestRule)
            .apply(base, description)
    }

    /**
     * Runs benchmark for the given [ComposeTestCase].
     *
     * Note that benchmark by default runs on the ui thread and disposes composition afterwards.
     *
     * @param givenTestCase The test case to be executed
     * @param block The benchmark instruction to be performed over the given test case
     */
    fun <T : ComposeTestCase> runBenchmarkFor(
        givenTestCase: () -> T,
        block: ComposeBenchmarkScope<T>.() -> Unit
    ) {
        require(givenTestCase !is AndroidTestCase) {
            "Expected ${ComposeTestCase::class.simpleName}!"
        }

        activityTestRule.runOnUiThread {
            // TODO(pavlis): Assert that there is no existing composition before we run benchmark
            val runner = createAndroidComposeBenchmarkRunner(
                givenTestCase,
                activityTestRule.activity
            )
            try {
                block(runner)
            } finally {
                runner.disposeContent()
            }
        }
    }

    /**
     * Convenience proxy for [BenchmarkRule.measureRepeated].
     */
    fun measureRepeated(block: BenchmarkRule.Scope.() -> Unit) {
        benchmarkRule.measureRepeated(block)
    }

    /**
     * Convenience proxy for `ActivityTestRule.runOnUiThread`
     */
    fun runOnUiThread(block: () -> Unit) {
        activityTestRule.runOnUiThread(block)
    }
}