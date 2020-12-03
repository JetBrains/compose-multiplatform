package example.todo.common.database

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import example.todo.database.TodoDatabase

@Suppress("FunctionName") // Factory function
fun TodoDatabaseDriver(): SqlDriver =
    NativeSqliteDriver(TodoDatabase.Schema, "TodoDatabase.db")
