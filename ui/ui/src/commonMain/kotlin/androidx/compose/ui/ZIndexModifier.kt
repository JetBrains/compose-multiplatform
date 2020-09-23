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
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement

/**
 * A [Modifier.Element] that controls the drawing order for the children of the same layout
 * parent. A child with larger [zIndex] will be drawn after all the children with smaller [zIndex].
 * When children have the same [zIndex] the original order in which the items were added into the
 * parent layout is applied.
 * Note that if there would be multiple [ZIndexModifier] modifiers applied for the same layout
 * only the first one in the modifiers chain will be used. If no [ZIndexModifier]s applied for the
 * layout then zIndex for this Layout is 0.
 *
 * @see [Modifier.zIndex]
 */
interface ZIndexModifier : Modifier.Element {
    val zIndex: Float
}

/**
 * Creates a [ZIndexModifier] that controls the drawing order for the children of the same layout
 * parent. A child with larger [zIndex] will be drawn after all the children with smaller [zIndex].
 * When children have the same [zIndex] the original order in which the items were added into the
 * parent layout is applied.
 * Note that if there would be multiple [ZIndexModifier] modifiers applied for the same layout
 * only the first one in the modifiers chain will be used. If no [ZIndexModifier]s applied for the
 * layout then zIndex for this Layout is 0.
 *
 * @sample androidx.compose.ui.samples.ZIndexModifierSample
 */
@Stable
fun Modifier.zIndex(zIndex: Float): Modifier = this.then(SimpleZIndexModifier(zIndex))

private data class SimpleZIndexModifier(
    override val zIndex: Float
) : ZIndexModifier, InspectableValue {
    override val nameFallback = "zIndex"
    override val valueOverride = zIndex
    override val inspectableElements = emptySequence<ValueElement>()
}
