/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.layout.OnPlacedModifier
import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.layout.LookaheadOnPlacedModifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.semantics.SemanticsEntity
import androidx.compose.ui.semantics.SemanticsModifier

/**
 * A collection of [LayoutNodeEntity] elements. [LayoutNodeEntity] is a node in
 * a linked list and this contains the [head] of the list.
 *
 * This linked list structure makes it easier to execute recursing algorithms
 * using the [LayoutNodeEntity.next] without having to allocate any lambdas.
 */
@kotlin.jvm.JvmInline
internal value class EntityList(
    val entities: Array<LayoutNodeEntity<*, *>?> = arrayOfNulls(TypeCount)
) {
    /**
     * Add [LayoutNodeEntity] values for types that [modifier] supports that should be
     * added before the LayoutModifier.
     */
    fun addBeforeLayoutModifier(layoutNodeWrapper: LayoutNodeWrapper, modifier: Modifier) {
        if (modifier is DrawModifier) {
            add(DrawEntity(layoutNodeWrapper, modifier), DrawEntityType.index)
        }
        if (modifier is PointerInputModifier) {
            add(PointerInputEntity(layoutNodeWrapper, modifier), PointerInputEntityType.index)
        }
        if (modifier is SemanticsModifier) {
            add(SemanticsEntity(layoutNodeWrapper, modifier), SemanticsEntityType.index)
        }
        if (modifier is ParentDataModifier) {
            add(SimpleEntity(layoutNodeWrapper, modifier), ParentDataEntityType.index)
        }
    }

    /**
     * Add [LayoutNodeEntity] values that must be added after the LayoutModifier.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun addAfterLayoutModifier(layoutNodeWrapper: LayoutNodeWrapper, modifier: Modifier) {
        if (modifier is OnPlacedModifier) {
            add(SimpleEntity(layoutNodeWrapper, modifier), OnPlacedEntityType.index)
        }
        if (modifier is OnRemeasuredModifier) {
            add(SimpleEntity(layoutNodeWrapper, modifier), RemeasureEntityType.index)
        }
        if (modifier is LookaheadOnPlacedModifier) {
            add(SimpleEntity(layoutNodeWrapper, modifier), LookaheadOnPlacedEntityType.index)
        }
    }

    private fun <T : LayoutNodeEntity<T, *>> add(entity: T, index: Int) {
        @Suppress("UNCHECKED_CAST")
        val head = entities[index] as T?
        entity.next = head
        entities[index] = entity
    }

    /**
     * The head of the linked list of [LayoutNodeEntity] elements of the type [entityType].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : LayoutNodeEntity<T, M>, M : Modifier> head(entityType: EntityType<T, M>): T? =
        entities[entityType.index] as T?

    /**
     * Returns `true` if there are any elements of the given type.
     */
    fun has(entityType: EntityType<*, *>): Boolean = entities[entityType.index] != null

    /**
     * Remove all entries from the list and call [LayoutNodeEntity.onDetach] on them.
     */
    fun clear() {
        forEach {
            if (it.isAttached) {
                it.onDetach()
            }
        }
        for (index in entities.indices) {
            entities[index] = null
        }
    }

    /**
     * Calls [block] on all entries.
     */
    inline fun forEach(block: (LayoutNodeEntity<*, *>) -> Unit) {
        entities.forEach { head ->
            var node = head
            while (node != null) {
                block(node)
                node = node.next
            }
        }
    }

    /**
     * Executes [block] over all entities with [entityType].
     */
    inline fun <T : LayoutNodeEntity<T, M>, M : Modifier> forEach(
        entityType: EntityType<T, M>,
        block: (T) -> Unit
    ) {
        entities[entityType.index].forEach(block)
    }

    private inline fun <T : LayoutNodeEntity<T, M>, M : Modifier> LayoutNodeEntity<*, *>?.forEach(
        block: (T) -> Unit
    ) {
        var node = this
        while (node != null) {
            @Suppress("UNCHECKED_CAST")
            block(node as T)
            node = node.next
        }
    }

    @kotlin.jvm.JvmInline
    value class EntityType<T : LayoutNodeEntity<T, M>, M : Modifier>(val index: Int)

    companion object {
        val DrawEntityType = EntityType<DrawEntity, DrawModifier>(0)
        val PointerInputEntityType = EntityType<PointerInputEntity, PointerInputModifier>(1)
        val SemanticsEntityType = EntityType<SemanticsEntity, SemanticsModifier>(2)
        val ParentDataEntityType =
            EntityType<SimpleEntity<ParentDataModifier>, ParentDataModifier>(3)
        val OnPlacedEntityType =
            EntityType<SimpleEntity<OnPlacedModifier>, OnPlacedModifier>(4)
        val RemeasureEntityType =
            EntityType<SimpleEntity<OnRemeasuredModifier>, OnRemeasuredModifier>(5)
        @OptIn(ExperimentalComposeUiApi::class)
        val LookaheadOnPlacedEntityType =
            EntityType<SimpleEntity<LookaheadOnPlacedModifier>, LookaheadOnPlacedModifier>(
                6
            )

        private const val TypeCount = 7
    }
}
