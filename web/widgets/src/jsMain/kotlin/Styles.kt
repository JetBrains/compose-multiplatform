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
