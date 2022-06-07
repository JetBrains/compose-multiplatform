/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A Modifier local that stores [FocusProperties] for a sub-hierarchy.
 *
 * @see [focusProperties]
 */
internal val ModifierLocalFocusProperties =
    modifierLocalOf<FocusPropertiesModifier?> { null }

internal object DefaultFocusProperties : FocusProperties {
    override var canFocus: Boolean
        get() = true
        set(_) = noSet()

    override var next: FocusRequester
        get() = FocusRequester.Default
        set(_) = noSet()

    override var previous: FocusRequester
        get() = FocusRequester.Default
        set(_) = noSet()

    override var up: FocusRequester
        get() = FocusRequester.Default
        set(_) = noSet()

    override var down: FocusRequester
        get() = FocusRequester.Default
        set(_) = noSet()

    override var left: FocusRequester
        get() = FocusRequester.Default
        set(_) = noSet()

    override var right: FocusRequester
        get() = FocusRequester.Default
        set(_) = noSet()

    override var start: FocusRequester
        get() = FocusRequester.Default
        set(_) = noSet()

    override var end: FocusRequester
        get() = FocusRequester.Default
        set(_) = noSet()

    private fun noSet(): Nothing = error("Attempting to change DefaultFocusProperties")
}

/**
 * Properties that are applied to [focusTarget]s that can read the [ModifierLocalFocusProperties]
 * Modifier Local.
 *
 * @see [focusProperties]
 */
interface FocusProperties {
    /**
     * When set to false, indicates that the [focusTarget] that this is applied to can no longer
     * take focus. If the [focusTarget] is currently focused, setting this property to false will
     * end up clearing focus.
     */
    var canFocus: Boolean

    /**
     * A custom item to be used when the user requests the focus to move to the "next" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var next: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user requests the focus to move to the "previous" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var previous: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     *  A custom item to be used when the user moves focus "up".
     *
     *  @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var up: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     *  A custom item to be used when the user moves focus "down".
     *
     *  @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var down: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user requests a focus moves to the "left" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var left: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user requests a focus moves to the "right" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var right: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user requests a focus moves to the "left" in LTR mode and
     * "right" in RTL mode.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var start: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user requests a focus moves to the "right" in LTR mode
     * and "left" in RTL mode.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var end: FocusRequester
        get() = FocusRequester.Default
        set(_) {}
}

/**
 * This modifier allows you to specify properties that are accessible to [focusTarget]s further
 * down the modifier chain or on child layout nodes.
 *
 * @sample androidx.compose.ui.samples.FocusPropertiesSample
 */
fun Modifier.focusProperties(scope: FocusProperties.() -> Unit): Modifier = this.then(
    FocusPropertiesModifier(
        focusPropertiesScope = scope,
        inspectorInfo = debugInspectorInfo {
            name = "focusProperties"
            properties["scope"] = scope
        }
    )
)

@Stable
internal class FocusPropertiesModifier(
    val focusPropertiesScope: FocusProperties.() -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit
) : ModifierLocalConsumer,
    ModifierLocalProvider<FocusPropertiesModifier?>,
    InspectorValueInfo(inspectorInfo) {

    private var parent: FocusPropertiesModifier? by mutableStateOf(null)

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        parent = scope.run { ModifierLocalFocusProperties.current }
    }

    override val key = ModifierLocalFocusProperties

    override val value: FocusPropertiesModifier
        get() = this

    override fun equals(other: Any?) =
        other is FocusPropertiesModifier && focusPropertiesScope == other.focusPropertiesScope

    override fun hashCode() = focusPropertiesScope.hashCode()

    fun calculateProperties(focusProperties: FocusProperties) {
        // Populate with the specified focus properties.
        focusProperties.apply(focusPropertiesScope)

        // Parent can override any values set by this
        parent?.calculateProperties(focusProperties)
    }
}

internal fun FocusModifier.setUpdatedProperties(properties: FocusProperties) {
    if (properties.canFocus) activateNode() else deactivateNode()
}

internal class FocusPropertiesImpl : FocusProperties {
    override var canFocus: Boolean = true
    override var next: FocusRequester = FocusRequester.Default
    override var previous: FocusRequester = FocusRequester.Default
    override var up: FocusRequester = FocusRequester.Default
    override var down: FocusRequester = FocusRequester.Default
    override var left: FocusRequester = FocusRequester.Default
    override var right: FocusRequester = FocusRequester.Default
    override var start: FocusRequester = FocusRequester.Default
    override var end: FocusRequester = FocusRequester.Default
}

internal fun FocusProperties.clear() {
    canFocus = true
    next = FocusRequester.Default
    previous = FocusRequester.Default
    up = FocusRequester.Default
    down = FocusRequester.Default
    left = FocusRequester.Default
    right = FocusRequester.Default
    start = FocusRequester.Default
    end = FocusRequester.Default
}

internal fun FocusModifier.refreshFocusProperties() {
    val layoutNodeWrapper = layoutNodeWrapper ?: return
    focusProperties.clear()
    layoutNodeWrapper.layoutNode.owner?.snapshotObserver?.observeReads(this,
        FocusModifier.RefreshFocusProperties
    ) {
        focusPropertiesModifier?.calculateProperties(focusProperties)
    }
    setUpdatedProperties(focusProperties)
}