package example.todo.common.database

import co.touchlab.sqliter.DatabaseConfiguration
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import com.squareup.sqldelight.drivers.native.wrapConnection
import example.todo.database.TodoDatabase

@Suppress("FunctionName") // Factory function
actual fun TestDatabaseDriver(): SqlDriver {
    val schema = TodoDatabase.Schema

    return NativeSqliteDriver(
        DatabaseConfiguration(
            name = ":memory:",
            version = schema.version,
            create = { wrapConnection(it, schema::create) },
            upgrade = { connection, oldVersion, newVersion ->
                wrapConnection(connection) {
                    schema.migrate(it, oldVersion, newVersion)
                }
            },
            inMemory = true
        )
    )
}