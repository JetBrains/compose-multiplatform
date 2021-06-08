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
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.checked
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Input
import org.w3c.dom.HTMLInputElement

@Composable
actual fun EditTextActual(
  text: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier,
  color: Color,
  size: TextUnit
) {
  Input(
    type = InputType.Text,
    value = text,
    attrs = {

      onInput {
        val updated = (it.nativeEvent.target as HTMLInputElement).value
        console.log("updated2 $updated")
        onValueChange(updated)
      }

    })
}
