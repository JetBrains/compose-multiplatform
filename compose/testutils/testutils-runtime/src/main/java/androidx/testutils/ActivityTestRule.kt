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

package androidx.testutils

import android.app.Activity
import android.os.Looper

/**
 * Wait for execution, by default waiting 2 cycles to ensure that posted transitions are
 * executed and have had a chance to run.
 */
@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<out Activity>.waitForExecution(cycles: Int = 2) {
    // Wait for two cycles. When starting a postponed transition, it will post to
    // the UI thread and then the execution will be added onto the queue after that.
    // The two-cycle wait makes sure fragments have the opportunity to complete both
    // before returning.
    try {
        for (i in 0 until cycles) {
            runOnUiThreadRethrow {}
        }
    } catch (throwable: Throwable) {
        throw RuntimeException(throwable)
    }
}

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<out Activity>.runOnUiThreadRethrow(block: () -> Unit) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        block()
    } else {
        try {
            runOnUiThread {
                block()
            }
        } catch (t: Throwable) {
            throw RuntimeException(t)
        }
    }
}
