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

package androidx.compose.ui.layout

import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf
import kotlin.jvm.JvmInline

/**
 * A modifier local that provides access to a [BeyondBoundsLayout] that a child can use to
 * ask a parent to layout more items that are beyond its visible bounds.
 */
val ModifierLocalBeyondBoundsLayout: ProvidableModifierLocal<BeyondBoundsLayout?> =
    modifierLocalOf { null }

/**
 * Layout extra items in the specified direction.
 *
 * A [BeyondBoundsLayout] instance can be obtained by consuming the
 * [BeyondBoundsLayout modifier local][ModifierLocalBeyondBoundsLayout].
 * It can be used to send a request to layout more items in a particular
 * [direction][LayoutDirection]. This can be useful when composition or layout is determined lazily,
 * as with a LazyColumn. The request is received by any parent up the hierarchy that provides this
 * modifier local.
 */
interface BeyondBoundsLayout {
    /**
     * Send a request to layout more items in the specified
     * [direction][LayoutDirection]. The request is received by a parent up the
     * hierarchy. The parent adds one item at a time and calls [block] after each item is added.
     * The parent continues adding new items as long as [block] returns null. Once you have all
     * the items you need, you can perform some operation and return a non-null value. Returning
     * this value stops the laying out of beyond bounds items. (Note that you have to return a
     * non-null value stop iterating).
     *
     * @param direction The direction from the visible bounds in which more items are requested.
     * @param block Continue to layout more items until this block returns a non null item.
     * @return The value returned by the last run of [block]. If we layout all the available
     * items then the returned value is null. When this function returns all the beyond bounds items
     * may be disposed. Therefore you have to perform any custom logic within the [block] and return
     * the value you need.
     */
    fun <T> layout(
        direction: LayoutDirection,
        block: BeyondBoundsScope.() -> T?
    ): T?

    /**
     * The scope used in [BeyondBoundsLayout.layout].
     */
    interface BeyondBoundsScope {
        /**
         * Whether we have more content to lay out in the specified direction.
         */
        val hasMoreContent: Boolean
    }

    /**
     * The direction (from the visible bounds) that a [BeyondBoundsLayout] is requesting more items
     * to be laid.
     */
    @JvmInline
    value class LayoutDirection internal constructor(
        @Suppress("unused") private val value: Int
    ) {
        companion object {
            /**
             * Direction used in [BeyondBoundsLayout.layout] to request the layout of extra items
             * before the current bounds.
             */
            val Before = LayoutDirection(1)
            /**
             * Direction used in [BeyondBoundsLayout.layout] to request the layout of extra items
             * after the current bounds.
             */
            val After = LayoutDirection(2)
            /**
             * Direction used in [BeyondBoundsLayout.layout] to request the layout of extra items
             * to the left of the current bounds.
             */
            val Left = LayoutDirection(3)
            /**
             * Direction used in [BeyondBoundsLayout.layout] to request the layout of extra items
             * to the right of the current bounds.
             */
            val Right = LayoutDirection(4)
            /**
             * Direction used in [BeyondBoundsLayout.layout] to request the layout of extra items
             * above the current bounds.
             */
            val Above = LayoutDirection(5)
            /**
             * Direction used in [BeyondBoundsLayout.layout] to request the layout of extra items
             * below the current bounds.
             */
            val Below = LayoutDirection(6)
        }

        override fun toString(): String = when (this) {
            Before -> "Before"
            After -> "After"
            Left -> "Left"
            Right -> "Right"
            Above -> "Above"
            Below -> "Below"
            else -> "invalid LayoutDirection"
        }
    }
}
