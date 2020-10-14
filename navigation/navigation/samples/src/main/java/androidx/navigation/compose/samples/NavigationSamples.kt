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

package androidx.navigation.compose.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonConstants
import androidx.compose.material.Divider
import androidx.navigation.compose.AmbientNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class Screen(val title: String) {
    object Profile : Screen("Profile")
    object Dashboard : Screen("Dashboard")
    object Scrollable : Screen("Scrollable")
}

@Sampled
@Composable
fun BasicNav() {
    NavHost(startDestination = "Profile") {
        composable("Profile") { Profile() }
        composable("Dashboard") { Dashboard() }
        composable("Scrollable") { Scrollable() }
    }
}

@Composable
fun Profile() {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = Screen.Profile.title)
        NavigateButton(Screen.Dashboard)
        Divider(color = Color.Black)
        NavigateButton(Screen.Scrollable)
        Spacer(Modifier.weight(1f))
        NavigateBackButton()
    }
}

@Composable
fun Dashboard() {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = Screen.Dashboard.title)
        Spacer(Modifier.weight(1f))
        NavigateBackButton()
    }
}

@Composable
fun Scrollable() {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        NavigateButton(Screen.Dashboard)
        ScrollableColumn(Modifier.weight(1f)) {
            phrases.forEach { phrase ->
                Text(phrase, fontSize = 30.sp)
            }
        }
        NavigateBackButton()
    }
}

@Sampled
@Composable
fun NavigateButton(screen: Screen) {
    val navController = AmbientNavController.current
    Button(
        onClick = { navController.navigate(screen.title) },
        colors = ButtonConstants.defaultButtonColors(backgroundColor = LightGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Navigate to " + screen.title)
    }
}

@Composable
fun NavigateBackButton() {
    val navController = AmbientNavController.current
    if (navController.previousBackStackEntry != null) {
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonConstants.defaultButtonColors(backgroundColor = LightGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Go to Previous screen")
        }
    }
}

private val phrases = listOf(
    "Easy As Pie",
    "Wouldn't Harm a Fly",
    "No-Brainer",
    "Keep On Truckin'",
    "An Arm and a Leg",
    "Down To Earth",
    "Under the Weather",
    "Up In Arms",
    "Cup Of Joe",
    "Not the Sharpest Tool in the Shed",
    "Ring Any Bells?",
    "Son of a Gun",
    "Hard Pill to Swallow",
    "Close But No Cigar",
    "Beating a Dead Horse",
    "If You Can't Stand the Heat, Get Out of the Kitchen",
    "Cut To The Chase",
    "Heads Up",
    "Goody Two-Shoes",
    "Fish Out Of Water",
    "Cry Over Spilt Milk",
    "Elephant in the Room",
    "There's No I in Team",
    "Poke Fun At",
    "Talk the Talk",
    "Know the Ropes",
    "Fool's Gold",
    "It's Not Brain Surgery",
    "Fight Fire With Fire",
    "Go For Broke"
)
