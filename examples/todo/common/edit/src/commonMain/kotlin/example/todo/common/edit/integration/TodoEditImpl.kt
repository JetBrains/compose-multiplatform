package example.todo.common.edit.integration

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import example.todo.common.edit.TodoEdit
import example.todo.common.edit.TodoEdit.Dependencies
import example.todo.common.edit.store.TodoEditStore.Intent
import example.todo.common.edit.store.TodoEditStoreProvider
import example.todo.common.utils.composeState
import example.todo.common.utils.getStore

internal class TodoEditImpl(
    componentContext: ComponentContext,
    dependencies: Dependencies
) : TodoEdit, ComponentContext by componentContext, Dependencies by dependencies {

    private val store =
        instanceKeeper.getStore {
            TodoEditStoreProvider(
                storeFactory = storeFactory,
                database = TodoEditStoreDatabase(queries = database.todoDatabaseQueries),
                id = itemId
            ).provide()
        }

    @Composable
    override fun invoke() {
        val state by store.composeState

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TopAppBar(
                title = { Text("Edit todo") },
                navigationIcon = {
                    Button(onClick = ::onFinished) {
                        Text(text = "<")
                    }
                }
            )

            TextField(
                value = state.text,
                modifier = Modifier.weight(1F).fillMaxWidth().padding(8.dp),
                label = { Text("Todo text") },
                onValueChange = ::onTextChanged
            )

            Row(modifier = Modifier.padding(8.dp)) {
                Text(text = "Completed")

                Spacer(modifier = Modifier.width(8.dp))

                Checkbox(
                    checked = state.isDone,
                    onCheckedChange = ::onDoneChanged
                )
            }
        }
    }

    private fun onTextChanged(text: String) {
        store.accept(Intent.SetText(text = text))
    }

    private fun onDoneChanged(isDone: Boolean) {
        store.accept(Intent.SetDone(isDone = isDone))
    }

    private fun onFinished() {
        editOutput.onNext(TodoEdit.Output.Finished)
    }
}
