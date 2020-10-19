package example.todo.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.setContent
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.backpressed.toBackPressedDispatched
import com.arkivanov.decompose.instancekeeper.toInstanceKeeper
import com.arkivanov.decompose.lifecycle.asDecomposeLifecycle
import com.arkivanov.decompose.statekeeper.toStateKeeper
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.arkivanov.mvikotlin.timetravel.store.TimeTravelStoreFactory
import example.todo.common.database.TodoDatabaseDriver
import example.todo.common.root.TodoRoot
import example.todo.database.TodoDatabase

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val todoRoot =
            TodoRoot(
                componentContext = DefaultComponentContext(
                    lifecycle = lifecycle.asDecomposeLifecycle(),
                    stateKeeper = savedStateRegistry.toStateKeeper(),
                    instanceKeeper = viewModelStore.toInstanceKeeper(),
                    backPressedDispatcher = onBackPressedDispatcher.toBackPressedDispatched(lifecycle)
                ),
                dependencies = object : TodoRoot.Dependencies {
                    // You can play with time travel using IDEA plugin: https://arkivanov.github.io/MVIKotlin/time_travel.html
                    override val storeFactory: StoreFactory = LoggingStoreFactory(TimeTravelStoreFactory(DefaultStoreFactory))
                    override val database: TodoDatabase = TodoDatabase(TodoDatabaseDriver(this@MainActivity))
                }
            )

        setContent {
            ComposeAppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    todoRoot()
                }
            }
        }
    }
}
