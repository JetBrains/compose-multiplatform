package example.todo.web

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import example.todo.common.database.DefaultTodoSharedDatabase
import example.todo.common.database.todoDatabaseDriver
import example.todo.common.root.integration.TodoRootComponent
import kotlinx.browser.document
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLElement

fun main() {
    val rootElement = document.getElementById("root") as HTMLElement

    val lifecycle = LifecycleRegistry()

    val root =
        TodoRootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            storeFactory = DefaultStoreFactory(),
            database = DefaultTodoSharedDatabase(todoDatabaseDriver())
        )

    lifecycle.resume()

    renderComposable(root = rootElement) {
        TodoRootUi(root)
    }
}
