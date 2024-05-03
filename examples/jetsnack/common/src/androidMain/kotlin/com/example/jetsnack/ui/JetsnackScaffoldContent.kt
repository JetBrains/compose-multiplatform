package com.example.jetsnack.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.jetsnack.ui.home.Feed
import com.example.jetsnack.ui.home.HomeSections
import com.example.jetsnack.ui.home.Profile
import com.example.jetsnack.ui.home.cart.Cart
import com.example.jetsnack.ui.home.search.Search
import com.example.jetsnack.ui.snackdetail.SnackDetail

@Composable
actual fun JetsnackScaffoldContent(
    innerPaddingModifier: PaddingValues,
    appState: MppJetsnackAppState
) {
    NavHost(
        navController = appState.navController,
        startDestination = MainDestinations.HOME_ROUTE,
        modifier = Modifier.padding(innerPaddingModifier)
    ) {
        jetsnackNavGraph(
            onSnackSelected = appState::navigateToSnackDetail,
            upPress = appState::upPress
        )
    }
}

private fun NavGraphBuilder.jetsnackNavGraph(
    onSnackSelected: (Long, NavBackStackEntry) -> Unit,
    upPress: () -> Unit,
) {
    navigation(
        route = MainDestinations.HOME_ROUTE,
        startDestination = HomeSections.FEED.route
    ) {
        addHomeGraph(onSnackSelected)
    }
    composable(
        "${MainDestinations.SNACK_DETAIL_ROUTE}/{${MainDestinations.SNACK_ID_KEY}}",
        arguments = listOf(navArgument(MainDestinations.SNACK_ID_KEY) { type = NavType.LongType })
    ) { backStackEntry ->
        val arguments = requireNotNull(backStackEntry.arguments)
        val snackId = arguments.getLong(MainDestinations.SNACK_ID_KEY)
        SnackDetail(snackId, upPress, onSnackClick = { onSnackSelected(snackId, backStackEntry) })
    }
}

fun NavGraphBuilder.addHomeGraph(
    onSnackSelected: (Long, NavBackStackEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    composable(HomeSections.FEED.route) { from ->
        Feed(onSnackClick = { id -> onSnackSelected(id, from) }, modifier)
    }
    composable(HomeSections.SEARCH.route) { from ->
        Search(onSnackClick = { id -> onSnackSelected(id, from) }, modifier)
    }
    composable(HomeSections.CART.route) { from ->
        Cart(onSnackClick = { id -> onSnackSelected(id, from) }, modifier)
    }
    composable(HomeSections.PROFILE.route) {
        Profile(modifier)
    }
}