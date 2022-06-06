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

package androidx.compose.ui.layout

import androidx.compose.ui.Modifier
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * A [Modifier.Element] that provides a [Remeasurement] object associated with the layout node
 * the modifier is applied to.
 */
@JvmDefaultWithCompatibility
interface RemeasurementModifier : Modifier.Element {
    /**
     * This method is executed when the modifier is attached to the layout node.
     *
     * @param remeasurement [Remeasurement] object associated with the layout node the modifier is
     * applied to.
     */
    fun onRemeasurementAvailable(remeasurement: Remeasurement)
}

/**
 * This object is associated with a layout node and allows to execute some extra measure/layout
 * actions which are needed for some complex layouts. In most cases you don't need it as
 * measuring and layout should be correctly working automatically for most cases.
 */
interface Remeasurement {
    /**
     * Performs the node remeasuring synchronously even if the node was not marked as needs
     * remeasure before. Useful for cases like when during scrolling you need to re-execute the
     * measure block to consume the scroll offset and remeasure your children in a blocking way.
     */
    fun forceRemeasure()
}
