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

package androidx.ui.test

/**
 * Test scope accessible from benchmarks. Provides extended set of hooks for compose benchmarking.
 */
interface ComposeBenchmarkScope<T> : ComposeExecutionControl {
    /**
     * Instantiates the current [ComposeTestCase] in order to perform benchmarks on it.
     *
     * This is normally executed as part of [setupContent].
     *
     * Do not measure this in benchmark.
     */
    fun createTestCase()

    /**
     * Emits the content. This and [createTestCase] are by default called by first invocation of
     * doFrame. However this is useful in case you need to benchmark the first composition.
     *
     * If you want to call this multiple times, make sure you call [disposeContent] and
     * [createTestCase] in between. If you don't need to measure this but need to setup the
     * content consider using [setupContent].
     */
    fun emitContent()

    /**
     * Request layout on the underlying view. This is should typically not be needed if your
     * changes invalidate layout by default.
     */
    fun requestLayout()

    /**
     * Invalidates the view / compose hierarchy. This is should typically not be needed if your
     * changes invalidate view by default.
     */
    fun invalidateViews()

    /**
     * Preparation for [draw]. Do not measure this in benchmark.
     */
    fun drawPrepare()

    /**
     * To be run in benchmark. Call [drawPrepare] before and [drawFinish] after.
     */
    fun draw()

    /**
     * Final step after [draw]. Do not measure this in benchmark.
     */
    fun drawFinish()

    /**
     * Calls measureWithSpec on the underlying view.
     */
    // TODO(b/143754545): Try to remove this.
    fun measureWithSpec(widthSpec: Int, heightSpec: Int)

    /**
     * Disposes the content and destroys the current [ComposeTestCase].
     *
     * This is typically needed when benchmarking the first content setup or composition.
     */
    fun disposeContent()

    /**
     * Returns the current instantiated [ComposeTestCase].
     *
     * Make sure you called [setupContent] or [createTestCase] before calling this.
     */
    fun getTestCase(): T
}

/**
 * Sets up the content. This is by default called by first invocation of
 * [ComposeBenchmarkScope.doFrame].
 *
 * If you want to call this multiple times, make sure you call
 * [ComposeBenchmarkScope.disposeContent] in between.
 */
fun <T> ComposeBenchmarkScope<T>.setupContent() {
    createTestCase()
    emitContent()
}