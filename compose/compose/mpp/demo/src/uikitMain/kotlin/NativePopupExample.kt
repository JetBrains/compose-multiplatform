import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UINavigationController
import platform.UIKit.navigationController

/*
 * Copyright 2023 The Android Open Source Project
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

val NativeModalWithNaviationExample = Screen.Example("Native modal with navigation") {
    NativeModalWithNavigation()
}
@Composable
private fun NativeModalWithNavigation() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val viewController = LocalUIViewController.current
        Button(onClick = {
            val navigationController = UINavigationController(rootViewController = ComposeUIViewController {
                NativeNavigationPage()
            })

            viewController.presentViewController(navigationController, true, null)
        }) {
            Text("Present popup")
        }
    }
}

@Composable
private fun NativeNavigationPage() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        val navigationController = LocalUIViewController.current.navigationController

        Button(onClick = {
            navigationController?.pushViewController(
                ComposeUIViewController {
                    NativeNavigationPage()
                }, true
            )
        }) {
            Text("Push")
        }
    }
}
