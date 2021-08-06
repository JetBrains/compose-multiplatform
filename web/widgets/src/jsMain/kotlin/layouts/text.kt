package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.asAttributeBuilderApplier
import org.jetbrains.compose.common.ui.unit.TextUnit
import org.jetbrains.compose.common.ui.unit.TextUnitType
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.ui.Styles
import org.jetbrains.compose.web.dom.Text as TextNode

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
                color(rgba(color.red, color.green, color.blue, color.alpha))
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
