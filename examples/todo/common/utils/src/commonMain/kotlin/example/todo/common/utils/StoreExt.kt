package example.todo.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.extensions.reaktive.states
import com.badoo.reaktive.observable.subscribe

@Composable
val <T : Any> Store<*, T, *>.composeState: State<T>
    get() {
        val composeState = remember(this) { mutableStateOf(state) }
        val disposable = states.subscribe(onNext = { composeState.value = it })
        onDispose(disposable::dispose)

        return composeState
    }
