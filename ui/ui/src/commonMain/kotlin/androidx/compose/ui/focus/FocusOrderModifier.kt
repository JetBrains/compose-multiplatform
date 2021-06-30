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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifiedFocusNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.LayoutDirection

/**
 * A [modifier][Modifier.Element] that can be used to set a custom focus traversal order.
 *
 * @see Modifier.focusOrder
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
 *
 * @sample androidx.compose.ui.samples.CustomFocusOrderSample
 */
class FocusOrder {

    /**
     * A custom item to be used when the user requests a focus moves to the "next" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var next: FocusRequester = FocusRequester.Default

    /**
     * A custom item to be used when the user requests a focus moves to the "previous" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var previous: FocusRequester = FocusRequester.Default

    /**
     *  A custom item to be used when the user moves focus "up".
     *
     *  @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var up: FocusRequester = FocusRequester.Default

    /**
     *  A custom item to be used when the user moves focus "down".
     *
     *  @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var down: FocusRequester = FocusRequester.Default

    /**
     * A custom item to be used when the user requests a focus moves to the "left" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var left: FocusRequester = FocusRequester.Default

    /**
     * A custom item to be used when the user requests a focus moves to the "right" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var right: FocusRequester = FocusRequester.Default

    /**
     * A custom item to be used when the user requests a focus moves to the "left" in LTR mode and
     * "right" in RTL mode.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var start: FocusRequester = FocusRequester.Default

    /**
     * A custom item to be used when the user requests a focus moves to the "right" in LTR mode
     * and "left" in RTL mode.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
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
 *
 * @sample androidx.compose.ui.samples.CustomFocusOrderSample
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
 * A modifier that lets you specify a [FocusRequester] for the current composable so that this
 * [focusRequester] can be used by another composable to specify a custom focus order.
 *
 * @sample androidx.compose.ui.samples.CustomFocusOrderSample
 */
fun Modifier.focusOrder(focusRequester: FocusRequester): Modifier = focusRequester(focusRequester)

/**
 * A modifier that lets you specify a [FocusRequester] for the current composable along with
 * [focusOrder].
 *
 * @sample androidx.compose.ui.samples.CustomFocusOrderSample
 */
fun Modifier.focusOrder(
    focusRequester: FocusRequester,
    focusOrderReceiver: FocusOrder.() -> Unit
): Modifier = this
    .focusRequester(focusRequester)
    .focusOrder(focusOrderReceiver)

/**
 * Search up the component tree for any parent/parents that have specified a custom focus order.
 * Allowing parents higher up the hierarchy to overwrite the focus order specified by their
 * children.
 */
internal fun ModifiedFocusNode.customFocusSearch(
    focusDirection: FocusDirection,
    layoutDirection: LayoutDirection
): FocusRequester {
    val focusOrder = FocusOrder()
    wrappedBy?.populateFocusOrder(focusOrder)

    return when (focusDirection) {
        FocusDirection.Next -> focusOrder.next
        FocusDirection.Previous -> focusOrder.previous
        FocusDirection.Up -> focusOrder.up
        FocusDirection.Down -> focusOrder.down
        FocusDirection.Left -> when (layoutDirection) {
            LayoutDirection.Ltr -> focusOrder.start
            LayoutDirection.Rtl -> focusOrder.end
        }.takeUnless { it == FocusRequester.Default } ?: focusOrder.left
        FocusDirection.Right -> when (layoutDirection) {
            LayoutDirection.Ltr -> focusOrder.end
            LayoutDirection.Rtl -> focusOrder.start
        }.takeUnless { it == FocusRequester.Default } ?: focusOrder.right
        // TODO(b/183746982): add focus order API for "In" and "Out".
        //  Developers can to specify a custom "In" to specify which child should be visited when
        //  the user presses dPad center. (They can also redirect the "In" to some other item).
        //  Developers can specify a custom "Out" to specify which composable should take focus
        //  when the user presses the back button.
        @OptIn(ExperimentalComposeUiApi::class)
        (FocusDirection.In) -> FocusRequester.Default
        @OptIn(ExperimentalComposeUiApi::class)
        (FocusDirection.Out) -> FocusRequester.Default
        else -> error("invalid FocusDirection")
    }
}