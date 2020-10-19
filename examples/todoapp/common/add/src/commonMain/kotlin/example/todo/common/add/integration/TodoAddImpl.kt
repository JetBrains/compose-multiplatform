package example.todo.common.add.integration

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.keyInputFilter
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import example.todo.common.add.TodoAdd
import example.todo.common.add.TodoAdd.Dependencies
import example.todo.common.add.store.TodoAddStore.Intent
import example.todo.common.add.store.TodoAddStoreProvider
import example.todo.common.utils.composeState
import example.todo.common.utils.getStore
import example.todo.common.utils.onKeyUp

internal class TodoAddImpl(
    componentContext: ComponentContext,
    dependencies: Dependencies
) : TodoAdd, ComponentContext by componentContext, Dependencies by dependencies {

    private val store =
        instanceKeeper.getStore {
            TodoAddStoreProvider(
                storeFactory = storeFactory,
                database = TodoAddStoreDatabase(queries = database.todoDatabaseQueries)
            ).provide()
        }

    @OptIn(ExperimentalKeyInput::class)
    @Composable
    override fun invoke() {
        val state by store.composeState

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            OutlinedTextField(
                value = state.text,
                modifier = Modifier.weight(weight = 1F).keyInputFilter(onKeyUp(Key.Enter, ::onAddClicked)),
                onValueChange = ::onTextChanged,
                label = { Text(text = "Add a todo") }
            )

            Button(modifier = Modifier.padding(start = 8.dp), onClick = ::onAddClicked) {
                Text(text = "+")
            }
        }
    }

    private fun onTextChanged(text: String) {
        store.accept(Intent.SetText(text = text))
    }

    private fun onAddClicked() {
        store.accept(Intent.Add)
    }
}
