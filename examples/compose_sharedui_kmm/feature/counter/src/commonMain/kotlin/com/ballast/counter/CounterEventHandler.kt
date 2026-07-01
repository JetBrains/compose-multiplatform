package com.ballast.sharedui.root

import com.copperleaf.ballast.EventHandler
import com.copperleaf.ballast.EventHandlerScope

class CounterEventHandler :
    EventHandler<CounterContract.Inputs, CounterContract.Events, CounterContract.State> {
    override suspend fun EventHandlerScope<CounterContract.Inputs, CounterContract.Events, CounterContract.State>.handleEvent(
        event: CounterContract.Events,
    ) {

    }
}
