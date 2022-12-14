package example.todoapp.lite.common

internal data class TodoItem(
    val id: Long = 0L,
    val text: String = "",
    val isDone: Boolean = false
)
