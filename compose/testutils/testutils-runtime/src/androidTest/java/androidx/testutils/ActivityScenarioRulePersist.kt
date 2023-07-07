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

import android.widget.TextView
import androidx.test.filters.LargeTest
import androidx.testutils.runtime.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

val ITERATIONS: List<Int> = (1..10).toList()

/**
 * Per-test overhead - ~400ms
 *
 * Default ActivityScenarioRule / ActivityTestRule - asserts activity re-launched for each test.
 */
@RunWith(Parameterized::class)
@LargeTest
class ActivityNoPersist(val index: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec() = ITERATIONS

        @BeforeClass
        @JvmStatic
        fun setup() {
            TestActivity.resumes = 1
        }
    }

    @get:Rule
    val activityRule = ResettableActivityScenarioRule(TestActivity::class.java)

    @Test
    fun test() {
        activityRule.scenario.onActivity {
            assertTrue(TestActivity.resumes >= index) // workaround setup ordering unpredictability
        }
    }
}

/**
 * Per-test overhead - ~5ms
 *
 * Using ActivityScenarioRule as a ClassRule - asserts activity only launched once.
 *
 * NOTE: Big downside is dangerous state sharing between tests...
 */
@RunWith(Parameterized::class)
@LargeTest
class ActivityPersist(@Suppress("unused") val ignored: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec() = ITERATIONS

        @BeforeClass
        @JvmStatic
        fun setup() {
            TestActivity.resumes = 1
        }

        @get:ClassRule
        @JvmStatic
        val activityRule = ResettableActivityScenarioRule(TestActivity::class.java)
    }

    @Test
    fun test() {
        activityRule.scenario.onActivity {
            assertTrue(TestActivity.resumes <= 2) // workaround setup ordering unpredictability
        }
    }
}

/**
 * Per-test overhead - ~5ms
 *
 * Using ActivityScenarioRule as a ClassRule - only launched once, and content view is shared...
 */
@RunWith(Parameterized::class)
@LargeTest
class ActivityPersistNoReset(val index: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec() = ITERATIONS

        @get:ClassRule
        @JvmStatic
        val activityRule = ResettableActivityScenarioRule(TestActivity::class.java)
    }

    @Test
    fun contentViewIsShared() {
        activityRule.scenario.onActivity {
            val text: TextView = it.findViewById(R.id.text)
            if (index != 1) {
                assertFalse(text.text.isBlank())
            }
            text.text = "$index"
        }
    }
}

/**
 * Per-test overhead - ~5ms
 *
 * Using ActivityScenarioRule as a ClassRule - only launched once, BUT RESETTING for each test.
 */
@RunWith(Parameterized::class)
@LargeTest
class ActivityPersistWithReset(val index: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec() = ITERATIONS

        @get:ClassRule
        @JvmStatic
        val activityRule = ResettableActivityScenarioRule(TestActivity::class.java)
    }

    @get:Rule
    val resetRule = ActivityScenarioResetRule(activityRule.scenario) {
        it.setContentView(R.layout.content_view)
    }

    @Test
    fun contentViewIsReplaced() {
        activityRule.scenario.onActivity {
            val text: TextView = it.findViewById(R.id.text)
            assertTrue(text.text.isBlank())
            text.text = "$index"
        }
    }
}