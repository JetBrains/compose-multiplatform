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
import org.junit.rules.ExternalResource

/**
 * Use this Rule in conjunction with a [ResettableActivityScenarioRule] to launch one Activity per
 * test class.
 *
 * Example usage:
 * ```
 * @RunWith(AndroidJUnit4::class)
 * @LargeTest
 * class ActivityPersistWithReset() {
 *     companion object {
 *         @get:ClassRule
 *         @JvmStatic
 *         val activityRule = ResettableActivityScenarioRule(TestActivity::class.java)
 *     }
 *
 *     @get:Rule
 *     val resetRule = ActivityScenarioResetRule(activityRule.scenario) {
 *         it.setContentView(R.layout.content_view)
 *     }
 *
 *     @Test
 *     fun test1() {
 *         activityRule.scenario.onActivity {
 *             ...
 *         }
 *     }
 *
 *     @Test
 *     fun test2() {
 *         activityRule.scenario.onActivity {
 *             ...
 *         }
 *     }
 *     ...
 * }
 * ```
 */
open class ActivityScenarioResetRule<A : Activity>(
    private val scenario: ActivityScenario<A>,
    private val predicate: (A) -> Unit
) : ExternalResource() {
    override fun before() {
        super.before()
        scenario.onActivity {
            predicate.invoke(it)
        }
        // reset has likely modified activity state, so allow state (e.g. layout/measure) to resolve
        scenario.onActivity {}
    }

    override fun after() {
        // TODO: validate activity hasn't been modified from launch state.

        // If using this reset rule, it's invalid for the activity to not be resumed, for keyguard
        // to be left up, dialog left open, etc., so we can validate that hasn't happened here.
        super.after()
    }
}