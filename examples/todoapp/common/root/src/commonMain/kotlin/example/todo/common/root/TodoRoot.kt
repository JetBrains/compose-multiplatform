package example.todo.common.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import example.todo.common.edit.TodoEdit
import example.todo.common.main.TodoMain

interface TodoRoot {

    val childStack: Value<ChildStack<*, Child>>

    sealed class Child {
        data class Main(val component: TodoMain) : Child()
        data class Edit(val component: TodoEdit) : Child()
    }
}
