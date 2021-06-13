package example.todo.common.database

import com.badoo.reaktive.promise.asSingle
import com.badoo.reaktive.single.Single
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.sqljs.initSqlDriver
import example.todo.database.TodoDatabase

fun todoDatabaseDriver(): Single<SqlDriver> =
    initSqlDriver(TodoDatabase.Schema).asSingle()
