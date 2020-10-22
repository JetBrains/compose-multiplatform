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

package androidx.compose.ui.test

import androidx.compose.ui.semantics.SemanticsNode

/**
 * Executes [selector] on every parent of this [SemanticsNode] and returns the closest
 * [SemanticsNode] to return `true` from [selector] or null if [selector] returns false
 * for all ancestors.
 *
 * @param includeSelf Whether it should include self into the search.
 */
internal fun SemanticsNode.findClosestParentNode(
    includeSelf: Boolean = false,
    selector: (SemanticsNode) -> Boolean
): SemanticsNode? {
    var currentParent = if (includeSelf) this else parent
    while (currentParent != null) {
        if (selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parent
        }
    }

    return null
}