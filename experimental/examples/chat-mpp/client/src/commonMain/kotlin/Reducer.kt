import kotlinx.coroutines.flow.StateFlow

/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */


sealed interface Action {
    data class SendMessage(val message: Message) : Action
}

data class State(
    val messages: List<Message> = emptyList()
)

fun chatReducer(state: State, action: Action): State =
    when (action) {
        is Action.SendMessage -> {
            state.copy(
                messages = state.messages + action.message
            )
        }
    }

interface Store {
    fun send(action: Action)
    val stateFlow: StateFlow<State>
    val state get() = stateFlow.value
}
