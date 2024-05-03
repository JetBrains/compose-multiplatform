/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetsnack.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.jetsnack.ui.components.JetsnackScaffold
import com.example.jetsnack.ui.components.JetsnackSnackbar
import com.example.jetsnack.ui.home.JetsnackBottomBar
import com.example.jetsnack.ui.snackdetail.jetSnackSystemBarsPadding
import com.example.jetsnack.ui.theme.JetsnackTheme

@Composable
fun JetsnackApp() {
    JetsnackTheme {
        val appState = rememberMppJetsnackAppState()
        JetsnackScaffold(
            bottomBar = {
                if (appState.shouldShowBottomBar()) {
                    JetsnackBottomBar(
                        tabs = appState.bottomBarTabs,
                        currentRoute = appState.currentRoute!!,
                        navigateToRoute = appState::navigateToBottomBarRoute
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = it,
                    modifier = Modifier.jetSnackSystemBarsPadding(),
                    snackbar = { snackbarData -> JetsnackSnackbar(snackbarData) }
                )
            },
            scaffoldState = appState.scaffoldState
        ) { innerPaddingModifier ->
            JetsnackScaffoldContent(innerPaddingModifier, appState)
        }
    }
}

@Composable
expect fun JetsnackScaffoldContent(innerPaddingModifier: PaddingValues, appState: MppJetsnackAppState)

