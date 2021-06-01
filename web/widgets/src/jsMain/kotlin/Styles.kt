package org.jetbrains.compose.web.ui

import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.px

import org.jetbrains.compose.web.css.StyleSheet

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
