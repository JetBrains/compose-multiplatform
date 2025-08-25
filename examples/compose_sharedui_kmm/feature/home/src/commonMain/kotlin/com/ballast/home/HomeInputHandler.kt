package com.ballast.sharedui.root

import com.copperleaf.ballast.InputHandler
import com.copperleaf.ballast.InputHandlerScope

class HomeInputHandler :
    InputHandler<HomeContract.Inputs, HomeContract.Events, HomeContract.State> {
    override suspend fun InputHandlerScope<HomeContract.Inputs, HomeContract.Events, HomeContract.State>.handleInput(
        input: HomeContract.Inputs,
    ) = when (input) {
        is HomeContract.Inputs.OnNameChanged -> {
             updateState { it.copy(name = input.newName) }
        }
    }
}
