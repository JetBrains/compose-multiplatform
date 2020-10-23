# Tray and menu notification

## What is covered

In this tutorial we'll show you how to work with the system tray, create an application menu bar and a window-specific menu bar, and send system notifications using Compose for Desktop.

## Tray

You can add an application icon to the system tray. You can also send notifications to the user using the system tray. There are 3 types of notification:

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
    Window(
		icon = getMyAppIcon()
	) {
        onActive {
            val tray = Tray().apply {
                icon(getTrayIcon())
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
    graphics.setColor(Color.green)
    graphics.fillOval(size / 4, 0, size / 2, size)
    graphics.setColor(Color.blue)
    graphics.fillOval(0, size / 4, size, size / 2)
    graphics.setColor(Color.red)
    graphics.fillOval(size / 4, size / 4, size / 2, size / 2)
    graphics.dispose()
    return image
}

fun getTrayIcon() : BufferedImage {
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
Notifier also has 3 types of notification:

1. notify - simple notification
2. warn - warning notification
3. error - error notification

```kotlin
import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Text
import androidx.compose.material.Button
import androidx.compose.ui.window.Notifier
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

fun main() {
    val message = "Some message!"
    val notifier = Notifier()
    Window(
		icon = getMyAppIcon()
	) {
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

fun getMyAppIcon() : BufferedImage {
    val size = 256
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.setColor(Color.green)
    graphics.fillOval(size / 4, 0, size / 2, size)
    graphics.setColor(Color.blue)
    graphics.fillOval(0, size / 4, size, size / 2)
    graphics.setColor(Color.red)
    graphics.fillOval(size / 4, size / 4, size / 2, size / 2)
    graphics.dispose()
    return image
}
```

![Notifier](notifier.gif)

## MenuBar

MenuBar is used to create and customize the common context menu of the application or a particular window.
To create a common context menu for all the application windows, you need to configure the AppManager.

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

You can create a MenuBar for a specific window, and have the other windows use the defined MenuBar.

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
