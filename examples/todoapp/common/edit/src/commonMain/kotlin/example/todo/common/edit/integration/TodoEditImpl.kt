package example.todo.common.edit.integration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.badoo.reaktive.base.invoke
import example.todo.common.edit.TodoEdit
import example.todo.common.edit.TodoEdit.Dependencies
import example.todo.common.edit.TodoEdit.Model
import example.todo.common.edit.TodoEdit.Output
import example.todo.common.edit.store.TodoEditStore.Intent
import example.todo.common.edit.store.TodoEditStoreProvider
import example.todo.common.utils.asValue
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

    override val models: Value<Model> = store.asValue().map(stateToModel)

    override fun onTextChanged(text: String) {
        store.accept(Intent.SetText(text = text))
    }

    override fun onDoneChanged(isDone: Boolean) {
        store.accept(Intent.SetDone(isDone = isDone))
    }

    override fun onCloseClicked() {
        editOutput(Output.Finished)
    }
}
