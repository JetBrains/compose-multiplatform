/*
 * Copied from Decompose
 */

package example.todo.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.UiSavedStateRegistry
import androidx.compose.runtime.savedinstancestate.UiSavedStateRegistryAmbient
import com.arkivanov.decompose.RouterState
import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.value.Value

private typealias SavedState = Map<String, List<Any?>>

@Composable
fun <C : Parcelable, T : Any> Value<RouterState<C, T>>.children(render: @Composable() (child: T, configuration: C) -> Unit) {
    val parentRegistry: UiSavedStateRegistry? = UiSavedStateRegistryAmbient.current
    val children = remember { Children<C>() }

    if (parentRegistry != null) {
        onDispose {
            children.inactive.entries.forEach { (key, value) ->
                parentRegistry.unregisterProvider(key, value.provider)
            }
            children.active?.also {
                parentRegistry.unregisterProvider(it.key, it.provider)
            }
        }
    }

    invoke { state ->
        val activeChildConfiguration = state.activeChild.configuration

        val currentChild: ActiveChild<C>? = children.active
        if ((currentChild != null) && state.backStack.any { it.configuration === currentChild.configuration }) {
            parentRegistry?.unregisterProvider(currentChild.key, currentChild.provider)
            val inactiveChild = InactiveChild(configuration = currentChild.configuration, savedState = currentChild.provider())
            children.inactive[currentChild.key] = inactiveChild
            parentRegistry?.registerProvider(currentChild.key, inactiveChild.provider)
        }

        val activeChildRegistry: UiSavedStateRegistry

        if (currentChild?.configuration === activeChildConfiguration) {
            activeChildRegistry = currentChild.registry
        } else {
            val key = activeChildConfiguration.toString()

            val savedChild: InactiveChild<C>? = children.inactive.remove(key)
            if (savedChild != null) {
                parentRegistry?.unregisterProvider(key, savedChild.provider)
            }
            @Suppress("UNCHECKED_CAST")
            val savedState: SavedState? = savedChild?.savedState ?: parentRegistry?.consumeRestored(key) as SavedState?

            activeChildRegistry = UiSavedStateRegistry(savedState) { true }

            val newActiveChild = ActiveChild(configuration = activeChildConfiguration, key = key, registry = activeChildRegistry)
            children.active = newActiveChild
            parentRegistry?.registerProvider(key, newActiveChild.provider)
        }

        children.inactive.entries.removeAll { (key, value) ->
            val remove = state.backStack.none { it.configuration === value.configuration }
            if (remove) {
                parentRegistry?.unregisterProvider(key, value.provider)
            }
            remove
        }

        Providers(UiSavedStateRegistryAmbient provides activeChildRegistry) {
            render(state.activeChild.component, activeChildConfiguration)
        }
    }
}

private class Children<C : Parcelable> {
    val inactive: MutableMap<String, InactiveChild<C>> = HashMap()
    var active: ActiveChild<C>? = null
}

private class ActiveChild<out C : Parcelable>(
    val configuration: C,
    val key: String,
    val registry: UiSavedStateRegistry
) {
    val provider: () -> SavedState = registry::performSave
}

private class InactiveChild<out C : Parcelable>(
    val configuration: C,
    val savedState: SavedState
) {
    val provider: () -> SavedState = ::savedState
}
