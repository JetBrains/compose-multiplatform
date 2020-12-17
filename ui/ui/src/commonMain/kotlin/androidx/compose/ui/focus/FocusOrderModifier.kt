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

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A [modifier][Modifier.Element] that can be used to set a custom focus traversal order.
 */
interface FocusOrderModifier : Modifier.Element {

    /**
     * Populates the [next][FocusOrder.next] / [left][FocusOrder.left] /
     * [right][FocusOrder.right] / [up][FocusOrder.up] / [down][FocusOrder.down] items if
     * you don't want to use the default focus traversal order.
     */
    fun populateFocusOrder(focusOrder: FocusOrder)
}

/**
 * Specifies custom focus destinations that are used instead of the default focus traversal order.
 */
class FocusOrder {

    /**
     * A custom item to be used when the user moves focus to the "next" item.
     */
    var next: FocusRequester = FocusRequester.Default

    /**
     * A custom item to be used when the user moves focus to the "previous" item.
     */
    var previous: FocusRequester = FocusRequester.Default

    /**
     *  A custom item to be used when the user moves focus "up".
     */
    var up: FocusRequester = FocusRequester.Default

    /**
     *  A custom item to be used when the user moves focus "down".
     */
    var down: FocusRequester = FocusRequester.Default

    /**
     * A custom item to be used when the user moves focus to the "left" item.
     */
    var left: FocusRequester = FocusRequester.Default

    /**
     * A custom item to be used when the user moves focus to the "right" item.
     */
    var right: FocusRequester = FocusRequester.Default

    /**
     * A custom item to be used when the user moves focus to the "left" in LTR mode and "right"
     * in RTL mode.
     */
    var start: FocusRequester = FocusRequester.Default

    /**
     * A custom item to be used when the user moves focus to the "right" in LTR mode and "left"
     * in RTL mode.
     */
    var end: FocusRequester = FocusRequester.Default
}

internal class FocusOrderModifierImpl(
    val focusOrderReceiver: FocusOrder.() -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit
) : FocusOrderModifier, InspectorValueInfo(inspectorInfo) {
    override fun populateFocusOrder(focusOrder: FocusOrder) {
        focusOrderReceiver(focusOrder)
    }
}

/**
 * Use this modifier to specify a custom focus traversal order.
 *
 * @param focusOrderReceiver Specifies [FocusRequester]s that are used when the user wants
 * to move the current focus to the [next][FocusOrder.next] item, or wants to move
 * focus [left][FocusOrder.left], [right][FocusOrder.right], [up][FocusOrder.up] or
 * [down][FocusOrder.down].
 */
fun Modifier.focusOrder(focusOrderReceiver: FocusOrder.() -> Unit): Modifier {
    return this.then(
        FocusOrderModifierImpl(
            focusOrderReceiver = focusOrderReceiver,
            inspectorInfo = debugInspectorInfo {
                name = "focusOrder"
                properties["focusOrderReceiver"] = focusOrderReceiver
            }
        )
    )
}

/**
 * A modifier that lets you specify a [FocusRequester] for the current composable along with
 * [focusOrder].
 */
fun Modifier.focusOrder(
    focusRequester: FocusRequester,
    focusOrderReceiver: FocusOrder.() -> Unit
): Modifier = this
    .focusRequester(focusRequester)
    .focusOrder(focusOrderReceiver)
