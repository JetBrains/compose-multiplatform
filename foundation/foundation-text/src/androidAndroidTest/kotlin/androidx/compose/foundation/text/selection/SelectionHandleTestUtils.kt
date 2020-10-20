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

package androidx.compose.foundation.text.selection

import android.view.View
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.node.Owner
import androidx.compose.ui.platform.AndroidOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.isPopupLayout
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions
import androidx.ui.test.ComposeTestRule
import androidx.ui.test.onRoot
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

internal fun ComposeTestRule.doubleSelectionHandleMatches(
    index: Int,
    viewMatcher: Matcher<in View>
) {
    // Make sure that current measurement/drawing is finished
    runOnIdle { }
    Espresso.onView(CoreMatchers.instanceOf(Owner::class.java))
        .inRoot(DoubleSelectionHandleMatcher(index))
        .check(ViewAssertions.matches(viewMatcher))
}

internal class DoubleSelectionHandleMatcher(val index: Int) : TypeSafeMatcher<Root>() {
    var popupsMatchedSoFar: Int = 0

    override fun describeTo(description: Description?) {
        description?.appendText("DoubleSelectionHandleMatcher")
    }

    override fun matchesSafely(item: Root?): Boolean {
        val matches = item != null && isPopupLayout(item.decorView)
        if (matches) {
            popupsMatchedSoFar++
        }
        return matches && popupsMatchedSoFar == index + 1
    }
}

internal fun ComposeTestRule.rootWidth(): Dp {
    val nodeInteraction = onRoot()
    val node = nodeInteraction.fetchSemanticsNode("Failed to get screen width")

    @OptIn(ExperimentalLayoutNodeApi::class)
    val owner = node.componentNode.owner as AndroidOwner

    return with(owner.density) {
        owner.view.width.toDp()
    }
}
