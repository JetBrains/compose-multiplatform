package example.todo.common.main.integration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.badoo.reaktive.base.Consumer
import com.badoo.reaktive.base.invoke
import example.todo.common.database.TodoSharedDatabase
import example.todo.common.main.TodoMain
import example.todo.common.main.TodoMain.Model
import example.todo.common.main.TodoMain.Output
import example.todo.common.main.store.TodoMainStore.Intent
import example.todo.common.main.store.TodoMainStoreProvider
import example.todo.common.utils.asValue
import example.todo.common.utils.getStore

class TodoMainComponent(
    componentContext: ComponentContext,
    storeFactory: StoreFactory,
    database: TodoSharedDatabase,
    private val output: Consumer<Output>
) : TodoMain, ComponentContext by componentContext {

    private val store =
        instanceKeeper.getStore {
            TodoMainStoreProvider(
                storeFactory = storeFactory,
                database = TodoMainStoreDatabase(database = database)
            ).provide()
        }

    override val models: Value<Model> = store.asValue().map(stateToModel)

    override fun onItemClicked(id: Long) {
        output(Output.Selected(id = id))
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
