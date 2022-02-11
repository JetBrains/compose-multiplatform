/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.benchmark

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@LargeTest
@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
class LifecycleAwareWindowRecomposerBenchmark {

    @get:Rule
    val rule = CombinedActivityBenchmarkRule()

    @Test
    @UiThreadTest
    fun createRecomposer() {
        val rootView = rule.activityTestRule.activity.window.decorView.rootView
        val lifecycle = object : Lifecycle() {
            override fun addObserver(observer: LifecycleObserver) {
                if (observer is LifecycleEventObserver) {
                    observer.onStateChanged({ this }, Event.ON_CREATE)
                }
            }

            override fun removeObserver(observer: LifecycleObserver) {}
            override fun getCurrentState(): State = State.CREATED
        }
        var view: View? = null
        rule.benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                view = View(rule.activityTestRule.activity)
                (rootView as ViewGroup).addView(view)
            }
            view!!.createLifecycleAwareWindowRecomposer(lifecycle = lifecycle)
            runWithTimingDisabled {
                (rootView as ViewGroup).removeAllViews()
                view = null
            }
        }
    }

    class CombinedActivityBenchmarkRule() : TestRule {
        @Suppress("DEPRECATION")
        val activityTestRule =
            androidx.test.rule.ActivityTestRule(ComponentActivity::class.java)

        val benchmarkRule = BenchmarkRule()

        override fun apply(base: Statement, description: Description?): Statement {
            return RuleChain.outerRule(benchmarkRule)
                .around(activityTestRule)
                .apply(base, description)
        }
    }
}