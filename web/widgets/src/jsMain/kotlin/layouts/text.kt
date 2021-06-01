package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Text as TextNode
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.ui.Styles
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.asAttributeBuilderApplier
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.Color.RGB
import org.jetbrains.compose.common.ui.unit.TextUnit
import org.jetbrains.compose.common.ui.unit.TextUnitType
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.px

@Composable
actual fun TextActual(
    text: String,
    modifier: Modifier,
    color: Color,
    size: TextUnit
) {
    Span(
        modifier.asAttributeBuilderApplier {
            style {
                color(RGB(color.red, color.green, color.blue))
                when (size.unitType) {
                    TextUnitType.Em -> fontSize(size.value.em)
                    TextUnitType.Sp -> fontSize(size.value.px)
                }
            }

            classes(Styles.textClass)
        }
    ) {
        TextNode(text)
    }
}
