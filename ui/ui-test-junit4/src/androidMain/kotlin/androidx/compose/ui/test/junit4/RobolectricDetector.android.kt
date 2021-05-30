/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.test.junit4

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.Description
import org.junit.runner.RunWith

internal object RobolectricDetector {

    private val RobolectricTestRunnerClassName = "org.robolectric.RobolectricTestRunner"
    private val RobolectricParameterizedTestRunnerClassName =
        "org.robolectric.ParameterizedRobolectricTestRunner"
    private val AndroidJUnit4ClassName =
        "androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner"

    private val RobolectricTestRunners = listOf(
        RobolectricTestRunnerClassName,
        RobolectricParameterizedTestRunnerClassName
    ).mapNotNull {
        try {
            Class.forName(it)
        } catch (_: ClassNotFoundException) {
            null
        }
    }

    internal fun usesRobolectricTestRunner(description: Description): Boolean {
        val testRunner = description.testClass?.getAnnotation(RunWith::class.java)?.value?.java
            ?: return false
        return testRunner.isRobolectricRunner || testRunner.usesRobolectricRunner
    }

    /**
     * Returns if this test runner delegates to a Robolectric test runner. The AndroidJUnit4 test
     * runner instantiates a delegate test runner, which is the actual test runner. It is either
     * a Robolectric test runner, or something else.
     */
    private val Class<*>.usesRobolectricRunner: Boolean
        get() = isAndroidJUnit4Runner && getDelegateTestRunner().isRobolectricRunner

    private val Class<*>.isRobolectricRunner: Boolean
        get() = RobolectricTestRunners.any { it.isAssignableFrom(this) }

    private val Class<*>.isAndroidJUnit4Runner: Boolean
        get() = AndroidJUnit4::class.java.isAssignableFrom(this)

    /**
     * Returns the delegate test runner used by the AndroidJUnit4 test runner. This uses the
     * exact same method to resolve the class as AndroidJUnit4 does.
     */
    private fun getDelegateTestRunner(): Class<*> {
        val delegateRunner = System.getProperty("android.junit.runner", null)
            ?: if (hasRobolectricOnHost()) {
                RobolectricTestRunnerClassName
            } else {
                AndroidJUnit4ClassName
            }
        // The runner has already been instantiated, so this should succeed
        return Class.forName(delegateRunner)
    }

    /**
     * Returns if RobolectricTestRunner is on the classpath, _and_ if we're running on the host.
     * This is the same detection method as used in AndroidJUnit4.
     */
    private fun hasRobolectricOnHost(): Boolean {
        val isAndroidRuntime =
            System.getProperty("java.runtime.name")?.lowercase()?.contains("android") ?: false
        return !isAndroidRuntime && try {
            // Load RobolectricTestRunner. If it succeeds, it's on the classpath
            Class.forName(RobolectricTestRunnerClassName); true
        } catch (e: ClassNotFoundException) {
            // If it fails, it's not on the classpath
            false
        }
    }
}
