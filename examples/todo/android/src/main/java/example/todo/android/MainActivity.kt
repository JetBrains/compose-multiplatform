/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
