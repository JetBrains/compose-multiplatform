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

package androidx.compose.ui.graphics.benchmark

import androidx.benchmark.junit4.PerfettoRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Duplicate of [VectorBenchmark], but which adds tracing.
 *
 * Note: Per PerfettoRule, these benchmarks will be ignored < API 29
 */
@Suppress("ClassName")
@LargeTest
@RunWith(AndroidJUnit4::class)
class VectorBenchmarkWithTracing : VectorBenchmark() {
    @get:Rule
    val perfettoRule = PerfettoRule()
}