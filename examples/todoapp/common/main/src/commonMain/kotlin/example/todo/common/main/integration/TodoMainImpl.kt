package example.todo.common.main.integration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.badoo.reaktive.base.invoke
import example.todo.common.main.TodoMain
import example.todo.common.main.TodoMain.Dependencies
import example.todo.common.main.TodoMain.Model
import example.todo.common.main.TodoMain.Output
import example.todo.common.main.store.TodoMainStore.Intent
import example.todo.common.main.store.TodoMainStoreProvider
import example.todo.common.utils.asValue
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

    override val models: Value<Model> = store.asValue().map(stateToModel)

    override fun onItemClicked(id: Long) {
        mainOutput(Output.Selected(id = id))
    }

    override fun onItemDoneChanged(id: Long, isDone: Boolean) {
        store.accept(Intent.SetItemDone(id = id, isDone = isDone))
    }

    override fun onItemDeleteClicked(id: Long) {
        store.accept(Intent.DeleteItem(id = id))
    }

    override fun onInputTextChanged(text: String) {
        store.accept(Intent.SetText(text = text))
    }

    override fun onAddItemClicked() {
        store.accept(Intent.AddItem)
    }
}
