package org.jetbrains.compose.resources.demo.shared

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

enum class Screens(val content: @Composable (contentPadding: PaddingValues) -> Unit) {
    Images({ ImagesRes(it) }),
    Strings({ StringRes(it) }),
    Font({}),
    File({ FileRes(it) }),
}

@Composable
internal fun UseResources() {
    var screen by remember { mutableStateOf(Screens.Images) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { screen.content(it) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == Screens.Images,
                    onClick = { screen = Screens.Images },
                    icon = { Icon(imageVector = Icons.Default.Image, contentDescription = null) },
                    label = { Text("Images") }
                )
                NavigationBarItem(
                    selected = screen == Screens.Strings,
                    onClick = { screen = Screens.Strings },
                    icon = { Icon(imageVector = Icons.Default.Abc, contentDescription = null) },
                    label = { Text("Strings") }
                )
                NavigationBarItem(
                    selected = screen == Screens.Font,
                    onClick = { screen = Screens.Font },
                    icon = { Icon(imageVector = Icons.Default.TextFields, contentDescription = null) },
                    label = { Text("Fonts") }
                )
                NavigationBarItem(
                    selected = screen == Screens.File,
                    onClick = { screen = Screens.File },
                    icon = { Icon(imageVector = Icons.Default.Attachment, contentDescription = null) },
                    label = { Text("Files") }
                )
            }
        }
    )
}
