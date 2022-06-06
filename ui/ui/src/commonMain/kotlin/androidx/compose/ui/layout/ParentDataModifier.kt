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
import androidx.compose.ui.unit.Density
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * A [Modifier] that provides data to the parent [Layout]. This can be read from within the
 * the [Layout] during measurement and positioning, via [IntrinsicMeasurable.parentData].
 * The parent data is commonly used to inform the parent how the child [Layout] should be measured
 * and positioned.
 */
@JvmDefaultWithCompatibility
interface ParentDataModifier : Modifier.Element {
    /**
     * Provides a parentData, given the [parentData] already provided through the modifier's chain.
     */
    fun Density.modifyParentData(parentData: Any?): Any?
}
