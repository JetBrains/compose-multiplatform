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

package androidx.compose.material

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.testutils.forGivenTestCase
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test simulating an immutable and observable theme object, to ensure recomposition correctness
 * when retrieving values from this theme. This emulates the Colors object in Material, which
 * follows this 'observable' pattern.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ObservableThemeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testObservableTheme() {
        val testCase = ObservableThemeTestCase()
        composeTestRule
            .forGivenTestCase(testCase)
            .performTestWithEventsControl {
                doFrame()
                assertNoPendingChanges()

                assertEquals(2, testCase.primaryCompositions)
                assertEquals(1, testCase.secondaryCompositions)

                doFrame()
                assertNoPendingChanges()

                testCase.toggleState()

                doFramesUntilNoChangesPending(maxAmountOfFrames = 1)

                assertEquals(4, testCase.primaryCompositions)
                assertEquals(1, testCase.secondaryCompositions)
            }
    }

    @Test
    fun testImmutableTheme() {
        val testCase = ImmutableThemeTestCase()
        composeTestRule
            .forGivenTestCase(testCase)
            .performTestWithEventsControl {
                doFrame()
                assertNoPendingChanges()

                assertEquals(2, testCase.primaryCompositions)
                assertEquals(1, testCase.secondaryCompositions)

                doFrame()
                assertNoPendingChanges()

                testCase.toggleState()

                doFramesUntilNoChangesPending(maxAmountOfFrames = 1)

                assertEquals(4, testCase.primaryCompositions)
                assertEquals(2, testCase.secondaryCompositions)
            }
    }
}

private sealed class ThemeTestCase : ComposeTestCase, ToggleableTestCase {
    private var primaryState: MutableState<Color>? = null

    private val primaryTracker = CompositionTracker()
    private val secondaryTracker = CompositionTracker()

    @Composable
    override fun Content() {
        val primary = remember { mutableStateOf(Color.Red) }
        primaryState = primary

        val palette = createTheme(primary.value)

        App(palette, primaryTracker = primaryTracker, secondaryTracker = secondaryTracker)
    }

    override fun toggleState() {
        with(primaryState!!) {
            value = if (value == Color.Blue) Color.Red else Color.Blue
        }
    }

    @Composable
    internal abstract fun createTheme(primary: Color): TestTheme

    val primaryCompositions get() = primaryTracker.compositions
    val secondaryCompositions get() = secondaryTracker.compositions
}

private interface TestTheme {
    val primary: Color
    val secondary: Color
}

/**
 * Test case using an observable [TestTheme] that will be memoized and mutated when
 * incoming values change, causing only functions consuming the specific changed color to recompose.
 */
private class ObservableThemeTestCase : ThemeTestCase() {
    @Composable
    override fun createTheme(primary: Color): TestTheme {
        return remember { ObservableTheme(primary = primary) }.also { it.primary = primary }
    }

    private class ObservableTheme(primary: Color) : TestTheme {
        override var primary by mutableStateOf(primary, structuralEqualityPolicy())
        override var secondary by mutableStateOf(Color.Black, structuralEqualityPolicy())
    }
}

/**
 * Test case using an immutable [TestTheme], that will cause a new value to be assigned to the
 * CompositionLocal every time we change this object, causing everything consuming this
 * CompositionLocal to recompose.
 */
private class ImmutableThemeTestCase : ThemeTestCase() {
    @Composable
    override fun createTheme(primary: Color): TestTheme = ImmutableTheme(primary = primary)

    private class ImmutableTheme(override val primary: Color) : TestTheme {
        override val secondary = Color.Black
    }
}

@Composable
private fun App(
    theme: TestTheme,
    primaryTracker: CompositionTracker,
    secondaryTracker: CompositionTracker
) {
    CompositionLocalProvider(LocalTestTheme provides theme) {
        CheapPrimaryColorConsumer(primaryTracker)
        ExpensiveSecondaryColorConsumer(secondaryTracker)
        CheapPrimaryColorConsumer(primaryTracker)
    }
}

@Composable
private fun CheapPrimaryColorConsumer(compositionTracker: CompositionTracker) {
    val primary = LocalTestTheme.current.primary
    // Consume color variable to avoid any optimizations
    println("Color $primary")
    compositionTracker.compositions++
}

@Composable
private fun ExpensiveSecondaryColorConsumer(compositionTracker: CompositionTracker) {
    val secondary = LocalTestTheme.current.secondary
    // simulate some (relatively) expensive work
    Thread.sleep(1)
    // Consume color variable to avoid any optimizations
    println("Color $secondary")
    compositionTracker.compositions++
}

/**
 * Immutable as we want to ensure that we always skip recomposition unless the CompositionLocal
 * value inside the function changes.
 */
@Immutable
private class CompositionTracker(var compositions: Int = 0)

private val LocalTestTheme = staticCompositionLocalOf<TestTheme> {
    error("CompositionLocal LocalTestThemem not present")
}