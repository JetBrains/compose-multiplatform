# Menu, tray, notifications (new Composable API, experimental)

## What is covered

In this tutorial, we'll show you how to work with the system tray, send system notifications, and create a menu bar using Compose for Desktop.

## Tray

You can add an application icon to the system tray. You can also send notifications to the user using the system tray. There are 3 types of notifications:

1. notify - simple notification
2. warn - warning notification
3. error - error notification

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import java.awt.Color
import java.awt.image.BufferedImage

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    var count by remember { mutableStateOf(0) }
    var isOpen by remember { mutableStateOf(true) }

    if (isOpen) {
        Window(
            onCloseRequest = ::exitApplication,
            icon = remember { getMyAppIcon() }
        ) {
            val trayState = rememberTrayState()
            val notification = Notification("Notification", "Message from MyApp!")
            Tray(
                state = trayState,
                icon = remember { getTrayIcon() },
                menu = {
                    Item(
                        "Increment value",
                        onClick = {
                            count++
                        }
                    )
                    Item(
                        "Send notification",
                        onClick = {
                            trayState.sendNotification(notification)
                        }
                    )
                    Item(
                        "Exit",
                        onClick = {
                            isOpen = false
                        }
                    )
                }
            )

            // content
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Value: ${count}")
            }
        }
    }
}

fun getMyAppIcon(): BufferedImage {
    val size = 256
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.color = Color.green
    graphics.fillOval(size / 4, 0, size / 2, size)
    graphics.color = Color.blue
    graphics.fillOval(0, size / 4, size, size / 2)
    graphics.color = Color.red
    graphics.fillOval(size / 4, size / 4, size / 2, size / 2)
    graphics.dispose()
    return image
}

fun getTrayIcon(): BufferedImage {
    val size = 256
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.color = Color.orange
    graphics.fillOval(0, 0, size, size)
    graphics.dispose()
    return image
}
```

![](tray.gif)

## MenuBar

MenuBar is used to create and customize the menu bar for a particular window.

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Currently we use Swing's menu under the hood, so we need to set this property to change the look and feel of the menu on Windows/Linux
    System.setProperty("skiko.rendering.laf.global", "true")

    application {
        var action by remember { mutableStateOf("Last action: None") }
        var isOpen by remember { mutableStateOf(true) }

        if (isOpen) {
            var isSubmenuShowing by remember { mutableStateOf(false) }

            Window(onCloseRequest = { isOpen = false }) {
                MenuBar {
                    Menu("Actions") {
                        Item(
                            if (isSubmenuShowing) "Hide advanced settings" else "Show advanced settings",
                            onClick = {
                                isSubmenuShowing = !isSubmenuShowing
                            }
                        )
                        if (isSubmenuShowing) {
                            Menu("Settings") {
                                Item("Setting 1", onClick = { action = "Last action: Setting 1" })
                                Item("Setting 2", onClick = { action = "Last action: Setting 2" })
                            }
                        }
                        Separator()
                        Item("About", onClick = { action = "Last action: About" })
                        Item("Exit", onClick = { isOpen = false })
                    }
                    Menu("File") {
                        Item("Copy", onClick = { action = "Last action: Copy" })
                        Item("Paste", onClick = { action = "Last action: Paste" },)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = action)
                }
            }
        }
    }
}
```

![](window_menubar.gif)
