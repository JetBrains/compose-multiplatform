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
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement

@Suppress("unused")
open class ActivityRecyclingAndroidJUnitRunner : AndroidJUnitRunner() {
    override fun waitForActivitiesToComplete() {
    }
}

/**
 * Implement this interface on your Activity to allow HackyActivityScenarioRule to
 * launch once-per-test-class.
 */
interface Resettable {
    fun setFinishEnabled(finishEnabled: Boolean)
}

/**
 * Copy of ActivityScenarioRule, but which works around AndroidX test infra trying to finish
 * activities in between each test.
 */
class ResettableActivityScenarioRule<A> : ExternalResource where A : Activity, A : Resettable {
    private val scenarioSupplier: () -> ActivityScenario<A>
    private lateinit var _scenario: ActivityScenario<A>
    private var finishEnabled: Boolean = true
    private var initialTouchMode: Boolean = false

    val scenario: ActivityScenario<A>
        get() = _scenario

    override fun apply(base: Statement?, description: Description): Statement {
        // Running as a ClassRule? Disable activity finish
        finishEnabled = (description.methodName != null)
        return super.apply(base, description)
    }

    @JvmOverloads
    constructor(activityClass: Class<A>, initialTouchMode: Boolean = false) {
        this.initialTouchMode = initialTouchMode
        InstrumentationRegistry.getInstrumentation().setInTouchMode(initialTouchMode)
        scenarioSupplier = { ActivityScenario.launch(activityClass) }
    }

    @JvmOverloads
    constructor(startActivityIntent: Intent, initialTouchMode: Boolean = false) {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(initialTouchMode)
        scenarioSupplier = { ActivityScenario.launch(startActivityIntent) }
    }

    @Throws(Throwable::class)
    override fun before() {
        _scenario = scenarioSupplier.invoke()
        _activity = internalGetActivity()
        if (!finishEnabled) {
//             TODO: Correct approach inside test lib would be removing activity from cleanup list
            scenario.onActivity {
                it.setFinishEnabled(false)
            }
        }
    }

    override fun after() {
        if (!finishEnabled) {
            scenario.onActivity {
                it.setFinishEnabled(true)
            }
        }
        scenario.close()
        InstrumentationRegistry.getInstrumentation().setInTouchMode(initialTouchMode)
    }

    // Below are compat hacks to get RecyclerView ActivityTestRule tests up and running quickly

    fun runOnUiThread(runnable: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                runnable.run()
            }
        }
    }

    fun runOnUiThread(action: () -> Unit) {
        runOnUiThread(Runnable(action))
    }

    private fun internalGetActivity(): A {
        val activityReturn = mutableListOf<A?>(null)
        scenario.onActivity { activity -> activityReturn[0] = activity }
        return activityReturn[0]!!
    }

    private lateinit var _activity: A
    fun getActivity(): A {
        return _activity
    }
}

@Suppress("FunctionName") /* Acts as constructor */
inline fun <reified A> ResettableActivityScenarioRule(
    initialTouchMode: Boolean = false
): ResettableActivityScenarioRule<A> where A : Activity, A : Resettable {
    return ResettableActivityScenarioRule(A::class.java, initialTouchMode)
}