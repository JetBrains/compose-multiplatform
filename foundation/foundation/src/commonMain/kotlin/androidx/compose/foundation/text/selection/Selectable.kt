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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange

/**
 * Provides [Selection] information for a composable to SelectionContainer. Composables who can
 * be selected should subscribe to [SelectionRegistrar] using this interface.
 */

internal interface Selectable {
    /**
     * An ID used by [SelectionRegistrar] to identify this [Selectable]. This value should not be
     * [SelectionRegistrar.InvalidSelectableId].
     * When a [Selectable] is created, it can request an ID from [SelectionRegistrar] by
     * calling [SelectionRegistrar.nextSelectableId].
     * @see SelectionRegistrar.nextSelectableId
     */
    val selectableId: Long

    /**
     * Updates the [Selection] information after a selection handle being moved. This method is
     * expected to be called consecutively during the selection handle position update.
     *
     * @param startHandlePosition graphical position of the start selection handle
     * @param endHandlePosition graphical position of the end selection handle
     * @param previousHandlePosition the previous position of the moving selection handle
     * @param containerLayoutCoordinates [LayoutCoordinates] of the composable
     * @param adjustment [Selection] range is adjusted according to this param
     * @param previousSelection previous selection result on this [Selectable]
     * @param isStartHandle whether the moving selection handle is the start selection handle
     *
     * @throws IllegalStateException when the given [previousSelection] doesn't belong to this
     * selectable. In other words, one of the [Selection.AnchorInfo] in the given
     * [previousSelection] has a selectableId that doesn't match to the [selectableId] of this
     * selectable.
     * @return a pair consisting of the updated [Selection] and a boolean value representing
     * whether the movement is consumed.
     */
    fun updateSelection(
        startHandlePosition: Offset,
        endHandlePosition: Offset,
        previousHandlePosition: Offset?,
        isStartHandle: Boolean = true,
        containerLayoutCoordinates: LayoutCoordinates,
        adjustment: SelectionAdjustment,
        previousSelection: Selection? = null
    ): Pair<Selection?, Boolean>

    /**
     * Returns selectAll [Selection] information for a selectable composable. If no selection can be
     * provided null should be returned.
     *
     * @return selectAll [Selection] information for a selectable composable. If no selection can be
     * provided null should be returned.
     */
    fun getSelectAllSelection(): Selection?

    /**
     * Return the [Offset] of a [SelectionHandle].
     *
     * @param selection [Selection] contains the [SelectionHandle]
     * @param isStartHandle true if it's the start handle, false if it's the end handle.
     *
     * @return [Offset] of this handle, based on which the [SelectionHandle] will be drawn.
     */
    fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset

    /**
     * Return the [LayoutCoordinates] of the [Selectable].
     *
     * @return [LayoutCoordinates] of the [Selectable]. This could be null if called before
     * composing.
     */
    fun getLayoutCoordinates(): LayoutCoordinates?

    /**
     * Return the [AnnotatedString] of the [Selectable].
     *
     * @return text content as [AnnotatedString] of the [Selectable].
     */
    fun getText(): AnnotatedString

    /**
     * Return the bounding box of the character for given character offset. This is currently for
     * text.
     * In future when we implemented other selectable Composables, we can return the bounding box of
     * the wanted rectangle. For example, for an image selectable, this should return the
     * bounding box of the image.
     *
     * @param offset a character offset
     * @return the bounding box for the character in [Rect], or [Rect.Zero] if the selectable is
     * empty.
     */
    fun getBoundingBox(offset: Int): Rect

    /**
     * Return the offsets of the start and end of the line containing [offset], or [TextRange.Zero]
     * if the selectable is empty. These offsets are in the same "coordinate space" as
     * [getBoundingBox], and despite being returned in a [TextRange], may not refer to offsets in
     * actual text if the selectable contains other types of content.
     */
    fun getRangeOfLineContaining(offset: Int): TextRange
}
