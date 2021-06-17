package example.todo.common.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import example.todo.common.main.TodoItem
import example.todo.common.main.TodoMain

@Composable
fun TodoMainContent(component: TodoMain) {
    val model by component.models.subscribeAsState()

    Column {
        TopAppBar(title = { Text(text = "Todo List") })

        Box(Modifier.weight(1F)) {
            TodoList(
                items = model.items,
                onItemClicked = component::onItemClicked,
                onDoneChanged = component::onItemDoneChanged,
                onDeleteItemClicked = component::onItemDeleteClicked
            )
        }

        TodoInput(
            text = model.text,
            onAddClicked = component::onAddItemClicked,
            onTextChanged = component::onInputTextChanged
        )
    }
}

@Composable
private fun TodoList(
    items: List<TodoItem>,
    onItemClicked: (id: Long) -> Unit,
    onDoneChanged: (id: Long, isDone: Boolean) -> Unit,
    onDeleteItemClicked: (id: Long) -> Unit
) {
    Box {
        val listState = rememberLazyListState()

        LazyColumn(state = listState) {
            items(items) {
                Item(
                    item = it,
                    onItemClicked = onItemClicked,
                    onDoneChanged = onDoneChanged,
                    onDeleteItemClicked = onDeleteItemClicked
                )

                Divider()
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(
                scrollState = listState,
                itemCount = items.size,
                averageItemSize = 37.dp
            )
        )
    }
}

@Composable
private fun Item(
    item: TodoItem,
    onItemClicked: (id: Long) -> Unit,
    onDoneChanged: (id: Long, isDone: Boolean) -> Unit,
    onDeleteItemClicked: (id: Long) -> Unit
) {
    Row(modifier = Modifier.clickable(onClick = { onItemClicked(item.id) })) {
        Spacer(modifier = Modifier.width(8.dp))

        Checkbox(
            checked = item.isDone,
            modifier = Modifier.align(Alignment.CenterVertically),
            onCheckedChange = { onDoneChanged(item.id, it) }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = AnnotatedString(item.text),
            modifier = Modifier.weight(1F).align(Alignment.CenterVertically),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = { onDeleteItemClicked(item.id) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.width(MARGIN_SCROLLBAR))
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TodoInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onAddClicked: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
        OutlinedTextField(
            value = text,
            modifier = Modifier.weight(weight = 1F).onKeyEvent(onKeyUp(Key.Enter, onAddClicked)),
            onValueChange = onTextChanged,
            label = { Text(text = "Add a todo") }
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onAddClicked) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
        }
    }
}
