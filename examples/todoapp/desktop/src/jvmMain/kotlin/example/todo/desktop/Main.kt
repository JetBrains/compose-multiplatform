package example.todo.desktop

import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.lifecycle.LifecycleRegistry
import com.arkivanov.decompose.lifecycle.resume
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.badoo.reaktive.coroutinesinterop.asScheduler
import com.badoo.reaktive.scheduler.overrideSchedulers
import example.todo.common.database.TodoDatabaseDriver
import example.todo.common.root.TodoRoot
import example.todo.database.TodoDatabase
import kotlinx.coroutines.Dispatchers

fun main() {
    overrideSchedulers(main = Dispatchers.Main::asScheduler)

    val lifecycle = LifecycleRegistry()
    lifecycle.resume()

    Window("Todo") {
        Surface(modifier = Modifier.fillMaxSize()) {
            MaterialTheme {
                DesktopTheme {
                    TodoRoot(
                        componentContext = DefaultComponentContext(lifecycle),
                        dependencies = object : TodoRoot.Dependencies {
                            override val storeFactory = DefaultStoreFactory
                            override val database = TodoDatabase(TodoDatabaseDriver())
                        }
                    ).invoke()
                }
            }
        }
    }
}
