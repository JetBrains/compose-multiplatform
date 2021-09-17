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

import androidx.compose.material.catalog.library.MaterialCatalogApp
import androidx.compose.material.catalog.library.MaterialRoute
import androidx.compose.material.catalog.model.MaterialSpecification
import androidx.compose.material.catalog.model.Material3Specification
import androidx.compose.material.catalog.model.Specifications
import androidx.compose.material.catalog.ui.specification.Specification
import androidx.compose.material3.catalog.library.Material3CatalogApp
import androidx.compose.material3.catalog.library.Material3Route
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SpecificationRoute
    ) {
        composable(SpecificationRoute) {
            Specification(
                specifications = Specifications,
                onSpecificationClick = { specification ->
                    when (specification) {
                        MaterialSpecification -> navController.navigate(MaterialRoute)
                        Material3Specification -> navController.navigate(Material3Route)
                        else -> throw IllegalArgumentException("Unknown specification")
                    }
                }
            )
        }
        composable(MaterialRoute) { MaterialCatalogApp() }
        composable(Material3Route) { Material3CatalogApp() }
    }
}

private const val SpecificationRoute = "specification"
