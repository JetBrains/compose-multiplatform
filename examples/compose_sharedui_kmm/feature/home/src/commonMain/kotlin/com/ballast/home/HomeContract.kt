package com.ballast.sharedui.root

object HomeContract {
    data class State(
        val name: String = "",
    )

    sealed interface Inputs {
        data class OnNameChanged(val newName: String) : Inputs
    }

    sealed interface Events {
    }
}
