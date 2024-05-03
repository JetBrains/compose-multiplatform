package com.example.jetsnack.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import com.example.jetsnack.ui.home.CartTodo
import com.example.jetsnack.ui.home.Feed
import com.example.jetsnack.ui.home.HomeSections
import com.example.jetsnack.ui.home.Profile
import com.example.jetsnack.ui.home.cart.Cart
import com.example.jetsnack.ui.home.search.Search
import com.example.jetsnack.ui.snackdetail.SnackDetail

@OptIn(ExperimentalAnimationApi::class)
@Composable
actual fun JetsnackScaffoldContent(
    innerPaddingModifier: PaddingValues,
    appState: MppJetsnackAppState
) {

    when (appState.currentRoute) {
        HomeSections.FEED.route -> {
            Feed(
                onSnackClick = appState::navigateToSnackDetail,
                modifier = Modifier.padding(innerPaddingModifier)
            )
        }

        HomeSections.SEARCH.route -> {
            Search(
                onSnackClick = appState::navigateToSnackDetail,
                modifier = Modifier.padding(innerPaddingModifier)
            )
        }

        HomeSections.CART.route -> {
            Cart(
                onSnackClick = appState::navigateToSnackDetail,
                modifier = Modifier.padding(innerPaddingModifier)
            )
        }

        HomeSections.PROFILE.route -> {
            Profile(modifier = Modifier.padding(innerPaddingModifier))
        }

        else -> {
            val snackId = appState.currentRoute?.takeIf {
                it.startsWith(MainDestinations.SNACK_DETAIL_ROUTE + "/")
            }?.let {
                it.split("/")[1].toLongOrNull()
            }
            if (snackId != null) {
                SnackDetail(snackId, appState::upPress, appState::navigateToSnackDetail)
            }
        }
    }
}

class NavigationStack<T>(initial: T) {
    private val stack = mutableStateListOf(initial)
    fun push(t: T) {
        stack.add(t)
    }

    fun replaceBy(t: T) {
        stack.removeLast()
        stack.add(t)
    }

    fun back() {
        if(stack.size > 1) {
            // Always keep one element on the view stack
            stack.removeLast()
        }
    }

    fun lastWithIndex() = stack.withIndex().last()
}