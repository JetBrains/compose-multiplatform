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

package androidx.ui.test

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.onAncestors
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onSibling
import androidx.compose.ui.test.onSiblings

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("onParent()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.onParent() = onParent()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("onChildren()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.onChildren() = onChildren()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("onChild()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.onChild() = onChild()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("onChildAt(index)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.onChildAt(index: Int) = onChildAt(index)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("onSiblings()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.onSiblings() = onSiblings()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("onSibling()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.onSibling() = onSibling()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("onAncestors()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.onAncestors() = onAncestors()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("onFirst()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteractionCollection.onFirst() = onFirst()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("onLast()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteractionCollection.onLast() = onLast()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("filter()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteractionCollection.filter(
    matcher: SemanticsMatcher
) = filter(matcher)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("filterToOne()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteractionCollection.filterToOne(
    matcher: SemanticsMatcher
) = filterToOne(matcher)