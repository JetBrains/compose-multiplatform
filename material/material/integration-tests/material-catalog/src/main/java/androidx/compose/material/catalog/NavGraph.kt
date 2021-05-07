/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material.catalog

import androidx.compose.material.catalog.model.Components
import androidx.compose.material.catalog.model.Theme
import androidx.compose.material.catalog.ui.component.Component
import androidx.compose.material.catalog.ui.example.Example
import androidx.compose.material.catalog.ui.home.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController

@Composable
fun NavGraph(
    theme: Theme,
    onThemeChange: (theme: Theme) -> Unit
) {
    // Using rememberUpdatedState as hoisted state is not correctly propagated to NavHost
    // https://issuetracker.google.com/issues/177338143
    val navTheme = rememberUpdatedState(theme)
    val navOnThemeChange = rememberUpdatedState(onThemeChange)
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable(HomeRoute) {
            Home(
                components = Components,
                theme = navTheme.value,
                onThemeChange = navOnThemeChange.value,
                onComponentClick = { component ->
                    val componentId = component.id
                    val route = "$ComponentRoute/$componentId"
                    navController.navigate(route)
                }
            )
        }
        composable(
            route = "$ComponentRoute/" +
                "{$ComponentIdArgName}",
            arguments = listOf(
                navArgument(ComponentIdArgName) { type = NavType.IntType }
            )
        ) { navBackStackEntry ->
            val arguments = requireNotNull(navBackStackEntry.arguments) { "No arguments" }
            val componentId = arguments.getInt(ComponentIdArgName)
            val component = Components.first { component -> component.id == componentId }
            Component(
                component = component,
                theme = navTheme.value,
                onThemeChange = navOnThemeChange.value,
                onExampleClick = { example ->
                    val exampleIndex = component.examples.indexOf(example)
                    val route = "$ExampleRoute/$componentId/$exampleIndex"
                    navController.navigate(route)
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = "$ExampleRoute/" +
                "{$ComponentIdArgName}/" +
                "{$ExampleIndexArgName}",
            arguments = listOf(
                navArgument(ComponentIdArgName) { type = NavType.IntType },
                navArgument(ExampleIndexArgName) { type = NavType.IntType }
            )
        ) { navBackStackEntry ->
            val arguments = requireNotNull(navBackStackEntry.arguments) { "No arguments" }
            val componentId = arguments.getInt(ComponentIdArgName)
            val exampleIndex = arguments.getInt(ExampleIndexArgName)
            val component = Components.first { component -> component.id == componentId }
            val example = component.examples[exampleIndex]
            Example(
                component = component,
                example = example,
                theme = navTheme.value,
                onThemeChange = navOnThemeChange.value,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private const val HomeRoute = "home"
private const val ComponentRoute = "component"
private const val ExampleRoute = "example"
private const val ComponentIdArgName = "componentId"
private const val ExampleIndexArgName = "exampleIndex"
