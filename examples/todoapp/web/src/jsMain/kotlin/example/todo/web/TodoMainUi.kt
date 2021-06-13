package example.todo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import example.todo.common.main.TodoItem
import example.todo.common.main.TodoMain
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexFlow
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.w3c.dom.HTMLUListElement


@Composable
fun TodoMainUi(component: TodoMain) {
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
            NavBar(title = "Todo List")
        }

        Ul(
            attrs = {
                style {
                    width(100.percent)
                    margin(0.px)
                    property("flex", "1 1 auto")
                    property("overflow-y", "scroll")
                }
            }
        ) {
            model.items.forEach { item ->
                Item(
                    item = item,
                    onClicked = component::onItemClicked,
                    onDoneChanged = component::onItemDoneChanged,
                    onDeleteClicked = component::onItemDeleteClicked
                )
            }
        }

        Div(
            attrs = {
                style {
                    width(100.percent)
                    property("flex", "0 1 auto")
                }
            }
        ) {
            TodoInput(
                text = model.text,
                onTextChanged = component::onInputTextChanged,
                onAddClicked = component::onAddItemClicked
            )
        }
    }
}

@Composable
private fun DOMScope<HTMLUListElement>.Item(
    item: TodoItem,
    onClicked: (id: Long) -> Unit,
    onDoneChanged: (id: Long, isDone: Boolean) -> Unit,
    onDeleteClicked: (id: Long) -> Unit
) {
    Li(
        attrs = {
            style {
                width(100.percent)
                display(DisplayStyle.Flex)
                flexFlow(FlexDirection.Row, FlexWrap.Nowrap)
                alignItems(AlignItems.Center)
                property("padding", "0px 0px 0px 16px")
            }
        }
    ) {
        MaterialCheckbox(
            checked = item.isDone,
            onCheckedChange = { onDoneChanged(item.id, !item.isDone) },
            attrs = {
                style {
                    property("flex", "0 1 auto")
                    property("padding-top", 10.px) // Fix for the checkbox not being centered vertically
                }
            }
        )

        Div(
            attrs = {
                style {
                    height(48.px)
                    property("flex", "1 1 auto")
                    property("white-space", "nowrap")
                    property("text-overflow", "ellipsis")
                    property("overflow", "hidden")
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                }
                onClick { onClicked(item.id) }
            }
        ) {
            Text(value = item.text)
        }

        ImageButton(
            onClick = { onDeleteClicked(item.id) },
            iconName = "delete",
            attrs = {
                style {
                    property("flex", "0 1 auto")
                    marginLeft(8.px)
                }
            }
        )
    }
}

@Composable
private fun TodoInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onAddClicked: () -> Unit
) {
    Div(
        attrs = {
            style {
                width(100.percent)
                display(DisplayStyle.Flex)
                flexFlow(FlexDirection.Row, FlexWrap.Nowrap)
                alignItems(AlignItems.Center)
            }
        }
    ) {
        MaterialTextArea(
            id = "text_area_add_todo",
            label = "Add todo",
            text = text,
            onTextChanged = onTextChanged,
            attrs = {
                style {
                    property("flex", "1 1 auto")
                    margin(16.px)
                }
            }
        )

        ImageButton(
            onClick = onAddClicked,
            iconName = "add",
            attrs = {
                style {
                    property("flex", "0 1 auto")
                    property("margin", "0px 16px 0px 0px")
                }
            }
        )
    }
}
