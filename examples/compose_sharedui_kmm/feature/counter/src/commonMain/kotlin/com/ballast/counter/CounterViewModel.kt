package com.ballast.sharedui.root

import com.copperleaf.ballast.BallastViewModelConfiguration
import com.copperleaf.ballast.build
import com.copperleaf.ballast.core.BasicViewModel
import com.copperleaf.ballast.core.LoggingInterceptor
import com.copperleaf.ballast.core.PrintlnLogger
import com.copperleaf.ballast.plusAssign
import com.copperleaf.ballast.withViewModel
import kotlinx.coroutines.CoroutineScope

class CounterViewModel(
    viewModelCoroutineScope: CoroutineScope,
) : BasicViewModel<
        CounterContract.Inputs,
        CounterContract.Events,
        CounterContract.State>(
    config = BallastViewModelConfiguration.Builder()
        .apply {
            this += LoggingInterceptor()
            logger = { PrintlnLogger() }
        }
        .withViewModel(
            initialState = CounterContract.State(),
            inputHandler = CounterInputHandler(),
            name = "LoginScreen",
        )
        .build(),
    eventHandler = CounterEventHandler(),
    coroutineScope = viewModelCoroutineScope,
)
