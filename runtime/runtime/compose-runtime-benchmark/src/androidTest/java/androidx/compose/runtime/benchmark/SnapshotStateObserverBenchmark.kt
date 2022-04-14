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

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import org.junit.Assume.assumeTrue

@LargeTest
@RunWith(AndroidJUnit4::class)
class SnapshotStateObserverBenchmark : ComposeBenchmarkBase() {
    companion object {
        private const val ScopeCount = 1000
        private const val StateCount = 1000
    }

    private val doNothing: (Any) -> Unit = { _ -> }

    private lateinit var stateObserver: SnapshotStateObserver
    private val models: List<MutableState<Any>> = List(StateCount) { mutableStateOf(0) }
    private val nodes: List<Any> = List(ScopeCount) { it }
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
        stateObserver.start()
        setupObservations()
        Snapshot.sendApplyNotifications()
    }

    @After
    fun teardown() {
        runOnUiThread {
            stateObserver.stop()
        }
    }

    @Test
    fun modelObservation() {
        assumeTrue(Build.VERSION.SDK_INT != 29)
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
        assumeTrue(Build.VERSION.SDK_INT != 29)
        runOnUiThread {
            val list = mutableListOf<Any>()
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
        assumeTrue(Build.VERSION.SDK_INT != 29)
        runOnUiThread {
            val nodeSet = hashSetOf<Any>()
            nodeSet.addAll(nodes)

            benchmarkRule.measureRepeated {
                stateObserver.clearIf { node ->
                    node in nodeSet
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
        assumeTrue(Build.VERSION.SDK_INT != 29)
        runOnUiThread {
            val states = mutableSetOf<Int>()
            repeat(50) {
                states += random.nextInt(StateCount)
            }
            val snapshot: Snapshot = Snapshot.current
            benchmarkRule.measureRepeated {
                random = Random(0)
                stateObserver.notifyChanges(states, snapshot)
                runWithTimingDisabled {
                    stateObserver.clear()
                    setupObservations()
                }
            }
        }
    }

    private fun runOnUiThread(block: () -> Unit) = activityRule.runOnUiThread(block)
    private fun setupObservations() = nodes.forEach { observeForNode(it) }

    private fun observeForNode(node: Any) {
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
