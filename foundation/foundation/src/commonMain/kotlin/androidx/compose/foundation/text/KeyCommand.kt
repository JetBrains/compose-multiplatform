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

package androidx.compose.foundation.text

internal enum class KeyCommand(
    // Indicates, that this command is supposed to edit text so should be applied only to
    // editable text fields
    val editsText: Boolean
) {
    LEFT_CHAR(false),
    RIGHT_CHAR(false),

    RIGHT_WORD(false),
    LEFT_WORD(false),

    NEXT_PARAGRAPH(false),
    PREV_PARAGRAPH(false),

    LINE_START(false),
    LINE_END(false),
    LINE_LEFT(false),
    LINE_RIGHT(false),

    UP(false),
    DOWN(false),

    PAGE_UP(false),
    PAGE_DOWN(false),

    HOME(false),
    END(false),

    COPY(false),
    PASTE(true),
    CUT(true),

    DELETE_PREV_CHAR(true),
    DELETE_NEXT_CHAR(true),

    DELETE_PREV_WORD(true),
    DELETE_NEXT_WORD(true),

    DELETE_FROM_LINE_START(true),
    DELETE_TO_LINE_END(true),

    SELECT_ALL(false),

    SELECT_LEFT_CHAR(false),
    SELECT_RIGHT_CHAR(false),

    SELECT_UP(false),
    SELECT_DOWN(false),

    SELECT_PAGE_UP(false),
    SELECT_PAGE_DOWN(false),

    SELECT_HOME(false),
    SELECT_END(false),

    SELECT_LEFT_WORD(false),
    SELECT_RIGHT_WORD(false),
    SELECT_NEXT_PARAGRAPH(false),
    SELECT_PREV_PARAGRAPH(false),

    SELECT_LINE_START(false),
    SELECT_LINE_END(false),
    SELECT_LINE_LEFT(false),
    SELECT_LINE_RIGHT(false),

    DESELECT(false),

    NEW_LINE(true),
    TAB(true),

    UNDO(true),
    REDO(true)
}