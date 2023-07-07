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

package androidx.compose.material3.catalog.library

import androidx.compose.material3.catalog.library.model.Components
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.ui.component.Component
import androidx.compose.material3.catalog.library.ui.example.Example
import androidx.compose.material3.catalog.library.ui.home.Home
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun NavGraph(
    theme: Theme,
    onThemeChange: (theme: Theme) -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable(HomeRoute) {
            Home(
                components = Components,
                theme = theme,
                onThemeChange = onThemeChange,
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
                theme = theme,
                onThemeChange = onThemeChange,
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
                theme = theme,
                onThemeChange = onThemeChange,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

const val Material3Route = "material3"
private const val HomeRoute = "home"
private const val ComponentRoute = "component"
private const val ExampleRoute = "example"
private const val ComponentIdArgName = "componentId"
private const val ExampleIndexArgName = "exampleIndex"
