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

package androidx.compose.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.Composable
import androidx.compose.Composer
import androidx.compose.FrameManager
import androidx.compose.composer
import androidx.compose.disposeComposition
import androidx.compose.runWithCurrent
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.setContent
import org.junit.Assert.assertTrue
import org.junit.Rule

abstract class ComposeBenchmarkBase {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(ComposeActivity::class.java)

    fun measureCompose(block: @Composable() () -> Unit) {
        val activity = activityRule.activity
        benchmarkRule.measureRepeated {
            activity.setContent(block)

            runWithTimingDisabled {
                activity.setContent { }
            }
        }
        activity.disposeComposition()
    }

    fun measureRecompose(block: RecomposeReceiver.() -> Unit) {
        val receiver = RecomposeReceiver()
        receiver.block()
        var activeComposer: Composer<*>? = null

        val activity = activityRule.activity

        activity.setContent {
            activeComposer = composer
            receiver.composeCb()
        }

        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                receiver.updateModelCb()
                FrameManager.nextFrame()
            }

            val didSomething = activeComposer?.let { composer ->
                composer.runWithCurrent {
                    composer.recompose().also { composer.applyChanges() }
                }
            } ?: false
            assertTrue(didSomething)
        }

        activity.disposeComposition()
    }
}

class RecomposeReceiver {
    var composeCb: @Composable() () -> Unit = @Composable { }
    var updateModelCb: () -> Unit = { }

    fun compose(block: @Composable() () -> Unit) {
        composeCb = block
    }

    fun update(block: () -> Unit) {
        updateModelCb = block
    }
}
