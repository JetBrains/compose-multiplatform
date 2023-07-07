/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.integration.docs.navigation

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/navigation
 *
 * No action required if it's modified.
 */

@Composable
private fun NavigationSnippet1() {
    val navController = rememberNavController()
}

@Composable
private fun NavigationSnippet2(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "profile") {
        composable("profile") { Profile(/*...*/) }
        composable("friendslist") { FriendsList(/*...*/) }
        /*...*/
    }
}

private object NavigationSnippet3 {
    @Composable
    fun Profile(navController: NavController) {
        /*...*/
        Button(onClick = { navController.navigate("friends") }) {
            Text(text = "Navigate next")
        }
        /*...*/
    }
}

@Composable
private fun NavigationSnippet4(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "profile/{userId}") {
        /*...*/
        composable("profile/{userId}") { /*...*/ }
    }
}

@Composable
private fun NavigationSnippet5(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "profile/{userId}") {
        /*...*/
        composable(
            "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { /*...*/ }
    }
}

private fun NavGraphBuilder.NavigationSnippet6(navController: NavHostController) {
    composable("profile/{userId}") { backStackEntry ->
        Profile(navController, backStackEntry.arguments?.getString("userId"))
    }
}

@Composable
private fun NavigationSnippet7(navController: NavHostController) {
    navController.navigate("profile/user1234")
}

private fun NavGraphBuilder.NavigationSnippet8(navController: NavHostController) {
    composable(
        "profile?userId={userId}",
        arguments = listOf(navArgument("userId") { defaultValue = "me" })
    ) { backStackEntry ->
        Profile(navController, backStackEntry.arguments?.getString("userId"))
    }
}

/* Deep links */

private fun NavGraphBuilder.NavigationSnippet9(navController: NavHostController) {
    val uri = "https://example.com"

    composable(
        "profile?id={id}",
        deepLinks = listOf(navDeepLink { uriPattern = "$uri/{id}" })
    ) { backStackEntry ->
        Profile(navController, backStackEntry.arguments?.getString("id"))
    }
}

@Composable
private fun NavigationSnippet10() {
    val id = "exampleId"
    val context = LocalContext.current
    val deepLinkIntent = Intent(
        Intent.ACTION_VIEW,
        "https://example.com/$id".toUri(),
        context,
        MyActivity::class.java
    )

    val deepLinkPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(deepLinkIntent)
        getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

@Composable
private fun NavigationSnippet11(items: List<Screen>) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    BottomNavigationItem(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text(stringResource(screen.resourceId)) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            // This is the equivalent to popUpTo the start destination
                            navController.popBackStack(
                                navController.graph.startDestinationId, false
                            )

                            // This if check gives us a "singleTop" behavior where we do not create a
                            // second instance of the composable if we are already on that destination
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Profile.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Profile.route) { Profile(navController) }
            composable(Screen.FriendsList.route) { FriendsList(navController) }
        }
    }
}

private fun NavGraphBuilder.NavigationSnippet12(navController: NavHostController) {
    composable(
        "profile?userId={userId}",
        arguments = listOf(navArgument("userId") { defaultValue = "me" })
    ) { backStackEntry ->
        Profile(backStackEntry.arguments?.getString("userId")) { friendUserId ->
            navController.navigate("profile?userId=$friendUserId")
        }
    }
}

/*
Fakes needed for snippets to build:
 */

private object R {
    object string {
        const val profile = 0
        const val friends_list = 1
    }
}

@Composable
private fun Profile() {
}

@Composable
private fun Profile(userId: String?, content: @Composable (String) -> Unit) {
}

@Composable
private fun Profile(navController: NavHostController) {
}

@Composable
private fun FriendsList() {
}

@Composable
private fun FriendsList(navController: NavHostController) {
}

@Composable
private fun Profile(navController: NavHostController, arg: String?) {
    TODO()
}

private class MyActivity
private sealed class Screen(val route: String, @StringRes val resourceId: Int) {
    object Profile : Screen("profile", R.string.profile)
    object FriendsList : Screen("friendslist", R.string.friends_list)
}
