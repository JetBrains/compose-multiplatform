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
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import java.io.Closeable

/**
 * Run [block] using [ActivityScenario.onActivity], returning the result of the block.
 */
inline fun <reified A : Activity, T : Any> ActivityScenarioRule<A>.withActivity(
    crossinline block: A.() -> T
): T = scenario.withActivity(block)

/**
 * Run [block] using [ActivityScenario.onActivity], returning the result of the block.
 */
inline fun <reified A : Activity, T : Any> ActivityScenario<A>.withActivity(
    crossinline block: A.() -> T
): T {
    lateinit var value: T
    var err: Throwable? = null
    onActivity { activity ->
        try {
            value = block(activity)
        } catch (t: Throwable) {
            err = t
        }
    }
    err?.let { throw it }
    return value
}

/**
 * Run [block] in a [use] block when using [ActivityScenario.launch], rather
 * than just a [with] block to ensure the Activity is closed once test is complete.
 */
fun <C : Closeable> withUse(closeable: C, block: C.() -> Unit) {
    closeable.use {
        block(it)
    }
}