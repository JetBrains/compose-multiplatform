package com.ballast.sharedui.root

object CounterContract {
    data class State(
        val count: Int = 0,
        val timerCount: Int = 0
    )

    sealed interface Inputs {
        data class Increment(val amount: Int = 1) : Inputs
        data class Decrement(val amount: Int = 1) : Inputs
    }

    sealed interface Events {
    }
}
