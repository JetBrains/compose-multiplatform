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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [ProduceStateDetector].
 */
class ProduceStateDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ProduceStateDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ProduceStateDetector.ProduceStateDoesNotAssignValue)

    @Test
    fun errors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                @Composable
                fun Test() {
                    produceState(true, true) {
                        // Reading, not assigning the value, so this should be an error
                        val foo = value
                    }
                    produceState(true, true) {
                        // This method is a member of ProduceStateScope, so we know that it isn't
                        // going to assign value for us
                        awaitDispose {  }
                    }
                    produceState(true, true) {
                        // Receiver type of State, so assigning value is not possible
                        doSomethingWithState()
                    }
                    produceState(true, true) {
                        // Parameter type of State, so assigning value is not possible
                        doSomethingElseWithState(this)
                    }
                }

                fun <T> State<T>.doSomethingWithState() {}

                fun <T> doSomethingElseWithState(state: State<T>) {}
            """
            ),
            Stubs.Composable,
            Stubs.SnapshotState
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:8: Error: produceState calls should assign value inside the producer lambda [ProduceStateDoesNotAssignValue]
                    produceState(true, true) {
                    ~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:12: Error: produceState calls should assign value inside the producer lambda [ProduceStateDoesNotAssignValue]
                    produceState(true, true) {
                    ~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:17: Error: produceState calls should assign value inside the producer lambda [ProduceStateDoesNotAssignValue]
                    produceState(true, true) {
                    ~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:21: Error: produceState calls should assign value inside the producer lambda [ProduceStateDoesNotAssignValue]
                    produceState(true, true) {
                    ~~~~~~~~~~~~
4 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                @Composable
                fun Test() {
                    produceState(true, true) {
                        value = true
                    }
                    produceState(true, true) {
                        this.value = true
                    }
                    produceState(true, true) {
                        doSomethingWithScope()
                    }
                    produceState(true, true) {
                        doSomethingElseWithScope(this)
                    }
                    produceState(true, true) {
                        doSomethingWithState()
                    }
                    produceState(true, true) {
                        doSomethingElseWithState(this)
                    }
                }

                fun <T> MutableState<T>.doSomethingWithState() {}

                fun <T> doSomethingElseWithState(state: MutableState<T>) {}

                fun <T> ProduceStateScope<T>.doSomethingWithScope() {}

                fun <T> doSomethingElseWithScope(scope: ProduceStateScope<T>) {}
            """
            ),
            Stubs.Composable,
            Stubs.SnapshotState
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
