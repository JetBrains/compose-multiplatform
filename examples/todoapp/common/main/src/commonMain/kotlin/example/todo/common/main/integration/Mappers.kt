package example.todo.common.main.integration

import example.todo.common.main.TodoMain.Model
import example.todo.common.main.store.TodoMainStore.State

internal val stateToModel: (State) -> Model =
    {
        Model(
            items = it.items,
            text = it.text
        )
    }