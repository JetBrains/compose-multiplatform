package example.todo.common.database

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import example.todo.database.TodoDatabase

@Suppress("FunctionName") // FactoryFunction
actual fun TestDatabaseDriver(): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TodoDatabase.Schema.create(driver)

    return driver
}
