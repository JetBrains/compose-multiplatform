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
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * A [Modifier.Element] that adds semantics key/value for use in testing,
 * accessibility, and similar use cases.
 */
@JvmDefaultWithCompatibility
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
    clearAndSetSemantics: Boolean,
    properties: (SemanticsPropertyReceiver.() -> Unit)
) : SemanticsModifier {
    override val semanticsConfiguration: SemanticsConfiguration =
        SemanticsConfiguration().also {
            it.isMergingSemanticsOfDescendants = mergeDescendants
            it.isClearingSemantics = clearAndSetSemantics

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
 * Add semantics key/value pairs to the layout node, for use in testing, accessibility, etc.
 *
 * The provided lambda receiver scope provides "key = value"-style setters for any
 * [SemanticsPropertyKey]. Additionally, chaining multiple semantics modifiers is
 * also a supported style.
 *
 * The resulting semantics produce two [SemanticsNode] trees:
 *
 * The "unmerged tree" rooted at [SemanticsOwner.unmergedRootSemanticsNode] has one
 * [SemanticsNode] per layout node which has any [SemanticsModifier] on it.  This [SemanticsNode]
 * contains all the properties set in all the [SemanticsModifier]s on that node.
 *
 * The "merged tree" rooted at [SemanticsOwner.rootSemanticsNode] has equal-or-fewer nodes: it
 * simplifies the structure based on [mergeDescendants] and [clearAndSetSemantics].  For most
 * purposes (especially accessibility, or the testing of accessibility), the merged semantics
 * tree should be used.
 *
 * @param mergeDescendants Whether the semantic information provided by the owning component and
 * its descendants should be treated as one logical entity.
 * Most commonly set on screen-reader-focusable items such as buttons or form fields.
 * In the merged semantics tree, all descendant nodes (except those themselves marked
 * [mergeDescendants]) will disappear from the tree, and their properties will get merged
 * into the parent's configuration (using a merging algorithm that varies based on the type
 * of property -- for example, text properties will get concatenated, separated by commas).
 * In the unmerged semantics tree, the node is simply marked with
 * [SemanticsConfiguration.isMergingSemanticsOfDescendants].
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
    SemanticsModifierCore(id, mergeDescendants, clearAndSetSemantics = false, properties)
}

/**
 * Clears the semantics of all the descendant nodes and sets new semantics.
 *
 * In the merged semantics tree, this clears the semantic information provided
 * by the node's descendants (but not those of the layout node itself, if any) and sets
 * the provided semantics.  (In the unmerged tree, the semantics node is marked with
 * "[SemanticsConfiguration.isClearingSemantics]", but nothing is actually cleared.)
 *
 * Compose's default semantics provide baseline usability for screen-readers, but this can be
 * used to provide a more polished screen-reader experience: for example, clearing the
 * semantics of a group of tiny buttons, and setting equivalent actions on the card containing them.
 *
 * @param properties properties to add to the semantics. [SemanticsPropertyReceiver] will be
 * provided in the scope to allow access for common properties and its values.
 */
fun Modifier.clearAndSetSemantics(
    properties: (SemanticsPropertyReceiver.() -> Unit)
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "clearAndSetSemantics"
        this.properties["properties"] = properties
    }
) {
    val id = remember { SemanticsModifierCore.generateSemanticsId() }
    SemanticsModifierCore(id, mergeDescendants = false, clearAndSetSemantics = true, properties)
}
