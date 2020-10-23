package example.todo.common.edit.integration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ComponentContext
import example.todo.common.edit.TodoEdit
import example.todo.common.edit.TodoEdit.Dependencies
import example.todo.common.edit.store.TodoEditStoreProvider
import example.todo.common.edit.ui.TodoEditUi
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

        TodoEditUi(
            state = state,
            output = editOutput,
            intents = store::accept
        )
    }
}
