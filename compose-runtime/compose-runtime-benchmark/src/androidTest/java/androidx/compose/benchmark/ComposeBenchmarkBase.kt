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

import android.app.Activity
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.Composable
import androidx.compose.Composer
import androidx.compose.Composition
import androidx.compose.FrameManager
import androidx.compose.Recomposer
import androidx.compose.currentComposer
import androidx.ui.core.AndroidOwner
import androidx.ui.core.setContent
import org.junit.Assert.assertTrue
import org.junit.Rule

abstract class ComposeBenchmarkBase {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(ComposeActivity::class.java)

    fun measureCompose(block: @Composable () -> Unit) {
        val activity = activityRule.activity
        var composition: Composition? = null
        benchmarkRule.measureRepeated {
            composition = activity.setContent(Recomposer.current(), block)

            // AndroidComposeView is postponing the composition till the saved state will be restored.
            // We will emulate the restoration of the empty state to trigger the real composition.
            val composeView = (findComposeView(activity) as ViewGroup?)!!
            composeView.restoreHierarchyState(SparseArray())

            runWithTimingDisabled {
                composition?.dispose()
            }
        }
        composition?.dispose()
    }

    fun measureRecompose(block: RecomposeReceiver.() -> Unit) {
        val receiver = RecomposeReceiver()
        receiver.block()
        var activeComposer: Composer<*>? = null

        val activity = activityRule.activity

        val composition = activity.setContent {
            activeComposer = currentComposer
            receiver.composeCb()
        }

        // AndroidOwner is postponing the composition till the saved state will be restored.
        // We will emulate the restoration of the empty state to trigger the real composition.
        val ownerView = findComposeView(activity)!!.view
        ownerView.restoreHierarchyState(SparseArray())

        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                receiver.updateModelCb()
                FrameManager.nextFrame()
            }

            val didSomething = activeComposer?.let { composer ->
                composer.recompose().also { composer.applyChanges() }
            } ?: false
            assertTrue(didSomething)
        }

        composition.dispose()
    }
}

class RecomposeReceiver {
    var composeCb: @Composable () -> Unit = @Composable { }
    var updateModelCb: () -> Unit = { }

    fun compose(block: @Composable () -> Unit) {
        composeCb = block
    }

    fun update(block: () -> Unit) {
        updateModelCb = block
    }
}

// TODO(chuckj): Consider refacgtoring to use AndroidTestCaseRunner from UI
// This code is copied from AndroidTestCaseRunner.kt
private fun findComposeView(activity: Activity): AndroidOwner? {
    return findComposeView(activity.findViewById(android.R.id.content) as ViewGroup)
}

private fun findComposeView(view: View): AndroidOwner? {
    if (view is AndroidOwner) {
        return view
    }

    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val composeView = findComposeView(view.getChildAt(i))
            if (composeView != null) {
                return composeView
            }
        }
    }
    return null
}