package example.todo.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.rootComponent
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.arkivanov.mvikotlin.timetravel.store.TimeTravelStoreFactory
import example.todo.common.database.TodoDatabaseDriver
import example.todo.common.root.TodoRoot
import example.todo.common.ui.TodoRootContent
import example.todo.database.TodoDatabase

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComposeAppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    TodoRootContent(rootComponent(::todoRoot))
                }
            }
        }
    }

    private fun todoRoot(componentContext: ComponentContext): TodoRoot =
        TodoRoot(
            componentContext = componentContext,
            dependencies = object : TodoRoot.Dependencies {
                // You can play with time travel using IDEA plugin: https://arkivanov.github.io/MVIKotlin/time_travel.html
                override val storeFactory: StoreFactory = LoggingStoreFactory(TimeTravelStoreFactory(DefaultStoreFactory))
                override val database: TodoDatabase = TodoDatabase(TodoDatabaseDriver(this@MainActivity))
            }
        )
}
