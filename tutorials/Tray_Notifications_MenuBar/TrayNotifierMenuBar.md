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
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onActive
import androidx.compose.runtime.onDispose
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.MenuItem
import androidx.compose.ui.window.Tray
import androidx.compose.ui.Modifier
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

fun main() {
    val count = mutableStateOf(0)
    Window {
        onActive {
            val tray = Tray().apply {
                icon(getMyAppIcon())
                menu(
                    MenuItem(
                        name = "Increment value",
                        onClick = {
                            count.value++
                        }
                    ),
                    MenuItem(
                        name = "Send notification",
                        onClick = {
                            notify("Notification", "Message from MyApp!")
                        }
                    ),
                    MenuItem(
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

        // content
        Box(
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.Center
        ) {
            Text(text = "Value: ${count.value}")
        }
    }
}

fun getMyAppIcon() : BufferedImage {
    val size = 256
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.setColor(Color.orange)
    graphics.fillOval(0, 0, size, size)
    graphics.dispose()
    return image
}
```

![Tray](tray.gif)

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
    val notifier = Notifier()
    Window {
        Column {
            Button(onClick = { notifier.notify("Notification.", message) }) {
                Text(text = "Notify")
            }
            Button(onClick = { notifier.warn("Warning.", message) }) {
                Text(text = "Warning")
            }
            Button(onClick = { notifier.error("Error.", message) }) {
                Text(text = "Error")
            }
        }
    }
}
```

![Notifier](notifier.gif)

## MenuBar

MenuBar is used to create and customize the common context menu of the application or any particular window.
To create a common context menu for all application windows, you need to configure the AppManager.

```kotlin
import androidx.compose.desktop.AppManager
import androidx.compose.desktop.Window
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.KeyStroke
import androidx.compose.ui.window.MenuItem
import androidx.compose.ui.window.Menu
import androidx.compose.ui.window.MenuBar

fun main() {
    val action = mutableStateOf("Last action: None")

    AppManager.setMenu(
        MenuBar(
            Menu(
                name = "Actions",
                MenuItem(
                    name = "About",
                    onClick = { action.value = "Last action: About (Command + I)" },
                    shortcut = KeyStroke(Key.I)
                ),
                MenuItem(
                    name = "Exit",
                    onClick = { AppManager.exit() },
                    shortcut = KeyStroke(Key.X)
                )
            ),
            Menu(
                name = "File",
                MenuItem(
                    name = "Copy",
                    onClick = { action.value = "Last action: Copy (Command + C)" },
                    shortcut = KeyStroke(Key.C)
                ),
                MenuItem(
                    name = "Paste",
                    onClick = { action.value = "Last action: Paste (Command + V)" },
                    shortcut = KeyStroke(Key.V)
                )
            )
        )
    )

    Window {
        // content
        Box(
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.Center
        ) {
            Text(text = action.value)
        }
    }
}
```

![Application MenuBar](app_menubar.gif)

You can to create a MenuBar for a specific window (while others will use the common MenuBar, if defined).

```kotlin
import androidx.compose.desktop.AppManager
import androidx.compose.desktop.Window
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.KeyStroke
import androidx.compose.ui.window.MenuItem
import androidx.compose.ui.window.Menu
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

fun main() {
    val action = mutableStateOf("Last action: None")

    Window(
        menuBar = MenuBar(
            Menu(
                name = "Actions",
                MenuItem(
                    name = "About",
                    onClick = { action.value = "Last action: About (Command + I)" },
                    shortcut = KeyStroke(Key.I)
                ),
                MenuItem(
                    name = "Exit",
                    onClick = { AppManager.exit() },
                    shortcut = KeyStroke(Key.X)
                )
            ),
            Menu(
                name = "File",
                MenuItem(
                    name = "Copy",
                    onClick = { action.value = "Last action: Copy (Command + C)" },
                    shortcut = KeyStroke(Key.C)
                ),
                MenuItem(
                    name = "Paste",
                    onClick = { action.value = "Last action: Paste (Command + V)" },
                    shortcut = KeyStroke(Key.V)
                )
            )
        )
    ) {
        // content
        Button(
            onClick = {
                Window(
                    title = "Another window",
                    size = IntSize(350, 200),
                    location = IntOffset(100, 100),
                    centered = false
                ) {
                    
                }
            }
        ) {
            Text(text = "New window")
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.Center
        ) {
            Text(text = action.value)
        }
    }
}
```

![Window MenuBar](window_menubar.gif)
