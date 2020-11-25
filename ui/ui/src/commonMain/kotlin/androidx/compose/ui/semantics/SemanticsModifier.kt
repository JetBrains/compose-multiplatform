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

package androidx.compose.ui.semantics

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.AtomicInt
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A [Modifier.Element] that adds semantics key/value for use in testing,
 * accessibility, and similar use cases.
 */
interface SemanticsModifier : Modifier.Element {
    /**
     * The unique id of this semantics.
     *
     * Should be generated from SemanticsModifierCore.generateSemanticsId().
     */
    val id: Int

    /**
     * The SemanticsConfiguration holds substantive data, especially a list of key/value pairs
     * such as (label -> "buttonName").
     */
    val semanticsConfiguration: SemanticsConfiguration
}

internal class SemanticsModifierCore(
    override val id: Int,
    mergeDescendants: Boolean,
    properties: (SemanticsPropertyReceiver.() -> Unit)
) : SemanticsModifier {
    override val semanticsConfiguration: SemanticsConfiguration =
        SemanticsConfiguration().also {
            it.isMergingSemanticsOfDescendants = mergeDescendants

            it.properties()
        }
    companion object {
        private var lastIdentifier = AtomicInt(0)
        fun generateSemanticsId() = lastIdentifier.addAndGet(1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SemanticsModifierCore) return false

        if (id != other.id) return false
        if (semanticsConfiguration != other.semanticsConfiguration) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * semanticsConfiguration.hashCode() + id.hashCode()
    }
}

/**
 * Add semantics key/value for use in testing, accessibility, and similar use cases.
 *
 * @param mergeDescendants Whether the semantic information provided by the owning component and
 * its descendants (which do not themselves merge descendants) should be treated as one logical
 * entity.
 * @param properties properties to add to the semantics. [SemanticsPropertyReceiver] will be
 * provided in the scope to allow access for common properties and its values.
 */
fun Modifier.semantics(
    mergeDescendants: Boolean = false,
    properties: (SemanticsPropertyReceiver.() -> Unit)
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "semantics"
        this.properties["mergeDescendants"] = mergeDescendants
        this.properties["properties"] = properties
    }
) {
    val id = remember { SemanticsModifierCore.generateSemanticsId() }
    SemanticsModifierCore(id, mergeDescendants, properties)
}
