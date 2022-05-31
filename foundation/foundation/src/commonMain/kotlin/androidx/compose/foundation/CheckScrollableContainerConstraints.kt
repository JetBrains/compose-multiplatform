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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.Constraints

/**
 * @throws [IllegalStateException] if the container was measured with the infinity constraints
 * in the direction of scrolling. This usually means nesting scrollable in the same direction
 * containers which is a performance issue and is discouraged.
 *
 * @param constraints [Constraints] used to measure the scrollable container
 * @param orientation orientation of the scrolling
 */
@ExperimentalFoundationApi
fun checkScrollableContainerConstraints(
    constraints: Constraints,
    orientation: Orientation
) {
    if (orientation == Orientation.Vertical) {
        check(constraints.maxHeight != Constraints.Infinity) {
            "Vertically scrollable component was measured with an infinity maximum height " +
                "constraints, which is disallowed. One of the common reasons is nesting layouts " +
                "like LazyColumn and Column(Modifier.verticalScroll()). If you want to add a " +
                "header before the list of items please add a header as a separate item() before " +
                "the main items() inside the LazyColumn scope. There are could be other reasons " +
                "for this to happen: your ComposeView was added into a LinearLayout with some " +
                "weight, you applied Modifier.wrapContentSize(unbounded = true) or wrote a " +
                "custom layout. Please try to remove the source of infinite constraints in the " +
                "hierarchy above the scrolling container."
        }
    } else {
        check(constraints.maxWidth != Constraints.Infinity) {
            "Horizontally scrollable component was measured with an infinity maximum width " +
                "constraints, which is disallowed. One of the common reasons is nesting layouts " +
                "like LazyRow and Row(Modifier.horizontalScroll()). If you want to add a " +
                "header before the list of items please add a header as a separate item() before " +
                "the main items() inside the LazyRow scope. There are could be other reasons " +
                "for this to happen: your ComposeView was added into a LinearLayout with some " +
                "weight, you applied Modifier.wrapContentSize(unbounded = true) or wrote a " +
                "custom layout. Please try to remove the source of infinite constraints in the " +
                "hierarchy above the scrolling container."
        }
    }
}
