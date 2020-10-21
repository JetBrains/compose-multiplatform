package example.todo.common.main.integration

import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.keyInputFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import example.todo.common.main.TodoMain
import example.todo.common.main.TodoMain.Dependencies
import example.todo.common.main.TodoMain.Output
import example.todo.common.main.store.TodoItem
import example.todo.common.main.store.TodoMainStore.Intent
import example.todo.common.main.store.TodoMainStore.State
import example.todo.common.main.store.TodoMainStoreProvider
import example.todo.common.utils.composeState
import example.todo.common.utils.getStore
import example.todo.common.utils.onKeyUp

internal class TodoMainImpl(
    componentContext: ComponentContext,
    dependencies: Dependencies
) : TodoMain, ComponentContext by componentContext, Dependencies by dependencies {

    private val store =
        instanceKeeper.getStore {
            TodoMainStoreProvider(
                storeFactory = storeFactory,
                database = TodoMainStoreDatabase(queries = database.todoDatabaseQueries)
            ).provide()
        }

    internal val state: State get() = store.state

    @Composable
    override fun invoke() {
        val state by store.composeState

        Column {
            TopAppBar(title = { Text(text = "Todo List") })

            Box(Modifier.weight(1F)) {
                TodoList(items = state.items)
            }

            TodoInput(text = state.text)
        }
    }

    @Composable
    private fun TodoList(items: List<TodoItem>) {
        LazyColumnFor(items = items) { item ->
            Row(modifier = Modifier.clickable(onClick = { onItemClicked(id = item.id) }).padding(8.dp)) {
                Text(
                    text = AnnotatedString(item.text),
                    modifier = Modifier.weight(1F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Checkbox(
                    checked = item.isDone,
                    onCheckedChange = { onDoneChanged(id = item.id, isDone = it) }
                )
            }

            Divider()
        }
    }

    @OptIn(ExperimentalKeyInput::class)
    @Composable
    private fun TodoInput(text: String) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            OutlinedTextField(
                value = text,
                modifier = Modifier.weight(weight = 1F).keyInputFilter(onKeyUp(Key.Enter, ::onAddClicked)),
                onValueChange = ::onTextChanged,
                label = { Text(text = "Add a todo") }
            )

            Button(modifier = Modifier.padding(start = 8.dp), onClick = ::onAddClicked) {
                Text(text = "+")
            }
        }
    }

    internal fun onItemClicked(id: Long) {
        mainOutput.onNext(Output.Selected(id = id))
    }

    internal fun onDoneChanged(id: Long, isDone: Boolean) {
        store.accept(Intent.SetItemDone(id = id, isDone = isDone))
    }

    internal fun onTextChanged(text: String) {
        store.accept(Intent.SetText(text = text))
    }

    internal fun onAddClicked() {
        store.accept(Intent.AddItem)
    }
}
