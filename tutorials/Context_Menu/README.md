# Context Menu in Compose for Desktop

## What is covered

In this tutorial we will cover all aspects of work with Context Menu
using the Compose UI framework.

## Default context menu 
There is out-of-the box context menu support for TextField and Selectable text. 

To enable standard context menu for a TextField you just need to put it inside DesktopMaterialTheme:

```kotlin
import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
fun main() = singleWindowApplication(title = "Context menu") {
   DesktopMaterialTheme { //it is mandatory for Context Menu
       val text = remember {mutableStateOf("Hello!")}
       TextField(
           value = text.value,
           onValueChange = { text.value = it },
           label = { Text(text = "Input") }
       )
   }
} 
```

Standard context menu for TextField contains the following items based on text selection: Copy, Cut, Paste, Select All.

Enabling standard context menu for a Text component is similar - you just need to make it selectable: 

```kotlin
import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
fun main() = singleWindowApplication(title = "Context menu") {
   DesktopMaterialTheme { //it is mandatory for Context Menu
        SelectionContainer {
            Text("Hello World!")
        }
   }
} 
```
Context menu for text contains just Copy action.

## User-defined context menu
To enable additional context menu items for TextField and Text components, ContextMenuDataProvider and ContextMenuItem elements are used:

```kotlin
import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ContextMenuItem
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
fun main() = singleWindowApplication(title = "Context menu") {
   DesktopMaterialTheme { //it is mandatory for Context Menu
       val text = remember {mutableStateOf("Hello!")}
        Column {
            ContextMenuDataProvider(
                items = {
                    listOf(
                        ContextMenuItem("User-defined Action") {/*do something here*/},
                        ContextMenuItem("Another user-defined action") {/*do something else*/}
                    )
                }
            ) {
                TextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    label = { Text(text = "Input") }
                )

                Spacer(Modifier.height(16.dp))

                SelectionContainer {
                    Text("Hello World!")
                }
            }
        }
   }
} 
```
In this example Text/TextField context menus will be extended with two additional items. 

## Context menu for an arbitrary area
There is a possibility to create a context menu for an arbitrary application window area. This is implemented using ContextMenuArea API that is 
similar to ContextMenuDataProvider. 
```kotlin

import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ContextMenuItem
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
fun main() = singleWindowApplication(title = "Context menu") {
   DesktopMaterialTheme { //it is mandatory for Context Menu
        ContextMenuArea(items = {
            listOf(
                ContextMenuItem("User-defined Action") {/*do something here*/},
                ContextMenuItem("Another user-defined action") {/*do something else*/}
            )
        }) {
            Box(modifier = Modifier.background(Color.Blue).height(100.dp).width(100.dp)) {
            }
        }
   }
} 
```
Right click on the Blue Square will show a context menu with two items
