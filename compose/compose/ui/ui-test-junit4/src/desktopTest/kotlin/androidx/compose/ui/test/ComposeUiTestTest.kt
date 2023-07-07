/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.model.Statement

@RunWith(JUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ComposeUiTestTest {

    private lateinit var testDescription: Description

    /**
     * Records the current [testDescription] for tests that need to invoke the compose test rule
     * directly.
     */
    @get:Rule
    val testWatcher = object : TestWatcher() {
        override fun starting(description: Description) {
            testDescription = description
        }
    }

    // TODO why this test passed before the check for effectContext was added?
    //  effectContext didn't passed anywhere.
    @Test
    @Ignore("effectContext isn't implemented https://github.com/JetBrains/compose-multiplatform/issues/2960")
    fun effectContextPropagatedToComposition_runComposeUiTest() {
        val testElement = TestCoroutineContextElement()
        runComposeUiTest(effectContext = testElement) {
            lateinit var compositionScope: CoroutineScope
            setContent {
                compositionScope = rememberCoroutineScope()
            }

            runOnIdle {
                val elementFromComposition =
                    compositionScope.coroutineContext[TestCoroutineContextElement]
                assertThat(elementFromComposition).isSameInstanceAs(testElement)
            }
        }
    }

    // TODO why this test passed before the check for effectContext was added?
    //  effectContext didn't passed anywhere.
    @Test
    @Ignore("effectContext isn't implemented https://github.com/JetBrains/compose-multiplatform/issues/2960")
    fun effectContextPropagatedToComposition_createComposeRule() {
        val testElement = TestCoroutineContextElement()
        lateinit var compositionScope: CoroutineScope
        val rule = createComposeRule(testElement)
        val baseStatement = object : Statement() {
            override fun evaluate() {
                rule.setContent {
                    compositionScope = rememberCoroutineScope()
                }
                rule.waitForIdle()
            }
        }
        rule.apply(baseStatement, testDescription)
            .evaluate()

        val elementFromComposition =
            compositionScope.coroutineContext[TestCoroutineContextElement]
        assertThat(elementFromComposition).isSameInstanceAs(testElement)
    }

    @Test
    fun motionDurationScale_defaultValue() = runComposeUiTest {
        var lastRecordedMotionDurationScale: Float? = null
        setContent {
            val context = rememberCoroutineScope().coroutineContext
            lastRecordedMotionDurationScale = context[MotionDurationScale]?.scaleFactor
        }

        runOnIdle {
            assertThat(lastRecordedMotionDurationScale).isNull()
        }
    }

    // TODO why this test passed before the check for effectContext was added?
    //  effectContext didn't passed anywhere.
    @Test
    @Ignore("effectContext isn't implemented https://github.com/JetBrains/compose-multiplatform/issues/2960")
    fun motionDurationScale_propagatedToCoroutines() {
        val motionDurationScale = object : MotionDurationScale {
            override val scaleFactor: Float get() = 0f
        }
        runComposeUiTest(effectContext = motionDurationScale) {
            var lastRecordedMotionDurationScale: Float? = null
            setContent {
                val context = rememberCoroutineScope().coroutineContext
                lastRecordedMotionDurationScale = context[MotionDurationScale]?.scaleFactor
            }

            runOnIdle {
                assertThat(lastRecordedMotionDurationScale).isEqualTo(0f)
            }
        }
    }

    private class TestCoroutineContextElement : CoroutineContext.Element {
        override val key: CoroutineContext.Key<*> get() = Key

        companion object Key : CoroutineContext.Key<TestCoroutineContextElement>
    }
}