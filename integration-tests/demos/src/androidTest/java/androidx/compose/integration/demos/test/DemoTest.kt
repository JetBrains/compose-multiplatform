/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.integration.demos.test

import androidx.compose.integration.demos.AllDemosCategory
import androidx.compose.integration.demos.DemoActivity
import androidx.compose.integration.demos.Tags
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.Demo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.integration.demos.common.allDemos
import androidx.compose.integration.demos.common.allLaunchableDemos
import androidx.compose.material3.demos.Material3Demos
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val ignoredDemos = listOf<String>(
    // Not ignoring any of them \o/
)

@FlakyTest(bugId = 204322457)
@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DemoTest {
    // We need to provide the recompose factory first to use new clock.
    @get:Rule
    val rule = createAndroidComposeRule<DemoActivity>()

    @Test
    fun testFiltering() {
        assertIsOnRootScreen()
        // Enter filtering mode
        rule.onNodeWithTag(Tags.FilterButton).performClick()

        // TODO: use keyboard input APIs when available to actually filter the list
        val testDemo = AllButIgnoredDemos.allLaunchableDemos()
            // ActivityDemos don't set the title in the AppBar, so we can't verify if we've
            // opened the right one. So, only use ComposableDemos
            .filterIsInstance<ComposableDemo>()
            .sortedBy { it.title }
            .first()

        // Click on the first demo.
        val demoTitle = testDemo.title
        rule.onNodeWithText(demoTitle).performScrollTo().performClick()
        assertAppBarHasTitle(demoTitle)

        // Navigate back to root screen.
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        rule.waitForIdle()
        assertIsOnRootScreen()
    }

    @Test
    @MediumTest
    fun testAllDemosAreBeingTested() {
        assertThat(
            SplitDemoCategories.sumOf { it.allLaunchableDemos().size }
        ).isEqualTo(AllButIgnoredDemos.allLaunchableDemos().size)
    }

    @Test
    fun navigateThroughAllDemos_1() {
        navigateThroughAllDemos(SplitDemoCategories[0])
    }

    @Test
    fun navigateThroughAllDemos_2() {
        navigateThroughAllDemos(SplitDemoCategories[1])
    }

    @Test
    fun navigateThroughAllDemos_3() {
        navigateThroughAllDemos(SplitDemoCategories[2])
    }

    // Broken: b/206811195
    @Ignore
    @Test
    fun navigateThroughAllDemos_4() {
        navigateThroughAllDemos(SplitDemoCategories[3])
    }

    @Test
    fun navigateThroughMaterial3Demos() {
        val material3Demos = DemoCategory(
            "Jetpack Compose Demos",
            listOf(
                Material3Demos,
            )
        )
        navigateThroughAllDemos(material3Demos)
    }

    private fun navigateThroughAllDemos(root: DemoCategory, fastForwardClock: Boolean = false) {
        // Keep track of each demo we visit
        val visitedDemos = mutableListOf<Demo>()

        // Visit all demos, ensuring we start and end up on the root screen
        assertIsOnRootScreen()
        root.visitDemos(
            visitedDemos = visitedDemos,
            path = listOf(root),
            fastForwardClock = fastForwardClock
        )
        assertIsOnRootScreen()

        // Ensure that we visited all the demos we expected to, in the order we expected to.
        assertThat(visitedDemos).isEqualTo(root.allDemos())
    }

    /**
     * DFS traversal of each demo in a [DemoCategory] using [Demo.visit]
     *
     * @param path The path of categories that leads to this demo
     */
    private fun DemoCategory.visitDemos(
        visitedDemos: MutableList<Demo>,
        path: List<DemoCategory>,
        fastForwardClock: Boolean
    ) {
        demos.forEach { demo ->
            visitedDemos.add(demo)
            demo.visit(visitedDemos, path, fastForwardClock)
        }
    }

    /**
     * Visits a [Demo], and then navigates back up to the [DemoCategory] it was inside.
     *
     * If this [Demo] is a [DemoCategory], this will visit sub-[Demo]s first before continuing
     * in the current category.
     *
     * @param path The path of categories that leads to this demo
     */
    private fun Demo.visit(
        visitedDemos: MutableList<Demo>,
        path: List<DemoCategory>,
        fastForwardClock: Boolean
    ) {
        val navigationTitle = path.navigationTitle

        if (fastForwardClock) {
            // Skip through the enter animation of the list screen
            fastForwardClock()
        }

        rule.onNode(hasText(title) and hasClickAction())
            .assertExists("Couldn't find \"$title\" in \"$navigationTitle\"")
            .performScrollTo()
            .performClick()

        if (this is DemoCategory) {
            visitDemos(visitedDemos, path + this, fastForwardClock)
        }

        if (fastForwardClock) {
            // Skip through the enter animation of the visited demo
            fastForwardClock()
        }

        rule.waitForIdle()
        while (rule.onAllNodes(isDialog()).isNotEmpty()) {
            Espresso.pressBack()
            rule.waitForIdle()
        }

        clearFocusFromDemo()
        rule.waitForIdle()

        Espresso.pressBack()
        rule.waitForIdle()

        if (fastForwardClock) {
            // Pump press back
            fastForwardClock(2000)
        }

        assertAppBarHasTitle(navigationTitle)
    }

    private fun fastForwardClock(millis: Long = 5000) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(millis)
    }

    /**
     * Asserts that the app bar title matches the root category title, so we are on the root screen.
     */
    private fun assertIsOnRootScreen() = assertAppBarHasTitle(AllDemosCategory.title)

    /**
     * Asserts that the app bar title matches the given [title].
     */
    private fun assertAppBarHasTitle(title: String) =
        rule.onNodeWithTag(Tags.AppBarTitle).assertTextEquals(title)

    private fun SemanticsNodeInteractionCollection.isNotEmpty(): Boolean {
        return fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }

    private fun clearFocusFromDemo() {
        with(rule.activity) {
            if (hostView.hasFocus()) {
                if (hostView.isFocused) {
                    // One of the Compose components has focus.
                    focusManager.clearFocus(force = true)
                } else {
                    // A child view has focus. (View interop scenario).
                    // We could also use hostViewGroup.focusedChild?.clearFocus(), but the
                    // interop views might end up being focused if one of them is marked as
                    // focusedByDefault. So we clear focus by requesting focus on the owner.
                    rule.runOnUiThread { hostView.requestFocus() }
                }
            }
        }
    }
}

