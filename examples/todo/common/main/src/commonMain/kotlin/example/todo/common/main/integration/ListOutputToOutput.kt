package example.todo.common.main.integration

import example.todo.common.list.TodoList
import example.todo.common.main.TodoMain.Output

internal val listOutputToOutput: TodoList.Output.() -> Output? =
    {
        when (this) {
            is TodoList.Output.ItemSelected -> Output.Selected(id = id)
        }
    }
