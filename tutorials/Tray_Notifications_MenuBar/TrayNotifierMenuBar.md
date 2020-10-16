# Getting Started with Compose for Desktop

## What is covered

In this guide, we'll show you how to work with system tray, create application menu bar and create window specific menu bar, and send system notifications using Compose for Desktop.

## Tray

You can add an application icon into the system tray. Using Tray, you can also send notifications to the user. There are 3 types of notifications:

1. notify - simple notification
2. warn - warning notification
3. error - error notification

```kotlin
import androidx.compose.desktop.AppManager
import androidx.compose.desktop.Window
import androidx.compose.runtime.onActive
import androidx.compose.runtime.onDispose
import androidx.compose.ui.window.Item
import androidx.compose.ui.window.Tray

fun main() {
    Window {
        onActive {
            val tray = Tray().apply {
                icon(getImageIcon()) // custom function that returns BufferedImage
                menu(
                    Item(
                        name = "About",
                        onClick = {
                            println("This is MyApp")
                        }
                    ),
                    Item(
                        name = "Send notification",
                        onClick = {
                            tray.notify("Notification", "Message from MyApp!")
                        }
                    ),
                    Item(
                        name = "Exit",
                        onClick = {
                            AppManager.exit()
                        }
                    )
                )
            }
            onDispose {
                tray.remove()
            }
        }
    }
}
```

## Notifier
You can send system notifications with Notifier without using the system tray.
Notifier also has 3 types of notifications:

1. notify - simple notification
2. warn - warning notification
3. error - error notification

```kotlin
import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Text
import androidx.compose.material.Button
import androidx.compose.ui.window.Notifier

fun main() {
    val message = "Some message!"
    Window {
        Column {
            Button(onClick = { Notifier().notify("Notification.", message) }) {
                Text(text = "Notify")
            }
            Button(onClick = { Notifier().warn("Warning.", message) }) {
                Text(text = "Warning")
            }
            Button(onClick = { Notifier().error("Error.", message) }) {
                Text(text = "Error")
            }
        }
    }
}
```

## MenuBar

MenuBar is used to create and customize the common context menu of the application or any particular window.
To create a common context menu for all application windows, you need to configure the AppManager.

```kotlin
import androidx.compose.desktop.AppManager
import androidx.compose.desktop.Window
import androidx.compose.ui.window.Item
import androidx.compose.ui.window.keyStroke
import androidx.compose.ui.window.Menu
import androidx.compose.ui.window.MenuBar
import java.awt.event.KeyEvent

fun main() {
    AppManager.menu(
        MenuBar(
            Menu(
                name = "Actions",
                Item(
                    name = "About",
                    onClick = { println("This is MyApp") },
                    shortcut = keyStroke(KeyEvent.VK_I)
                ),
                Item(
                    name = "Exit",
                    onClick = { AppManager.exit() },
                    shortcut = keyStroke(KeyEvent.VK_X)
                )
            ),
            Menu(
                name = "File",
                Item(
                    name = "Copy",
                    onClick = { println("Copy operation.") },
                    shortcut = keyStroke(KeyEvent.VK_C)
                ),
                Item(
                    name = "Paste",
                    onClick = { println("Paste operation.") },
                    shortcut = keyStroke(KeyEvent.VK_V)
                )
            )
        )
    )

    Window {
        // content
    }
}
```

You can to create a MenuBar for a specific window (the rest of the windows will use the common MenuBar, if defined).

```kotlin
import androidx.compose.desktop.AppManager
import androidx.compose.desktop.Window
import androidx.compose.ui.window.Item
import androidx.compose.ui.window.keyStroke
import androidx.compose.ui.window.Menu
import androidx.compose.ui.window.MenuBar
import java.awt.event.KeyEvent

fun main() {
    Window(
        menuBar = MenuBar(
            Menu(
                name = "Actions",
                Item(
                    name = "About",
                    onClick = { println("This is MyApp") },
                    shortcut = keyStroke(KeyEvent.VK_I)
                ),
                Item(
                    name = "Exit",
                    onClick = { AppManager.exit() },
                    shortcut = keyStroke(KeyEvent.VK_X)
                )
            ),
            Menu(
                name = "File",
                Item(
                    name = "Copy",
                    onClick = { println("Copy operation.") },
                    shortcut = keyStroke(KeyEvent.VK_C)
                ),
                Item(
                    name = "Paste",
                    onClick = { println("Paste operation.") },
                    shortcut = keyStroke(KeyEvent.VK_V)
                )
            )
        )
    ) {
        // content
    }
}
```