private val AllButIgnoredDemos =
    AllDemosCategory.filter { path, demo ->
        demo.navigationTitle(path) !in ignoredDemos
    }

private val SplitDemoCategories = AllButIgnoredDemos.let { root ->
    root.allLaunchableDemos().let { leaves ->
        val size = leaves.size
        leaves.withIndex()
            .groupBy { it.index * 4 / size }
            .map {
                val selectedLeaves = it.value.map { it.value }
                root.filter { _, demo ->
                    demo in selectedLeaves
                }
            }
    }
}

private fun Demo.navigationTitle(path: List<DemoCategory>): String {
    return path.plus(this).navigationTitle
}

private val List<Demo>.navigationTitle: String
    get() = if (size == 1) first().title else drop(1).joinToString(" > ")

/**
 * Trims the tree of [Demo]s represented by this [DemoCategory] by cutting all leave demos for
 * which the [predicate] returns `false` and recursively removing all empty categories as a result.
 */
private fun DemoCategory.filter(
    path: List<DemoCategory> = emptyList(),
    predicate: (path: List<DemoCategory>, demo: Demo) -> Boolean
): DemoCategory {
    val newPath = path + this
    return DemoCategory(
        title,
        demos.mapNotNull {
            when (it) {
                is DemoCategory -> {
                    it.filter(newPath, predicate).let { if (it.demos.isEmpty()) null else it }
                }
                else -> {
                    if (predicate(newPath, it)) it else null
                }
            }
        }
    )
}
