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

import androidx.compose.foundation.text.Handle
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Dp
import com.google.common.truth.Truth.assertWithMessage

/**
 * Matches selection handles by looking for the [SelectionHandleInfoKey] property that has a
 * [SelectionHandleInfo] with the given [handle]. If [handle] is null (the default), then all
 * handles are matched.
 */
internal fun isSelectionHandle(handle: Handle? = null) =
    SemanticsMatcher("is ${handle ?: "any"} handle") { node ->
        if (handle == null) {
            SelectionHandleInfoKey in node.config
        } else {
            node.config.getOrNull(SelectionHandleInfoKey)?.handle == handle
        }
    }

/**
 * Asserts about the [SelectionHandleInfo.position] for the matching node. This is the position of
 * the handle's _anchor_, not the position of the popup itself. E.g. for a cursor handle this is the
 * position of the bottom of the cursor, which will be in the center of the popup.
 */
internal fun SemanticsNodeInteraction.assertHandlePositionMatches(
    expectedX: Dp,
    expectedY: Dp
) {
    val node = fetchSemanticsNode()
    with(node.layoutInfo.density) {
        val positionFound = node.config[SelectionHandleInfoKey].position
        val positionFoundX = positionFound.x.toDp()
        val positionFoundY = positionFound.y.toDp()
        val message = "Expected position ($expectedX, $expectedY), " +
            "but found ($positionFoundX, $positionFoundY)"
        assertWithMessage(message).that(positionFoundX.value)
            .isWithin(5f).of(expectedX.value)
        assertWithMessage(message).that(positionFoundY.value)
            .isWithin(5f).of(expectedY.value)
    }
}
