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
     * Returns [Selection] information for a selectable composable. If no selection can be provided
     * null should be returned.
     *
     * @param startPosition graphical position of the start of the selection
     * @param endPosition graphical position of the end of the selection
     * @param containerLayoutCoordinates [LayoutCoordinates] of the widget
     * @param adjustment [Selection] range is adjusted according to this param
     * @param previousSelection previous selection result
     * @param isStartHandle true if the start handle is being dragged
     *
     * @return null if no selection will be applied for this composable, or [Selection] instance
     *  if selection is applied to this composable.
     */
    fun getSelection(
        startPosition: Offset,
        endPosition: Offset,
        containerLayoutCoordinates: LayoutCoordinates,
        adjustment: SelectionAdjustment,
        previousSelection: Selection? = null,
        isStartHandle: Boolean = true
    ): Selection?

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
     * @return the bounding box for the character in [Rect].
     */
    fun getBoundingBox(offset: Int): Rect
}
