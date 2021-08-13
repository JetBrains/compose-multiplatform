# Context Menu in Compose for Desktop

## What is covered

In this tutorial we will cover all aspects of work with Context Menu
using the Compose UI framework.

## Default context menu 
There is out-of-the box context menu support for TextField and Selectable text. 

To enable standard context menu for a TextField you just need to put it inside DesktopMaterialTheme:

```kotlin
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

## Context menu for an arbitary area
There is a possibility to create a context menu for an arbitary application window area. This is implemented using ContextMenuArea API that is 
similar to ContextMenuDataProvider. 
```kotlin
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
