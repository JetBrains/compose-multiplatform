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

package androidx.compose.ui.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString

class MockSelectable(
    var getSelectionValue: Selection? = null,
    var getHandlePositionValue: Offset = Offset.Zero,
    var getLayoutCoordinatesValue: LayoutCoordinates? = null,
    var getTextValue: AnnotatedString = AnnotatedString(""),
    var getBoundingBoxValue: Rect = Rect.Zero
) : Selectable {
    val getSelectionValues = mutableListOf<GetSelectionParameters>()
    override fun getSelection(
        startPosition: Offset,
        endPosition: Offset,
        containerLayoutCoordinates: LayoutCoordinates,
        longPress: Boolean,
        previousSelection: Selection?,
        isStartHandle: Boolean
    ): Selection? {
        getSelectionValues += GetSelectionParameters(
            startPosition,
            endPosition,
            containerLayoutCoordinates,
            longPress,
            previousSelection,
            isStartHandle
        )
        return getSelectionValue
    }

    override fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset =
        getHandlePositionValue

    override fun getLayoutCoordinates(): LayoutCoordinates? = getLayoutCoordinatesValue

    override fun getText(): AnnotatedString = getTextValue

    override fun getBoundingBox(offset: Int): Rect = getBoundingBoxValue

    data class GetSelectionParameters(
        val startPosition: Offset,
        val endPosition: Offset,
        val containerLayoutCoordinates: LayoutCoordinates,
        val longPress: Boolean,
        val previousSelection: Selection?,
        val isStartHandle: Boolean
    )
}