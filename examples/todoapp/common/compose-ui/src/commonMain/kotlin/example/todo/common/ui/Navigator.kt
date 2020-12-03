package example.todo.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.onActive
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.AmbientUiSavedStateRegistry
import androidx.compose.runtime.savedinstancestate.UiSavedStateRegistry
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.asState
import com.arkivanov.decompose.lifecycle.Lifecycle
import com.arkivanov.decompose.lifecycle.LifecycleRegistry
import com.arkivanov.decompose.lifecycle.destroy
import com.arkivanov.decompose.lifecycle.resume
import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.ParcelableContainer
import com.arkivanov.decompose.statekeeper.StateKeeper
import com.arkivanov.decompose.statekeeper.StateKeeperDispatcher
import kotlin.reflect.KClass

@Composable
fun <T : Parcelable> Navigator(
    initialConfiguration: () -> T,
    configurationClass: KClass<out T>,
    content: @Composable Router<T>.(T) -> Unit
) {
    val lifecycle = getLifecycle()
    val stateKeeper = getStateKeeper()

    val context = remember { DefaultComponentContext(lifecycle = lifecycle, stateKeeper = stateKeeper) }

    val router =
        remember {
            val decomposeRouter =
                context.router(
                    initialConfiguration = initialConfiguration,
                    configurationClass = configurationClass,
                    componentFactory = { configuration, _ -> configuration }
                )

            object : Router<T>, com.arkivanov.decompose.Router<T, T> by decomposeRouter {}
        }

    val routerState by router.state.asState()
    router.content(routerState.activeChild.configuration)
}

@Composable
private fun getLifecycle(): Lifecycle {
    val lifecycle = remember { LifecycleRegistry() }
    onActive { lifecycle.resume() }
    onDispose { lifecycle.destroy() }

    return lifecycle
}

@Composable
private fun getStateKeeper(key: String = "state"): StateKeeper {
    val savedStateRegistry: UiSavedStateRegistry? = AmbientUiSavedStateRegistry.current

    val dispatcher =
        remember {
            StateKeeperDispatcher(savedStateRegistry?.consumeRestored(key) as ParcelableContainer?)
        }

    if (savedStateRegistry != null) {
        val stateProvider = dispatcher::save
        onActive { savedStateRegistry.registerProvider(key, stateProvider) }
        onDispose { savedStateRegistry.unregisterProvider(key, stateProvider) }
    }

    return dispatcher
}

interface Router<in T : Parcelable> {
    fun push(configuration: T)
    fun pop()
}
