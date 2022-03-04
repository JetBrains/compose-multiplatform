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

package androidx.compose.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.common.truth.IterableSubject

/**
 * This function adds a parent composable which has size.
 * [View.requestFocus()][android.view.View.requestFocus] will not take focus if the view has no
 * size.
 */
internal fun ComposeContentTestRule.setFocusableContent(content: @Composable () -> Unit) {
    setContent {
        Box(modifier = Modifier.requiredSize(10.dp, 10.dp)) { content() }
    }
}

/**
 * This is a composable that makes it easier to create focusable boxes with a specific offset and
 * dimensions.
 */
@Composable
internal fun FocusableBox(
    isFocused: MutableState<Boolean>,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    focusRequester: FocusRequester? = null,
    deactivated: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    Layout(
        content = content,
        modifier = Modifier
            .offset { IntOffset(x, y) }
            .focusRequester(focusRequester ?: remember { FocusRequester() })
            .onFocusChanged { isFocused.value = it.isFocused }
            .focusProperties { canFocus = !deactivated }
            .focusTarget(),
        measurePolicy = remember(width, height) {
            MeasurePolicy { measurables, constraint ->
                layout(width, height) {
                    measurables.forEach {
                        val placeable = it.measure(constraint)
                        placeable.placeRelative(0, 0)
                    }
                }
            }
        }
    )
}

/**
 * Asserts that the elements appear in the specified order.
 *
 * Consider using this helper function instead of
 * [containsExactlyElementsIn][com.google.common.truth.IterableSubject.containsExactlyElementsIn]
 * or [containsExactly][com.google.common.truth.IterableSubject.containsExactly] as it also asserts
 * that the elements are in the specified order.
 */
fun IterableSubject.isExactly(vararg expected: Any?) {
    return containsExactlyElementsIn(expected).inOrder()
}

/**
 * focusTarget needs a SideEffect to work.
 */
internal fun Modifier.focusTarget(focusModifier: FocusModifier) = composed {
    SideEffect {
        focusModifier.sendOnFocusEvent()
    }
    this.then(focusModifier).then(ResetFocusModifierLocals)
}