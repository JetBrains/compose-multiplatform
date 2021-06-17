package example.todo.web

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.lifecycle.LifecycleRegistry
import com.arkivanov.decompose.lifecycle.resume
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import example.todo.common.database.DefaultTodoSharedDatabase
import example.todo.common.database.todoDatabaseDriver
import example.todo.common.root.integration.TodoRootComponent
import kotlinx.browser.document
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.ui.Styles
import org.w3c.dom.HTMLElement

fun main() {
    val rootElement = document.getElementById("root") as HTMLElement

    val lifecycle = LifecycleRegistry()

    val root =
        TodoRootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            storeFactory = DefaultStoreFactory,
            database = DefaultTodoSharedDatabase(todoDatabaseDriver())
        )

    lifecycle.resume()

    renderComposable(root = rootElement) {
        Style(Styles)

        TodoRootUi(root)
    }
}
