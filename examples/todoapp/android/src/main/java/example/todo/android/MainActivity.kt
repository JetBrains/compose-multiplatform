package example.todo.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.rememberRootComponent
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.arkivanov.mvikotlin.timetravel.store.TimeTravelStoreFactory
import example.todo.common.database.DefaultTodoSharedDatabase
import example.todo.common.database.TodoDatabaseDriver
import example.todo.common.root.TodoRoot
import example.todo.common.root.integration.TodoRootComponent
import example.todo.common.ui.TodoRootContent

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComposeAppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    TodoRootContent(rememberRootComponent(::todoRoot))
                }
            }
        }
    }

    private fun todoRoot(componentContext: ComponentContext): TodoRoot =
        TodoRootComponent(
            componentContext = componentContext,
            storeFactory = LoggingStoreFactory(TimeTravelStoreFactory(DefaultStoreFactory)),
            database = DefaultTodoSharedDatabase(TodoDatabaseDriver(context = this))
        )
}
