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

package androidx.compose.testutils

import android.view.View

/**
 * Test scope accessible from execution controlled tests to test compose.
 */
interface ComposeExecutionControl {
    /**
     * The measured width of the underlying view.
     */
    val measuredWidth: Int

    /**
     * The measured height of the underlying view.
     */
    val measuredHeight: Int

    /**
     * Performs measure.
     *
     * Note that this does not do any invalidation.
     */
    fun measure()

    /**
     * Performs layout.
     *
     * Note that this does not do any invalidation.
     */
    fun layout()

    /**
     * Performs full draw.
     *
     * Note that the performance is not close to real draw (unless running Q+).
     */
    fun drawToBitmap()

    /**
     * To be used for tests debugging.
     *
     * Draws the view under test into image view and places it in the current Activity. That will
     * also replace the current content under test. This can be useful to verify / preview results
     * of your time controlled tests.
     */
    fun capturePreviewPictureToActivity()

    /**
     * Whether the last frame / recompose had changes to recompose.
     */
    val didLastRecomposeHaveChanges: Boolean

    /**
     * Performs the full frame.
     *
     * This also sets up the content in case the content was not set up before.
     *
     * Following steps are performed
     * 1) Recompose
     * 2) Measure
     * 3) Layout
     * 4) Draw
     */
    fun doFrame()

    /**
     * Whether there are pending changes in the composition.
     */
    fun hasPendingChanges(): Boolean

    /**
     * Performs recomposition if needed.
     *
     * Note this is also called as part of [doFrame]
     */
    fun recompose()

    fun getHostView(): View
}

/**
 * Helper interface to run execution-controlled test via [ComposeTestRule].
 */
interface ComposeTestCaseSetup {
    /**
     * Takes the content provided via [ComposeTestRule#setContent] and runs the given test
     * instruction. The test is executed on the main thread and prevents interference from Activity
     * so the frames can be controlled manually. See [ComposeExecutionControl] for available
     * methods.
     */
    fun performTestWithEventsControl(block: ComposeExecutionControl.() -> Unit)
}

// Assertions

/**
 * Assert that the underlying view under test has a positive size.
 *
 * Useful to assert that the test case has some content.
 *
 * @throws AssertionError if the underlying view has zero measured size.
 */
fun ComposeExecutionControl.assertMeasureSizeIsPositive() {
    if (measuredWidth > 0 && measuredHeight > 0) {
        return
    }
    throw AssertionError("Measured size is not positive!")
}

/**
 * Asserts that last recomposition had some changes.
 */
fun ComposeExecutionControl.assertLastRecomposeHadChanges() {
    assertLastRecomposeResult(expectingChanges = true)
}

/**
 * Asserts that last recomposition had no some changes.
 */
fun ComposeExecutionControl.assertLastRecomposeHadNoChanges() {
    assertLastRecomposeResult(expectingChanges = false)
}

/**
 * Performs recomposition and asserts that there were or weren't pending changes based on
 * [expectingChanges].
 *
 * @throws AssertionError if condition not satisfied.
 */
private fun ComposeExecutionControl.assertLastRecomposeResult(expectingChanges: Boolean) {
    val message =
        if (expectingChanges) {
            "Expected pending changes on recomposition but there were none."
        } else {
            "Expected no pending changes on recomposition but there were some."
        }
    if (expectingChanges != didLastRecomposeHaveChanges) {
        throw AssertionError(message)
    }
}

/**
 * Performs recomposition and asserts that there were some pending changes.
 *
 * @throws AssertionError if last recomposition had no changes.
 */
fun ComposeExecutionControl.recomposeAssertHadChanges() {
    recompose()
    assertLastRecomposeHadChanges()
}

/**
 * Performs recomposition and asserts that there were some pending changes.
 *
 * @throws AssertionError if recomposition has pending changes.
 */
fun ComposeExecutionControl.assertNoPendingChanges() {
    if (hasPendingChanges()) {
        throw AssertionError("Expected no pending changes but there were some.")
    }
}

/**
 * Performs recomposition and asserts that there were some pending changes.
 *
 * @throws AssertionError if recomposition has no pending changes.
 */
fun ComposeExecutionControl.assertHasPendingChanges() {
    if (!hasPendingChanges()) {
        throw AssertionError("Expected pending changes but there were none.")
    }
}

// Assertions runners

/**
 * Performs the given amount of frames and asserts that there are no changes pending afterwards.
 * Also asserts that all the frames (except the last one) had changes to recompose.
 *
 * @throws AssertionError if any frame before [numberOfFramesToBeStable] frame had no pending
 * changes or the last frame had pending changes.
 */
fun ComposeExecutionControl.doFramesAssertAllHadChangesExceptLastOne(
    numberOfFramesToBeStable: Int
) {
    val framesDone = doFramesUntilNoChangesPending(numberOfFramesToBeStable)

    if (framesDone < numberOfFramesToBeStable) {
        throw AssertionError(
            "Hierarchy got stable in frame '$framesDone', which is before expected!"
        )
    }
}

// Runners

/**
 * Runs frames until there are no changes pending.
 *
 * @param maxAmountOfFrames Max amount of frames to perform before giving up and throwing exception.
 * @throws AssertionError if there are still pending changes after [maxAmountOfFrames] executed.
 */
fun ComposeExecutionControl.doFramesUntilNoChangesPending(maxAmountOfFrames: Int = 10): Int {
    var framesDone = 0
    while (framesDone < maxAmountOfFrames) {
        doFrame()
        framesDone++
        if (!hasPendingChanges()) {
            // We are stable!
            return framesDone
        }
    }

    // Still not stable
    throw AssertionError(
        "Changes are still pending after '$maxAmountOfFrames' " +
            "frames."
    )
}