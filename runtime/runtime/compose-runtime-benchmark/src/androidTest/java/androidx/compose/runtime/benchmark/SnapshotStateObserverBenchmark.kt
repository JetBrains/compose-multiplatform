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
import org.junit.runners.JUnit4
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

@LargeTest
@RunWith(JUnit4::class)
@OptIn(ExperimentalComposeApi::class)
class SnapshotStateObserverBenchmark : ComposeBenchmarkBase() {
    companion object {
        private const val ScopeCount = 1000
        private const val StateCount = 1000
    }

    private val doNothing: (Int) -> Unit = { _ -> }

    private lateinit var stateObserver: SnapshotStateObserver
    private val models = List(StateCount) { mutableStateOf(0) }
    private val nodes = List(ScopeCount) { it }
    private lateinit var random: Random

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
                runWithTimingDisabled {
                    nodes.forEach { node ->
                        stateObserver.clear(node)
                    }
                    random = Random(0)
                }
                setupObservations()
            }
        }
    }

    @Test
    fun nestedModelObservation() {
        runOnUiThread {
            val list = mutableListOf<Int>()
            repeat(10) {
                list += nodes[random.nextInt(ScopeCount)]
            }
            benchmarkRule.measureRepeated {
                runWithTimingDisabled {
                    random = Random(0)
                    nodes.forEach { node ->
                        stateObserver.clear(node)
                    }
                }
                stateObserver.observeReads(nodes[0], doNothing) {
                    list.forEach { node ->
                        observeForNode(node)
                    }
                }
            }
        }
    }

    @Test
    fun modelClear() {
        runOnUiThread {
            benchmarkRule.measureRepeated {
                nodes.forEach { node ->
                    stateObserver.clear(node)
                }
                random = Random(0)
                runWithTimingDisabled {
                    setupObservations()
                }
            }
        }
    }

    @Test
    fun notifyChanges() {
        runOnUiThread {
            val states = mutableSetOf<Int>()
            repeat(50) {
                states += random.nextInt(StateCount)
            }
            val snapshot: Snapshot = Snapshot.current
            benchmarkRule.measureRepeated {
                random = Random(0)
                stateObserver.notifyChanges(states, snapshot)
            }
        }
    }

    private fun runOnUiThread(block: () -> Unit) = activityRule.runOnUiThread(block)
    private fun setupObservations() = nodes.forEach { observeForNode(it) }

    private fun observeForNode(node: Int) {
        stateObserver.observeReads(node, doNothing) {
            // we want between 0-10, with the cluster near 0, but some outliers
            val numObservations = (10.0.pow(random.nextDouble(2.0)) / 10).roundToInt()
            repeat(numObservations) {
                // just access the value
                models[random.nextInt(StateCount)].value
            }
        }
    }
}
