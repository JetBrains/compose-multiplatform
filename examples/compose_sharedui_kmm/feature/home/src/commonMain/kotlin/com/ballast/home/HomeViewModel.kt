package com.ballast.sharedui.root

import com.copperleaf.ballast.BallastViewModelConfiguration
import com.copperleaf.ballast.build
import com.copperleaf.ballast.core.BasicViewModel
import com.copperleaf.ballast.core.LoggingInterceptor
import com.copperleaf.ballast.core.PrintlnLogger
import com.copperleaf.ballast.eventHandler
import com.copperleaf.ballast.plusAssign
import com.copperleaf.ballast.withViewModel
import kotlinx.coroutines.CoroutineScope

class HomeViewModel(
    viewModelCoroutineScope: CoroutineScope,
) : BasicViewModel<
        HomeContract.Inputs,
        HomeContract.Events,
        HomeContract.State>(
    config = BallastViewModelConfiguration.Builder()
        .apply {
            this += LoggingInterceptor()
            logger = { PrintlnLogger() }
        }
        .withViewModel(
            initialState = HomeContract.State(),
            inputHandler = HomeInputHandler(),
            name = "LoginScreen",
        )
        .build(),
    eventHandler = eventHandler {  },
    coroutineScope = viewModelCoroutineScope,
)
