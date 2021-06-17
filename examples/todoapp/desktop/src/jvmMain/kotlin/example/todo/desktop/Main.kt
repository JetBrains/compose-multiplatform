package example.todo.desktop

import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.rememberRootComponent
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.badoo.reaktive.coroutinesinterop.asScheduler
import com.badoo.reaktive.scheduler.overrideSchedulers
import example.todo.common.database.DefaultTodoSharedDatabase
import example.todo.common.database.TodoDatabaseDriver
import example.todo.common.root.TodoRoot
import example.todo.common.root.integration.TodoRootComponent
import example.todo.common.ui.TodoRootContent
import kotlinx.coroutines.Dispatchers

fun main() {
    overrideSchedulers(main = Dispatchers.Main::asScheduler)

    Window("Todo") {
        Surface(modifier = Modifier.fillMaxSize()) {
            MaterialTheme {
                DesktopTheme {
                    TodoRootContent(rememberRootComponent(factory = ::todoRoot))
                }
            }
        }
    }
}

private fun todoRoot(componentContext: ComponentContext): TodoRoot =
    TodoRootComponent(
        componentContext = componentContext,
        storeFactory = DefaultStoreFactory,
        database = DefaultTodoSharedDatabase(TodoDatabaseDriver())
    )
