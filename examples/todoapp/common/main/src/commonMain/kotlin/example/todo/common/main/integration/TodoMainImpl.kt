package example.todo.common.main.integration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ComponentContext
import example.todo.common.main.TodoMain
import example.todo.common.main.TodoMain.Dependencies
import example.todo.common.main.TodoMain.Output
import example.todo.common.main.store.TodoMainStore.Intent
import example.todo.common.main.store.TodoMainStore.State
import example.todo.common.main.store.TodoMainStoreProvider
import example.todo.common.main.ui.TodoMainUi
import example.todo.common.utils.composeState
import example.todo.common.utils.getStore

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

        TodoMainUi(
            state = state,
            output = mainOutput,
            intents = store::accept
        )
    }

    internal fun onIntent(intent: Intent) {
        store.accept(intent)
    }

    internal fun onOutput(output: Output) {
        mainOutput.onNext(output)
    }
}
