package example.todo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import example.todo.common.edit.TodoEdit
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexFlow
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
fun TodoEditUi(component: TodoEdit) {
    val model by component.models.subscribeAsState()

    Div(
        attrs = {
            style {
                width(100.percent)
                height(100.percent)
                display(DisplayStyle.Flex)
                flexFlow(FlexDirection.Column, FlexWrap.Nowrap)
            }
        }
    ) {
        Div(
            attrs = {
                style {
                    width(100.percent)
                    property("flex", "0 1 auto")
                }
            }
        ) {
            NavBar(
                title = "Edit todo",
                navigationIcon = NavBarIcon(
                    name = "arrow_back",
                    onClick = component::onCloseClicked
                )
            )
        }

        Div(
            attrs = {
                style {
                    width(100.percent)
                    property("flex", "1 1 auto")
                    property("padding", "0px 16px 0px 16px")
                    display(DisplayStyle.Flex)
                    flexFlow(FlexDirection.Column, FlexWrap.Nowrap)
                }
            }
        ) {
            MaterialTextArea(
                id = "text_area_edit_todo",
                label = "",
                text = model.text,
                onTextChanged = component::onTextChanged,
                attrs = {
                    style {
                        width(100.percent)
                        property("flex", "1 1 auto")
                    }
                }
            )
        }

        Div(
            attrs = {
                style {
                    width(100.percent)
                    property("flex", "0 1 auto")
                    property("padding-bottom", "16px")
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.Center)
                }
            }
        ) {
            MaterialCheckbox(
                checked = model.isDone,
                onCheckedChange = component::onDoneChanged,
                content = {
                    Text(value = "Completed")
                }
            )
        }
    }
}

