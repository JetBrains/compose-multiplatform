package com.adrianwitaszak.ballast.web.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.BoxSizing
import com.varabyte.kobweb.compose.css.CSSTransition
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.boxSizing
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.compose.ui.modifiers.minWidth
import com.varabyte.kobweb.compose.ui.modifiers.outline
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.transition
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.theme.colors.getColorMode
import com.varabyte.kobweb.silk.theme.toSilkPalette
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.s
import org.jetbrains.compose.web.dom.TextInput


@Composable
fun EditableTextView(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    inputType: InputType.InputTypeWithStringValue = InputType.Text,
) {
    Column(
        modifier = modifier
            .minWidth(250.px)
            .maxWidth(500.px)
            .transition(CSSTransition("scale", 0.2.s))
    ) {
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = modifier.fillMaxSize()
        ) {
            TextInput(
                value = value,
                attrs = Modifier
                    .fillMaxWidth()
                    .padding(20.px)
                    .height(60.px)
                    .backgroundColor(getColorMode().toSilkPalette().background)
                    .borderRadius(14.px)
                    .boxSizing(BoxSizing.BorderBox)
                    .outline(style = LineStyle.None)
                    .border(
                        width = 3.px,
                        style = LineStyle.Solid,
                        color = getColorMode().toSilkPalette().border
                    )
                    .padding(16.px)
                    .toAttrs {
                        onInput {
                            onValueChange(it.value)
                        }
                        type(inputType)
                    }
            )
        }
    }
}
