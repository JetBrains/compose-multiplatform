package example.todo.common.edit.integration

import example.todo.common.edit.TodoEdit.Model
import example.todo.common.edit.store.TodoEditStore.State

internal val stateToModel: (State) -> Model =
    {
        Model(
            text = it.text,
            isDone = it.isDone
        )
    }
