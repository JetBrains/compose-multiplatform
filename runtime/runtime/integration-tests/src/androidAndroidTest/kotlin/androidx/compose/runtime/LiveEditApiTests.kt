/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.material.Text
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

var someData = mutableStateOf(0)

@RunWith(AndroidJUnit4::class)
class LiveEditApiTests : BaseComposeTest() {
    @get:Rule
    override val activityRule = makeTestActivityRule()

    private fun invalidateGroup(key: Int) {
        invalidateGroupsWithKey(key)
    }

    // IMPORTANT: This must be the first test as the lambda key will change if the lambda is
    // moved this file.
    @Test
    @MediumTest
    fun forceRecompose_setContentLambda() {
        var setContentLambdaInvoked = 0
        val setContentLambdaKey = -1216916760 // Extracted from .class file (see note above)
        activity.show {
            Text("Some text")
            setContentLambdaInvoked++
        }
        activity.waitForAFrame()

        val setContentLambdaStart = setContentLambdaInvoked
        invalidateGroup(setContentLambdaKey)
        activity.waitForAFrame()

        assertTrue(
            "show's lambda should have been invoked",
            setContentLambdaInvoked > setContentLambdaStart
        )
    }

    @Test
    @MediumTest
    fun forceRecompose_Simple() {
        val activity = activityRule.activity
        activity.show {
            TestSimple()
        }
        activity.waitForAFrame()

        val someFunctionStart = someFunctionInvoked
        val nestedContentStart = nestedContentInvoked
        invalidateGroup(someFunctionKey)
        activity.waitForAFrame()

        assertTrue(
            "SomeFunction should have been invoked",
            someFunctionInvoked > someFunctionStart
        )
        assertTrue(
            "NestedContent should have been invoked",
            nestedContentInvoked > nestedContentStart
        )
    }

    @Test
    @MediumTest
    fun forceRecompose_NonRestartable() {
        val activity = activityRule.activity
        activity.show {
            TestNonRestartable()
        }
        activity.waitForAFrame()

        val nonRestartableStart = nonRestartableInvoked
        invalidateGroup(nonRestartableKey)

        activity.waitForAFrame()

        assertTrue(
            "NonRestartable should have been invoked",
            nonRestartableInvoked > nonRestartableStart
        )
    }

    @Test
    @MediumTest
    fun forceRecompose_ReadOnly() {
        activity.show { TestReadOnly() }
        activity.waitForAFrame()

        repeat(3) {
            val readOnlyStart = readOnlyInvoked
            invalidateGroup(readOnlyKey)
            activity.waitForAFrame()

            assertTrue(
                "ReadOnly should have been invoked, iteration $it",
                readOnlyInvoked > readOnlyStart
            )
        }
    }

    @Test
    @MediumTest
    fun forceRecompose_NonRestartableWrapper() {
        activity.show {
            TestNonRestartWrapper()
        }

        activity.waitForAFrame()

        // Ensure that scopes recomposable so the "shouldn't execute" checks below are correct
        invalidateGroup(nonRestartableKey)
        activity.waitForAFrame()

        // Invalidate restart
        run {
            val nonRestartableStart = nonRestartableInvoked
            val nonRestartWrapperStart = nonRestartWrapperInvoked
            invalidateGroup(nonRestartableKey)

            activity.waitForAFrame()

            assertTrue(
                "NonRestartable should have been invoked",
                nonRestartableInvoked > nonRestartableStart
            )
            assertTrue(
                "NonRestartWrapper invoked when it shouldn't have been",
                nonRestartWrapperStart == nonRestartWrapperInvoked
            )
        }

        // Invalidate the wrapper
        run {
            val nonRestartableStart = nonRestartableInvoked
            val nonRestartWrapperStart = nonRestartWrapperInvoked
            invalidateGroup(nonRestartWrapperKey)

            activity.waitForAFrame()

            assertTrue(
                "NonRestartable should have been invoked",
                nonRestartableInvoked > nonRestartableStart
            )
            assertTrue(
                "NonRestartWrapper should have been invoked",
                nonRestartWrapperInvoked > nonRestartWrapperStart
            )
        }
    }
}

const val someFunctionKey = -1580285603 // Extracted from .class file
var someFunctionInvoked = 0
@Composable
fun SomeFunction(a: Int) {
    Text("a = $a, someData = ${someData.value}")
    NestedContent()
    someFunctionInvoked++
}

const val nestedContentKey = 1771808426 // Extracted from .class file
var nestedContentInvoked = 0
@Composable
fun NestedContent() {
    Text("Some nested content: ${someData.value}")
    nestedContentInvoked++
}

const val nonRestartableKey = 1860384 // Extracted from .class file
var nonRestartableInvoked = 0
@Composable
@NonRestartableComposable
fun NonRestartable() {
    Text("Non restart")
    nonRestartableInvoked++
}

const val nonRestartWrapperKey = 1287143243 // Extracted from .class file
var nonRestartWrapperInvoked = 0
@Composable
@NonRestartableComposable
fun NonRestartWrapper(block: @Composable () -> Unit) {
    Text("Before")
    block()
    Text("After")
    nonRestartWrapperInvoked++
}

const val restartableWrapperKey = -153795690 // Extracted from .class file
var restartWrapperInvoked = 0
@Composable
fun RestartableWrapper(block: @Composable () -> Unit) {
    Text("Before")
    block()
    Text("After")
    restartWrapperInvoked++
}

const val readOnlyKey = -1414835162 // Extracted from .class file
var readOnlyInvoked = 0
@Composable
@ReadOnlyComposable
fun ReadOnly() {
    readOnlyInvoked++
}

// Test functions
@Composable
fun TestSimple() {
    Text("This is some text")
    SomeFunction(21)
}

@Composable
fun TestNonRestartable() {
    NonRestartable()
}

@Composable
fun TestNonRestartWrapper() {
    NonRestartWrapper {
        NonRestartable()
    }
    NestedContent()
}

@Composable
fun TestReadOnly() {
    ReadOnly()
}

@Composable
fun TestReadOnlyNested() {
    RestartableWrapper {
        ReadOnly()
    }
}