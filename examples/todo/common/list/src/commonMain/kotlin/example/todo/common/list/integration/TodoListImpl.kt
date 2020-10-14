package example.todo.common.list.integration

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import example.todo.common.list.TodoList
import example.todo.common.list.TodoList.Dependencies
import example.todo.common.list.TodoList.Output
import example.todo.common.list.store.TodoListStore
import example.todo.common.list.store.TodoListStore.Intent
import example.todo.common.list.store.TodoListStoreProvider
import example.todo.common.utils.composeState
import example.todo.common.utils.getStore

internal class TodoListImpl(
    componentContext: ComponentContext,
    dependencies: Dependencies
) : TodoList, ComponentContext by componentContext, Dependencies by dependencies {

    private val store =
        instanceKeeper.getStore {
            TodoListStoreProvider(
                storeFactory = storeFactory,
                database = TodoListStoreDatabase(queries = database.todoDatabaseQueries)
            ).provide()
        }

    internal val state: TodoListStore.State get() = store.state

    @Composable
    override fun invoke() {
        val state by store.composeState

        LazyColumnFor(items = state.items) { item ->
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

    internal fun onItemClicked(id: Long) {
        listOutput.onNext(Output.ItemSelected(id = id))
    }

    internal fun onDoneChanged(id: Long, isDone: Boolean) {
        store.accept(Intent.SetDone(id = id, isDone = isDone))
    }
}
