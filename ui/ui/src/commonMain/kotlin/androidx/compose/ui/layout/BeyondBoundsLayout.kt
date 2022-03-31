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

/**
 * A modifier local that provides access to a [BeyondBoundsLayout] that a child can use to
 * ask a parent to layout more items that are beyond its visible bounds.
 */
val ModifierLocalBeyondBoundsLayout: ProvidableModifierLocal<BeyondBoundsLayout> =
    modifierLocalOf { DefaultBeyondBoundsLayout }

private val DefaultBeyondBoundsLayout = object : BeyondBoundsLayout {
    override fun requestBeyondBoundsLayout(
        direction: BeyondBoundsLayoutDirection,
        block: () -> Boolean,
    ): Boolean {
        // Do nothing, just invoke the lambda.
        return block.invoke()
    }
}

/**
 * Send a request to layout extra items in the specified direction.
 *
 * A [BeyondBoundsLayout] instance can be obtained by consuming the
 * [BeyondBoundsLayoutRequester modifier local][ModifierLocalBeyondBoundsLayout].
 * It can be used to send a request to layout more items in a particular
 * [direction][BeyondBoundsLayoutDirection]. This can be useful when composition or layout
 * is determined lazily, as with a LazyColumn. The request is received by any parent up
 * the hierarchy that provides this modifier local.
 */
interface BeyondBoundsLayout {
    /**
     * Send a request to layout of more items in the specified
     * [direction][BeyondBoundsLayoutDirection]. The request is received by a parent up the hierarchy
     * by the parent that provided the modifier local.
     *
     * @param direction The direction from the visible bounds in which more items are requested.
     * @param block Continue to add layout more items until this block returns true or there are no
     * more items to add.
     * @return whether the terminating condition was satisfied. It returns true if we stopped adding
     * items because [block] returned true, and false if we stopped for other reasons. (eg. We ran
     * out of items to add).
     * The items are only guaranteed to be present within the scope of the block and the parent
     * can dispose the extra items as soon as this block finishes executing.
     */
    fun requestBeyondBoundsLayout(
        direction: BeyondBoundsLayoutDirection,
        block: () -> Boolean
    ): Boolean
}

/**
 * The direction (from the visible bounds) that a [BeyondBoundsLayout] is requesting more
 * items to be laid.
 */
@kotlin.jvm.JvmInline
value class BeyondBoundsLayoutDirection internal constructor(
    @Suppress("unused") private val value: Int
) {
    companion object {
        /**
         * Direction used in [BeyondBoundsLayout.requestBeyondBoundsLayout] to request the layout of
         * extra items before the current bounds.
         */
        val Before = BeyondBoundsLayoutDirection(1)
        /**
         * Direction used in [BeyondBoundsLayout.requestBeyondBoundsLayout] to request the layout of
         * extra items after the current bounds.
         */
        val After = BeyondBoundsLayoutDirection(2)
        /**
         * Direction used in [BeyondBoundsLayout.requestBeyondBoundsLayout] to request the layout of
         * extra items to the left of the current bounds.
         */
        val Left = BeyondBoundsLayoutDirection(3)
        /**
         * Direction used in [BeyondBoundsLayout.requestBeyondBoundsLayout] to request the layout of
         * extra items to the right of the current bounds.
         */
        val Right = BeyondBoundsLayoutDirection(4)
        /**
         * Direction used in [BeyondBoundsLayout.requestBeyondBoundsLayout] to request the layout of
         * extra items above the current bounds.
         */
        val Above = BeyondBoundsLayoutDirection(5)
        /**
         * Direction used in [BeyondBoundsLayout.requestBeyondBoundsLayout] to request the layout of
         * extra items below the current bounds.
         */
        val Below = BeyondBoundsLayoutDirection(6)
    }

    override fun toString(): String = when (this) {
        Before -> "Before"
        After -> "After"
        Left -> "Left"
        Right -> "Right"
        Above -> "Above"
        Below -> "Below"
        else -> "invalid BeyondBoundsLayoutDirection"
    }
}
