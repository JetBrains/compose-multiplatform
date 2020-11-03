package example.todo.common.main.ui

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.keyInputFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.badoo.reaktive.base.Consumer
import example.todo.common.main.TodoMain.Output
import example.todo.common.main.store.TodoItem
import example.todo.common.main.store.TodoMainStore.Intent
import example.todo.common.main.store.TodoMainStore.State
import example.todo.common.utils.compose.MARGIN_SCROLLBAR
import example.todo.common.utils.compose.VerticalScrollbar
import example.todo.common.utils.compose.rememberScrollbarAdapter
import example.todo.common.utils.onKeyUp

@Composable
internal fun TodoMainUi(
    state: State,
    output: Consumer<Output>,
    intents: (Intent) -> Unit
) {
    Column {
        TopAppBar(title = { Text(text = "Todo List") })

        Box(Modifier.weight(1F)) {
            TodoList(
                items = state.items,
                onItemClicked = { output.onNext(Output.Selected(id = it)) },
                onDoneChanged = { id, isDone -> intents(Intent.SetItemDone(id = id, isDone = isDone)) },
                onDeleteItemClicked = { intents(Intent.DeleteItem(id = it)) }
            )
        }

        TodoInput(
            text = state.text,
            onAddClicked = { intents(Intent.AddItem) },
            onTextChanged = { intents(Intent.SetText(text = it)) }
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

        LazyColumnFor(items = items, state = listState) {
            Item(
                item = it,
                onItemClicked = onItemClicked,
                onDoneChanged = onDoneChanged,
                onDeleteItemClicked = onDeleteItemClicked
            )

            Divider()
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
            Icon(Icons.Default.Delete)
        }

        Spacer(modifier = Modifier.width(MARGIN_SCROLLBAR))
    }
}

@OptIn(ExperimentalKeyInput::class)
@Composable
private fun TodoInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onAddClicked: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
        OutlinedTextField(
            value = text,
            modifier = Modifier.weight(weight = 1F).keyInputFilter(onKeyUp(Key.Enter, onAddClicked)),
            onValueChange = onTextChanged,
            label = { Text(text = "Add a todo") }
        )

        Button(modifier = Modifier.padding(start = 8.dp), onClick = onAddClicked) {
            Text(text = "+")
        }
    }
}
