# Getting Started with Compose for Desktop

## What is covered

In this guide, we'll show you how to work with windows using Compose for Desktop.

## Windows creation

The main class for creating windows is AppWindow. The easiest way to create and launch a new window is to use an instance of the AppWindow class and call its method show(). You can see an example below:

```kotlin
import androidx.compose.desktop.AppWindow

fun main() {
    AppWindow().show {
        // content
    }
}
```

There are two types of windows - modal and active. Below are functions for creating each type of window:

1. Window - active window type.
2. Dialog - modal window type. Such a window locks its parent window until the user completes working with it and closes the modal window.

You can see an example for both types of windows below.

```kotlin
import androidx.compose.desktop.Window
import androidx.compose.material.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Dialog

fun main() {
    Window {
        val dialogState = remember { mutableStateOf(false) }

        Button(onClick = { dialogState.value = true })

        if (dialogState.value) {
            Dialog(
                onDismissEvent = { dialogState.value = false }
            ) {
                // dialog's content
            }
        }
    }
}
```

## Window attributes

Each window has 9 parameters listed below:

1. title - window title
2. size - initial window size
3. location - initial window position
4. centered - set the window to the center of the display
5. icon - window icon
6. menuBar - window context menu
7. undecorated - disable native border and title bar of the window
8. events - window events
9. onDismissEvent - event when removing the window content from a composition

An example of using window parameters at the creation step:

```kotlin
import androidx.compose.desktop.Window
import androidx.compose.desktop.WindowEvents
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

fun main() {
    Window(
        title = "MyApp",
        size = IntSize(800, 600),
        location = IntOffset(200, 200),
        centered = false, // true - by default
        icon = getMyAppIcon(), // custom function that returns BufferedImage
        menuBar = getMyAppMenuBar(), // custom function that returns MenuBar
        undecorated = true, // false - by default
        events = WindowEvents(
            onOpen = { println("OnOpen") },
            ... // here may be other events
            onResize = { size ->
                println("Size: $size")
            }
        )
    ) {
        // content
    }
}
```

## Window properties

AppWindow parameters correspond to the following properties:

1. title - window title
2. width - window width
3. height - window height
4. x - position of the left top corner of the window along the X axis
5. y - position of the left top corner of the window along the Y axis
6. icon - window icon image
7. events - window events

To get the properties of a window, it is enough to have a link to the current or specific window. There are two ways to get the current focused window:

1. Using the global environment:

```kotlin
import androidx.compose.desktop.AppWindowAmbient
import androidx.compose.desktop.Window

fun main() {
    Window {
        val current = AppWindowAmbient.current
        Button(
            onClick = {
                if (current != null) {
                    println("Title: ${current.title} ${current.x} ${current.y}")
                }
            }
        )
    }
}
```

2. Using AppManager:

```kotlin
import androidx.compose.desktop.AppManager
import androidx.compose.desktop.Window

fun main() {
    Window {
        Button(
            onClick = {
                val current = AppManager.getCurrentFocusedWindow()
                if (current != null) {
                    println("Title: ${current.title} ${current.x} ${current.y}")
                }
            }
        )
    }
}
```

Using the following methods, one can change the properties of AppWindow:

1. setTitle(title: String) - window title
2. setSize(width: Int, height: Int) - window size
3. setLocation(x: Int, y: Int) - window position
4. setWindowCentered() - set the window to the center of the display
5. setIcon(image: BufferedImage?) - window icon

```kotlin
import androidx.compose.desktop.AppWindowAmbient
import androidx.compose.desktop.Window

fun main() {
    Window {
        val current = AppWindowAmbient.current
        Button(
            onClick = {
                if (current != null) {
                    current.setWindowCentered()
                }
            }
        )
    }
}
```

## Window events

Events could be defined using the events parameter at the window creation step or redefine using the events property at runtime.
Actions can be assigned to the following window events:

1. onOpen - event during window opening
2. onClose - event during window closing
3. onMinimize - event during window minimizing
4. onMaximize - event during window maximizing
5. onRestore - event during restoring window size after window minimize/maximize
6. onFocusGet - event when window gets focus
7. onFocusLost - event when window loses focus
8. onResize - event on window resize (argument is window size as IntSize)
9. onRelocate - event of the window reposition on display (argument is window position as IntOffset)

```kotlin
import androidx.compose.desktop.Window
import androidx.compose.desktop.WindowEvents

fun main() {
    Window(
        events = WindowEvents(
            onOpen = { println("OnOpen") },
            ... // here may be other events
            onResize = { size ->
                println("Size: $size")
            }
        )
    ) {
        // content
    }
}
```

## AppManager

The AppManager class is used to customize the behavior of the entire application. Its main features:

1. Description of common application events
```kotlin
AppManager.onEvent(
    onAppStart = { println("OnAppStart") },
    onAppExit = { println("OnAppExit") }
)
```
2. Customization of common application context menu
```kotlin
AppManager.menu(
    getCommonAppMenuBar() // custom function that returns MenuBar
)
```
3. Access to the application windows list
```kotlin
val windows = AppManager.getWindows()
```
4. Getting the current focused window
```kotlin
val current = AppManager.getCurrentFocusedWindow()
```
5. Application exit
```kotlin
AppManager.exit() // closes all windows
```

## Access to javax.swing components

Compose for Desktop uses Swing components as the window system. For more detailed customization, you can access the JFrame (Swing window representation):

```kotlin
import androidx.compose.desktop.AppManager
import androidx.compose.desktop.Window

fun main() {
    Window {
        Button(
            onClick = {
                val current = AppManager.getCurrentFocusedWindow()
                if (current != null) {
                    val jFrame = current.window
                    // do whatever you want with it, for example add some new listeners
                }
            }
        )
    }
}
```
