package com.sample.style

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.*

object WtOffsets : StyleSheet(AppStylesheet) {
    val wtTopOffset96 by style {
        marginTop(96.px)
        property(
            "margin-top",
            "calc(4*${AppCSSVariables.wtOffsetTopUnit.value(24.px)})"
        )
    }

    val wtTopOffset24 by style {
        marginTop(24.px)
        property(
            "margin-top",
            "calc(1*${AppCSSVariables.wtOffsetTopUnit.value(24.px)})"
        )
    }

    val wtTopOffset48 by style {
        marginTop(48.px)
    }
    
    val wtTopOffsetSm12 by style {
        media(maxWidth(640.px)) {
            self style {
                marginTop(12.px)
            }
        }
    }

    val wtTopOffsetSm24 by style {
        media(maxWidth(640.px)) {
            self style {
                marginTop(24.px)
            }
        }
    }
}