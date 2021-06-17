package example.todo.web

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.material.Text
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.checked
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.I
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Nav
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.jetbrains.compose.web.dom.Ul
import org.w3c.dom.HTMLUListElement

@Composable
fun MaterialCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    attrs: AttrBuilderContext<*> = {},
    content: @Composable () -> Unit = {}
) {
    Div(attrs = attrs) {
        Label {
            Input(
                type = InputType.Checkbox,
                attrs = {
                    classes("filled-in")
                    if (checked) checked()
                    onCheckboxInput { onCheckedChange(it.checked) }
                }
            )

            Span {
                content()
            }
        }
    }
}

@Composable
fun Card(attrs: AttrBuilderContext<*> = {}, content: @Composable () -> Unit) {
    Div(
        attrs = {
            classes("card")
            attrs()
        }
    ) {
        content()
    }
}

@Composable
fun MaterialTextArea(
    id: String,
    label: String,
    text: String,
    onTextChanged: (String) -> Unit,
    attrs: AttrBuilderContext<*> = {}
) {
    Div(
        attrs = {
            classes("input-field", "col", "s12")
            attrs()
        }
    ) {
        TextArea(
            value = text,
            attrs = {
                id("text_area_add_todo")
                classes("materialize-textarea")
                onTextInput { onTextChanged(it.inputValue) }
                style {
                    width(100.percent)
                    height(100.percent)
                }
            }
        )

        Label(forId = id) {
            Text(text = label)
        }
    }
}

@Composable
fun ImageButton(
    onClick: () -> Unit,
    iconName: String,
    attrs: AttrBuilderContext<*> = {}
) {
    A(
        attrs = {
            classes("waves-effect", "waves-teal", "btn-flat")
            style {
                width(48.px)
                height(48.px)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.Center)
            }
            this.onClick { onClick() }
            attrs()
        }
    ) {
        MaterialIcon(name = iconName)
    }
}

@Composable
fun MaterialIcon(name: String) {
    I(attrs = { classes("material-icons") }) { Text(value = name) }
}

@Composable
fun NavBar(
    title: String,
    navigationIcon: NavBarIcon? = null
) {
    Nav {
        Div(attrs = { classes("nav-wrapper") }) {
            if (navigationIcon != null) {
                Ul(attrs = { classes("left") }) {
                    NavBarIcon(icon = navigationIcon)
                }
            }

            A(
                attrs = {
                    classes("brand-logo")
                    style {
                        property("padding-left", 16.px)
                    }
                }
            ) {
                Text(value = title)
            }
        }
    }
}

@Composable
private fun ElementScope<HTMLUListElement>.NavBarIcon(icon: NavBarIcon) {
    Li {
        A(
            attrs = {
                onClick { icon.onClick() }
            }
        ) {
            MaterialIcon(name = icon.name)
        }
    }
}

class NavBarIcon(
    val name: String,
    val onClick: () -> Unit
)
