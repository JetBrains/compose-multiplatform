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

package androidx.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A [Modifier.Element] that controls the drawing order for the children of the same layout
 * parent. A child with larger [zIndex] will be drawn on top of all the children with smaller
 * [zIndex]. When children have the same [zIndex] the original order in which the items were
 * added into the parent layout is applied.
 *
 * Note that if there would be multiple [ZIndexModifier] modifiers applied for the same layout
 * the sum of their values will be used as the final zIndex. If no [ZIndexModifier]s applied for the
 * layout then zIndex for this Layout is 0.
 *
 * @see [Modifier.zIndex]
 */
interface ZIndexModifier : Modifier.Element {
    val zIndex: Float
}

/**
 * Creates a [ZIndexModifier] that controls the drawing order for the children of the same layout
 * parent. A child with larger [zIndex] will be drawn on top of all the children with smaller
 * [zIndex]. When children have the same [zIndex] the original order in which the items were
 * added into the parent layout is applied.
 *
 * Note that if there would be multiple [ZIndexModifier] modifiers applied for the same layout
 * the sum of their values will be used as the final zIndex. If no [ZIndexModifier]s applied for the
 * layout then zIndex for this Layout is 0.
 *
 * @sample androidx.compose.ui.samples.ZIndexModifierSample
 */
@Stable
fun Modifier.zIndex(zIndex: Float): Modifier = this.then(
    SimpleZIndexModifier(
        zIndex = zIndex,
        inspectorInfo = debugInspectorInfo {
            name = "zIndex"
            value = zIndex
        }
    )
)

private class SimpleZIndexModifier(
    override val zIndex: Float,
    inspectorInfo: InspectorInfo.() -> Unit
) : ZIndexModifier, InspectorValueInfo(inspectorInfo) {

    override fun hashCode(): Int =
        zIndex.hashCode()

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? SimpleZIndexModifier ?: return false
        return zIndex == otherModifier.zIndex
    }

    override fun toString(): String =
        "SimpleZIndexModifier(zIndex=$zIndex)"
}
