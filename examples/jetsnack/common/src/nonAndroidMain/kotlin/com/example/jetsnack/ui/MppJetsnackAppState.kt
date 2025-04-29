package com.example.jetsnack.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import com.example.jetsnack.model.SnackbarManager
import com.example.jetsnack.ui.home.HomeSections
import kotlinx.coroutines.CoroutineScope

@Stable
actual class MppJetsnackAppState(
    actual val scaffoldState: ScaffoldState,
    actual val snackbarManager: SnackbarManager,
    actual val coroutineScope: CoroutineScope,
) {
    actual val bottomBarTabs: Array<HomeSections>
        get() = HomeSections.values()

    private val navigationStack = NavigationStack(HomeSections.FEED.route)

    actual val currentRoute: String?
        get() = navigationStack.lastWithIndex().value


    @Composable
    actual fun shouldShowBottomBar(): Boolean {
        return currentRoute?.startsWith(MainDestinations.SNACK_DETAIL_ROUTE) != true
    }

    actual fun navigateToBottomBarRoute(route: String) {
        navigationStack.replaceBy(route)
    }

    fun navigateToSnackDetail(snackId: Long) {
        navigationStack.push("${MainDestinations.SNACK_DETAIL_ROUTE}/$snackId")
    }

    fun upPress() {
        navigationStack.back()
    }
}

@Composable
actual fun rememberMppJetsnackAppState(): MppJetsnackAppState {
    val scaffoldState = rememberScaffoldState()
    val snackbarManager = SnackbarManager
    val coroutineScope = rememberCoroutineScope()

    return remember(scaffoldState, snackbarManager, coroutineScope) {
        MppJetsnackAppState(scaffoldState, snackbarManager, coroutineScope)
    }
}