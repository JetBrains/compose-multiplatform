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

import androidx.compose.androidview.demos.ComposeInAndroidDialogDismissDialogDuringDispatch
import androidx.test.espresso.Espresso
import androidx.test.filters.LargeTest
import androidx.compose.integration.demos.AllDemosCategory
import androidx.compose.integration.demos.DemoActivity
import androidx.compose.integration.demos.Tags
import androidx.compose.integration.demos.common.ActivityDemo
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.Demo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.integration.demos.common.allDemos
import androidx.compose.integration.demos.common.allLaunchableDemos
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.ui.test.SemanticsNodeInteractionCollection
import androidx.ui.test.createAndroidComposeRule
import androidx.ui.test.assertTextEquals
import androidx.ui.test.performClick
import androidx.ui.test.performScrollTo
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.onNodeWithText
import androidx.ui.test.hasClickAction
import androidx.ui.test.hasText
import androidx.ui.test.isDialog
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoTest {
    @get:Rule
    val rule = createAndroidComposeRule<DemoActivity>(disableTransitions = true)

    private val SplitDemoCategories = mutableListOf<List<Demo>>().apply {
        val numberOfCategoriesPerSplit = AllDemosCategory.demos.size / 4

        add(
            AllDemosCategory.demos.subList(
                fromIndex = 0,
                toIndex = numberOfCategoriesPerSplit
            )
        )

        add(
            AllDemosCategory.demos.subList(
                fromIndex = numberOfCategoriesPerSplit,
                toIndex = numberOfCategoriesPerSplit * 2
            )
        )

        add(
            AllDemosCategory.demos.subList(
                fromIndex = numberOfCategoriesPerSplit * 2,
                toIndex = numberOfCategoriesPerSplit * 3
            )
        )

        add(
            AllDemosCategory.demos.subList(
                fromIndex = numberOfCategoriesPerSplit * 3,
                toIndex = AllDemosCategory.demos.size
            )
        )

        require(AllDemosCategory.demos.size == sumBy { it.size }) {
            "Not all demo categories are being tested!"
        }
    }

    @LargeTest
    @Test
    fun testFiltering() {
        assertIsOnRootScreen()
        // Enter filtering mode
        rule.onNodeWithTag(Tags.FilterButton).performClick()

        // TODO: use keyboard input APIs when available to actually filter the list
        val testDemo = AllDemosCategory.allLaunchableDemos()
            // ActivityDemos don't set the title in the AppBar, so we can't verify if we've
            // opened the right one. So, only use ComposableDemos
            .filterIsInstance<ComposableDemo>()
            .sortedBy { it.title }
            .first()
        // Click on the first demo
        val demoTitle = testDemo.title
        rule.onNodeWithText(demoTitle).performScrollTo().performClick()

        assertAppBarHasTitle(demoTitle)
        Espresso.pressBack()
        assertIsOnRootScreen()
    }

    @LargeTest
    @Test
    fun navigateThroughAllDemos_1() {
        navigateThroughAllDemos(SplitDemoCategories[0])
    }

    @LargeTest
    @Test
    fun navigateThroughAllDemos_2() {
        navigateThroughAllDemos(SplitDemoCategories[1])
    }

    @LargeTest
    @Test
    fun navigateThroughAllDemos_3() {
        navigateThroughAllDemos(SplitDemoCategories[2])
    }

    @LargeTest
    @Test
    fun navigateThroughAllDemos_4() {
        navigateThroughAllDemos(SplitDemoCategories[3])
    }

    private fun navigateThroughAllDemos(demoCategories: List<Demo>) {
        // Keep track of each demo we visit
        val visitedDemos = mutableListOf<Demo>()

        // Visit all demos, ensuring we start and end up on the root screen
        assertIsOnRootScreen()
        val root = DemoCategory(AllDemosCategory.title, demoCategories)
        root.visitDemos(
            visitedDemos = visitedDemos,
            path = listOf(root)
        )
        assertIsOnRootScreen()

        val expectedDemos = root.allDemos()

        // Ensure that we visited all the demos we expected to, in the order we expected to.
        assertThat(visitedDemos).isEqualTo(expectedDemos)
    }

    /**
     * DFS traversal of each demo in a [DemoCategory] using [Demo.visit]
     *
     * @param path The path of categories that leads to this demo
     */
    private fun DemoCategory.visitDemos(
        visitedDemos: MutableList<Demo>,
        path: List<DemoCategory>
    ) {
        demos.forEach { demo ->
            visitedDemos.add(demo)
            demo.visit(visitedDemos, path)
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
        path: List<DemoCategory>
    ) {
        val navigationTitle = if (path.size == 1) {
            path.first().title
        } else {
            path.drop(1).joinToString(" > ")
        }

        rule.onNode(hasText(title) and hasClickAction())
            .assertExists("Couldn't find \"$title\" in \"$navigationTitle\"")
            .performScrollTo()
            .performClick()

        if (this is DemoCategory) {
            visitDemos(visitedDemos, path + this)
        }

        // TODO: b/165693257 demos without a compose view crash as onAllNodes will fail to
        // find the semantic nodes.
        val hasComposeView: Boolean = (this as? ActivityDemo<*>)
            ?.activityClass != ComposeInAndroidDialogDismissDialogDuringDispatch::class

        if (hasComposeView) {
            while (rule.onAllNodes(isDialog()).isNotEmpty()) {
                rule.waitForIdle()
                Espresso.pressBack()
            }
        }

        rule.waitForIdle()
        Espresso.pressBack()

        assertAppBarHasTitle(navigationTitle)
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
        return fetchSemanticsNodes().isNotEmpty()
    }
}