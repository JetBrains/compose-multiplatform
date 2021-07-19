package org.jetbrains.compose.web.ui

import org.jetbrains.compose.web.css.*

object Styles : StyleSheet() {
    val columnClass = "compose-web-column"

    val textClass by style {
        display(Block)
        left(0.px)
    }

    val rowClass by style {
        display(Flex)
        flexDirection(Row)
    }

    val composeWebArrangementHorizontalStart by style {
        justifyContent(FlexStart)
    }

    val composeWebArrangementHorizontalEnd by style {
        justifyContent(FlexEnd)
    }

    val composeWebAlignmentVerticalTop by style {
        alignItems(FlexStart)
    }

    val composeWebAlignmentVerticalCenter by style {
        alignItems(Center)
    }

    val composeWebAlignmentVerticalBottom by style {
        alignItems(FlexEnd)
    }
}
