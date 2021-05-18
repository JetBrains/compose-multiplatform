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
package org.jetbrains.compose.web.ui

import androidx.compose.web.css.justifyContent
import androidx.compose.web.css.JustifyContent
import androidx.compose.web.css.alignItems
import androidx.compose.web.css.AlignItems
import androidx.compose.web.css.flexDirection
import androidx.compose.web.css.FlexDirection
import androidx.compose.web.css.display
import androidx.compose.web.css.DisplayStyle
import androidx.compose.web.css.left
import androidx.compose.web.css.px

import androidx.compose.web.css.StyleSheet

object Styles : StyleSheet() {
    val columnClass = "compose-web-column"

    val textClass by style {
        display(DisplayStyle.Block)
        left(0.px)
    }

    val rowClass by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Row)
    }

    val composeWebArrangementHorizontalStart by style {
        justifyContent(JustifyContent.FlexStart)
    }

    val composeWebArrangementHorizontalEnd by style {
        justifyContent(JustifyContent.FlexEnd)
    }

    val composeWebAlignmentVerticalTop by style {
        alignItems(AlignItems.FlexStart)
    }

    val composeWebAlignmentVerticalCenter by style {
        alignItems(AlignItems.Center)
    }

    val composeWebAlignmentVerticalBottom by style {
        alignItems(AlignItems.FlexEnd)
    }
}