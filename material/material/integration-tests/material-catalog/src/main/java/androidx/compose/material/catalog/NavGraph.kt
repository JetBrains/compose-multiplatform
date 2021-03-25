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
import androidx.compose.material.catalog.ui.component.Component
import androidx.compose.material.catalog.ui.example.Example
import androidx.compose.material.catalog.ui.home.Home
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE
    ) {
        composable(HOME_ROUTE) {
            Home(
                components = Components,
                onComponentClick = { component ->
                    val componentId = component.id
                    val route = "$COMPONENT_ROUTE/$componentId"
                    navController.navigate(route)
                }
            )
        }
        composable(
            route = "$COMPONENT_ROUTE/" +
                "{$COMPONENT_ID_ARG_NAME}",
            arguments = listOf(
                navArgument(COMPONENT_ID_ARG_NAME) { type = NavType.IntType }
            )
        ) { navBackStackEntry ->
            val arguments = requireNotNull(navBackStackEntry.arguments) { "No arguments" }
            val componentId = arguments.getInt(COMPONENT_ID_ARG_NAME)
            val component = Components.first { component -> component.id == componentId }
            Component(
                component = component,
                onExampleClick = { example ->
                    val exampleIndex = component.examples.indexOf(example)
                    val route = "$EXAMPLE_ROUTE/$componentId/$exampleIndex"
                    navController.navigate(route)
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = "$EXAMPLE_ROUTE/" +
                "{$COMPONENT_ID_ARG_NAME}/" +
                "{$EXAMPLE_INDEX_ARG_NAME}",
            arguments = listOf(
                navArgument(COMPONENT_ID_ARG_NAME) { type = NavType.IntType },
                navArgument(EXAMPLE_INDEX_ARG_NAME) { type = NavType.IntType }
            )
        ) { navBackStackEntry ->
            val arguments = requireNotNull(navBackStackEntry.arguments) { "No arguments" }
            val componentId = arguments.getInt(COMPONENT_ID_ARG_NAME)
            val exampleIndex = arguments.getInt(EXAMPLE_INDEX_ARG_NAME)
            val component = Components.first { component -> component.id == componentId }
            val example = component.examples[exampleIndex]
            Example(
                example = example,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private const val HOME_ROUTE = "home"
private const val COMPONENT_ROUTE = "component"
private const val EXAMPLE_ROUTE = "example"
private const val COMPONENT_ID_ARG_NAME = "componentId"
private const val EXAMPLE_INDEX_ARG_NAME = "exampleIndex"
