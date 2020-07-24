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

package androidx.compose.runtime.benchmark

import android.os.Handler
import android.os.Looper
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.random.Random

@LargeTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalComposeApi::class)
class SnapshotStateObserverBenchmark(
    private val numberOfModels: Int,
    private val numberOfNodes: Int
) : ComposeBenchmarkBase() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "states = {0}, nodes = {1}")
        fun initParameters(): Array<Any> = arrayOf(
            arrayOf(1000, 1000),
            arrayOf(10000, 100),
            arrayOf(100000, 10),
            arrayOf(100, 1000)
        )
    }

    private val doNothing: (Int) -> Unit = { _ -> }

    private lateinit var stateObserver: SnapshotStateObserver
    private val models = List(numberOfModels) { mutableStateOf(0) }
    private val nodes = List(numberOfNodes) { it }
    private lateinit var random: Random
    private val numObservations = numberOfModels / 10

    @Before
    fun setup() {
        random = Random(0)
        runOnUiThread {
            val handler = Handler(Looper.getMainLooper())
            stateObserver = SnapshotStateObserver { command ->
                if (Looper.myLooper() !== handler.looper) {
                    handler.post(command)
                } else {
                    command()
                }
            }
        }
        stateObserver.enableStateUpdatesObserving(true)
        setupObservations()
        Snapshot.sendApplyNotifications()
    }

    @After
    fun teardown() {
        runOnUiThread {
            stateObserver.enableStateUpdatesObserving(false)
        }
    }

    @Test
    fun modelObservation() {
        runOnUiThread {
            benchmarkRule.measureRepeated {
                random = Random(0)
                val node = nodes[random.nextInt(numberOfNodes)]
                observeForNode(node)
            }
        }
    }

    @Test
    fun nestedModelObservation() {
        runOnUiThread {
            stateObserver.observeReads(nodes[0], doNothing) {
                benchmarkRule.measureRepeated {
                    random = Random(0)
                    val node = nodes[random.nextInt(numberOfNodes)]
                    observeForNode(node)
                }
            }
        }
    }

    @Test
    fun modelClear() {
        runOnUiThread {
            benchmarkRule.measureRepeated {
                random = Random(0)
                val node = nodes[random.nextInt(numberOfNodes)]
                stateObserver.clear(node)
                runWithTimingDisabled {
                    observeForNode(node)
                }
            }
        }
    }

    private fun runOnUiThread(block: () -> Unit) = activityRule.runOnUiThread(block)
    private fun setupObservations() = nodes.forEach { observeForNode(it) }

    private fun observeForNode(node: Int) {
        stateObserver.observeReads(node, doNothing) {
            repeat(numObservations) {
                // just access the value
                models[random.nextInt(numberOfModels)].value
            }
        }
    }
}
