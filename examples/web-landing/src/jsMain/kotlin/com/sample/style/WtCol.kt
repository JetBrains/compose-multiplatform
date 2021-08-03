package com.sample.style

import org.jetbrains.compose.web.css.CSSBuilder
import org.jetbrains.compose.web.css.CSSUnitValue
import org.jetbrains.compose.web.css.GenericStyleSheetBuilder
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.media
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.mediaMaxWidth
import org.jetbrains.compose.web.css.selectors.CSSSelector

fun <TBuilder> GenericStyleSheetBuilder<TBuilder>.mediaMaxWidth(
    value: CSSUnitValue,
    cssSelector: CSSSelector,
    rulesBuild: TBuilder.() -> Unit
) {
    media(mediaMaxWidth(value)) {
        cssSelector style rulesBuild
    }
}

fun CSSBuilder.forMaxWidth(value: CSSUnitValue, builder: CSSBuilder.() -> Unit) {
    mediaMaxWidth(value, self, builder)
}

object WtCols : StyleSheet(AppStylesheet) {
    val wtCol2 by style {
        AppCSSVariables.wtColCount(2)
    }

    val wtCol3 by style {
        AppCSSVariables.wtColCount(3)
    }

    val wtCol4 by style {
        AppCSSVariables.wtColCount(4)
    }

    val wtCol5 by style {
        AppCSSVariables.wtColCount(5)
    }

    val wtCol6 by style {
        AppCSSVariables.wtColCount(6)
    }

    val wtCol9 by style {
        AppCSSVariables.wtColCount(9)
    }

    val wtCol10 by style {
        AppCSSVariables.wtColCount(10)
    }

    val wtColMd3 by style {
        forMaxWidth(1000.px) {
            AppCSSVariables.wtColCount(3)
        }
    }

    val wtColMd4 by style {
        forMaxWidth(1000.px) {
            AppCSSVariables.wtColCount(4)
        }
    }

    val wtColMd8 by style {
        forMaxWidth(1000.px) {
            AppCSSVariables.wtColCount(8)
        }
    }

    val wtColMd9 by style {
        forMaxWidth(1000.px) {
            AppCSSVariables.wtColCount(9)
        }
    }

    val wtColMd10 by style {
        forMaxWidth(1000.px) {
            AppCSSVariables.wtColCount(10)
        }
    }

    val wtColMd11 by style {
        forMaxWidth(1000.px) {
            AppCSSVariables.wtColCount(11)
        }
    }

    val wtColMd6 by style {
        forMaxWidth(1000.px) {
            AppCSSVariables.wtColCount(6)
        }
    }

    val wtColMd12 by style {
        forMaxWidth(1000.px) {
            AppCSSVariables.wtColCount(12)
        }
    }

    val wtColSm12 by style {
        forMaxWidth(640.px) {
            AppCSSVariables.wtColCount(12)
        }
    }

    val wtColLg6 by style {
        forMaxWidth(1276.px) {
            AppCSSVariables.wtColCount(6)
        }
    }

    val wtColSmAutoFill by style {
        forMaxWidth(640.px) {
            AppCSSVariables.wtColCount(0)
            flexGrow(1)
            maxWidth(100.percent)
        }
    }

    val wtColAutoFill by style {
        AppCSSVariables.wtColCount(0)
        flexGrow(1)
        maxWidth(100.percent)
    }

    val wtColInline by style {
        AppCSSVariables.wtColCount(0)
        maxWidth(100.percent)
        property("flex-basis", "auto")
    }
}
