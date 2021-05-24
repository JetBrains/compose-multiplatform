package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import androidx.compose.web.elements.Text as TextNode
import androidx.compose.web.elements.Span
import org.jetbrains.compose.web.ui.Styles
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.asStyleBuilderApplier
import org.jetbrains.compose.common.ui.asAttributeBuilderApplier
import org.jetbrains.compose.common.core.graphics.Color
import androidx.compose.web.css.color
import androidx.compose.web.css.fontSize
import androidx.compose.web.css.Color.RGB
import org.jetbrains.compose.common.ui.unit.TextUnit
import org.jetbrains.compose.common.ui.unit.TextUnitType
import androidx.compose.web.css.em
import androidx.compose.web.css.px

@Composable
actual fun TextActual(
    text: String,
    modifier: Modifier,
    color: Color,
    size: TextUnit
) {
    Span(
        style = modifier.asStyleBuilderApplier() {
            color(RGB(color.red, color.green, color.blue))
            when (size.unitType) {
                TextUnitType.Em -> fontSize(size.value.em)
                TextUnitType.Sp -> fontSize(size.value.px)
            }
        },
        attrs = modifier.asAttributeBuilderApplier() {
            classes(Styles.textClass)
        }
    ) {
        TextNode(text)
    }
}
