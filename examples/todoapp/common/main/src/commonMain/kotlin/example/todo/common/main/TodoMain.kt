package example.todo.common.main

import com.arkivanov.decompose.value.Value

interface TodoMain {

    val models: Value<Model>

    fun onItemClicked(id: Long)

    fun onItemDoneChanged(id: Long, isDone: Boolean)

    fun onItemDeleteClicked(id: Long)

    fun onInputTextChanged(text: String)

    fun onAddItemClicked()

    data class Model(
        val items: List<TodoItem>,
        val text: String
    )

    sealed class Output {
        data class Selected(val id: Long) : Output()
    }
}
