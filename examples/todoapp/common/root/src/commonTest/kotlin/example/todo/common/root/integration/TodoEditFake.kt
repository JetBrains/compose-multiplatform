package example.todo.common.root.integration

import com.arkivanov.decompose.value.Value
import com.badoo.reaktive.base.Consumer
import example.todo.common.edit.TodoEdit
import example.todo.common.edit.TodoEdit.Model
import example.todo.common.edit.TodoEdit.Output

class TodoEditFake(
    val itemId: Long,
    val output: Consumer<Output>
) : TodoEdit {

    override val models: Value<Model> get() = TODO("Not used")

    override fun onTextChanged(text: String) {
    }

    override fun onDoneChanged(isDone: Boolean) {
    }

    override fun onCloseClicked() {
    }
}