package example.todo.common.main.store

internal data class TodoItem(
    val id: Long = 0L,
    val order: Long = 0L,
    val text: String = "",
    val isDone: Boolean = false
)
