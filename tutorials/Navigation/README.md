# Navigation

## General attitude

The Jetpack Compose navigation library ([navigation-compose](https://developer.android.com/jetpack/compose/navigation)) is an Android-only library, and so can not be used together with Compose for Desktop. Our general attitude is not to “force” people to use a particular first-party library. However there are third-party libraries available. One could consider [Decompose](https://github.com/arkivanov/Decompose) as possible solution.

## Patterns

Navigation is not only about switching child components and managing the back stack. It may also affect the architecture of the application. 

There are two common patterns of the navigation in Compose: the navigation logic can be kept and managed either inside or outside `@Composable` world. Each approach has its advantages and disadvantages, so please decide wisely.

This tutorial describes both patterns, how to choose between them, and how the Decompose library can help.

## Prerequisites

This tutorial uses a very simple example of a List-Details application with just two screens: `ItemList` and `ItemDetails`. There are few things we need to do first.

### Setup

First let's add the Decompose library to the project. Please refer to the [Getting started](https://arkivanov.github.io/Decompose/getting-started/) section of the documentation.

### Item model and Database

Here is the `Item` data class that we will need:

```kotlin
data class Item(
    val id: Long,
    val text: String
)
```

And a simple `Database` interface that will be used by child screens (there is no concurrency just for simplicity):

``` kotlin
interface Database {
    fun getAll(): List<Item>
    fun getById(id: Long): Item
}
```

### Basic UI for child screens

We will need some basic UI for both `List` and `Details` screens.

The `ItemListScreen` `@Composable` component displays the list of `Items` and calls `onItemClick` callback when an item is clicked:

``` kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ItemListScreen(items: List<Item>, onItemClick: (id: Long) -> Unit) {
    LazyColumn {
        items(items = items) { item ->
            Text(
                text = item.text,
                modifier = Modifier.clickable { onItemClick(item.id) }
            )
        }
    }
}
```

The `ItemDetailsScreen` `@Composable` component displays the previously selected `Item` and calls `onBackClick` callback when the back button in the `TopAppBar` is clicked:

``` kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable

@Composable
fun ItemDetailsScreen(item: Item, onBackClick: () -> Unit) {
    Column {
        TopAppBar(
            title = { Text("Item details") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        )

        Text(text = item.text)
    }
}
```


### Children configuration

One of the main goals of the Decompose library is compile time safety. Each child is described by a class called `Configuration`. The purpose of the `Configuration` is to describe what child should be used and what arguments it has. So for each child there is an instance of its own `Configuration` class. Normally there are multiple children involved in the navigation, so the whole set of configurations is normally a sealed class.

For example, for a simple List-Details navigation we need just two entries:

``` kotlin
import com.arkivanov.decompose.statekeeper.Parcelable

sealed class Configuration : Parcelable {
    object List : Configuration()
    data class Details(val itemId: Long) : Configuration()
}
```

Such an approach looks a bit verbose, but it brings compile time safety in the following cases:

- Child arguments are verified at compile time (unlike passing arguments via strings, `Bundles`, etc.).
- Configurations can be checked exhaustively, so the compilation will fail if not all children are covered.

#### Parcelable configurations in Android

Desktop Compose is actually a multiplatform library and can also be used in Android. This makes it possible to share the navigation logic as well. But Android has additional requirements for navigation - the back stack should survive [configuration changes](https://developer.android.com/guide/topics/resources/runtime-changes). In general, the back stack should be saved and restored when such an event occurs.

To make this possible, all child Configurations must be [Parcelable](https://developer.android.com/reference/android/os/Parcelable). For convenience, Decompose defines both `Parcelable` and `@Parcelize` using [expect/actual](https://kotlinlang.org/docs/reference/mpp-connect-to-apis.html):

- `Parcelable` - this interface is defined by Decompose in the `commonMain` source set. It is typealised to the Android's `Parcelable` interface for Android target, and is just an empty interface in all other targets (including JVM/Desktop).
- `@Parcelize` - this annotation is also defined in the `commonMain` source set. It is typealised to the `@Parcelize` annotation provided by the [kotlin-parcelize](https://developer.android.com/kotlin/parcelize) plugin. And it is missing (as not needed) in non-Android targets.

If you need Android support, please make sure you have `kotlin-parcelize` plugin enabled. All Configurations should look like this:

``` kotlin
import com.arkivanov.decompose.statekeeper.Parcelable
import com.arkivanov.decompose.statekeeper.Parcelize

sealed class Configuration : Parcelable {
    @Parcelize
    object List : Configuration()

    @Parcelize
    data class Details(val itemId: Long) : Configuration()
}
```

## Managing navigation outside @Composable world

This pattern should be chosen if any of the following apply:

1. You support Multipaltform targets with different UI frameworks, and you want to share the navigation logic between them. For example if you support Desktop with Compose UI, iOS with SwiftUI and/or JavaScript with React UI.
2. You want to keep children running while in the back stack (stopped, but not destroyed).
3. You are targeting Android and need instance retaining functionality in children (aka AndroidX [ViewModels](https://developer.android.com/topic/libraries/architecture/viewmodel)) and you want to hide this logic as implementation details.
4. You want to keep the navigation logic (and probably the business logic) separate from UI.

The first point is quite obvious. If Compose is not the only UI you are using and you want to share the navigation logic, then it can not be managed by Compose.

The second point may be especially useful in Desktop. When a child is pushed to the back stack, it is stopped but not destroyed. So it keeps running in "background" without UI. This makes it possible to keep children's state in memory while navigating.

The third point is about instances retaining, like AndroidX `ViewModels`, and is mostly used in Android. It allows to retain (keep in memory) some data when Android configuration change occurs and the whole navigation stack is recreated. The most important advantage of instance retaining in this pattern is that it is encapsulated in children as implementation details.

The forth point is not that obvious but might be very important. Separating navigation and business logic from the user interface may improve testability. E.g. it becomes possible to test non-UI code in integration with just plain JUnit tests. And the UI can be tested in isolation as well using another testing frameworks.

You can find some integration tests in the TodoApp example:

- [TodoMainTest](https://github.com/JetBrains/compose-jb/blob/master/examples/todoapp/common/main/src/commonTest/kotlin/example/todo/common/main/integration/TodoMainTest.kt) - integration tests for the Main screen.
- [TodoRootTest](https://github.com/JetBrains/compose-jb/blob/master/examples/todoapp/common/root/src/commonTest/kotlin/example/todo/common/root/integration/TodoRootTest.kt) - integration tests for navigation between the Main and the Edit screens.

This pattern is encouraged by the Decompose library. If this is your choice, then you can just use its recommended approach. 

The main idea is to split (decompose) your project by multiple components. Components can be organized in a tree structure, and each level can (but not must) have multiple [Routers](https://arkivanov.github.io/Decompose/router/overview/). Each component is just a normal interface/class, an entry point to the underlying logic.

The only responsibility of the user interface is to listen for components' state changes and trigger their events.

The following resources can help with this pattern:
- The Decompose [documentation](https://arkivanov.github.io/Decompose/)
- The [TodoApp](https://github.com/JetBrains/compose-jb/tree/master/examples/todoapp) example
- The article "[Fully cross-platform Kotlin applications (almost)](https://proandroiddev.com/fully-cross-platform-kotlin-applications-almost-29c7054f8f28)"

### A very basic example:

`ItemList` child with UI:

``` kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

class ItemList(
    database: Database, // Accept the Database as dependency
    val onItemSelected: (itemId: Long) -> Unit // Called on item click
) {
    // No concurrency involved just for simplicity. The state can be updated if needed.
    private val _state = mutableStateOf(database.getAll())
    val state: State<List<Item>> = _state
}

@Composable
fun ItemListUi(list: ItemList) {
    ItemListScreen(
        items = list.state.value,
        onItemClick = list.onItemSelected
    )
}
```

`ItemDetails` child with UI:

``` kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

class ItemDetails(
    itemId: Long, // An item id to be loaded and displayed
    database: Database, // Accept the Database as dependency
    val onFinished: () -> Unit // Called on TopAppBar back button click
) {
    // No concurrency involved just for simplicity. The state can be updated if needed.
    private val _state = mutableStateOf(database.getById(id = itemId))
    val state: State<Item> = _state
}

@Composable
fun ItemDetailsUi(details: ItemDetails) {
    ItemDetailsScreen(
        item = details.state.value,
        onBackClick = details.onFinished
    )
}
```

Root with navigation (assuming only Compose UI is used):

``` kotlin
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.pop
import com.arkivanov.decompose.push
import com.arkivanov.decompose.router

typealias Content = @Composable () -> Unit

fun <T : Any> T.asContent(content: @Composable (T) -> Unit): Content = { content(this) }

class Root(
    componentContext: ComponentContext, // In Decompose each component has its own ComponentContext
    private val database: Database // Accept the Database as dependency
) : ComponentContext by componentContext {

    private val router =
        router<Configuration, Content>(
            initialConfiguration = Configuration.List, // Starting with List
            childFactory = ::createChild // The Router calls this function, providing the child Configuration and ComponentContext 
        )

    val routerState = router.state

    private fun createChild(configuration: Configuration, context: ComponentContext): Content =
        when (configuration) {
            is Configuration.List -> list()
            is Configuration.Details -> details(configuration)
        } // Configurations are handled exhaustively

    private fun list(): Content =
        ItemList(
            database = database, // Supply dependencies
            onItemSelected = { router.push(Configuration.Details(itemId = it)) } // Push Details on item click
        ).asContent { ItemListUi(it) }

    private fun details(configuration: Configuration.Details): Content =
        ItemDetails(
            itemId = configuration.itemId, // Safely pass arguments
            database = database, // Supply dependencies
            onFinished = router::pop // Go back to List
        ).asContent { ItemDetailsUi(it) }
}

@Composable
fun RootUi(root: Root) {
    Children(root.routerState) { child ->
        child.instance()
    }
}
```

Application and Root initialisation:

``` kotlin
import androidx.compose.desktop.DesktopTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.singleWindowApplication
import com.arkivanov.decompose.extensions.compose.jetbrains.rememberRootComponent

fun main() = singleWindowApplication(
    title = "Navigation tutorial"
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        MaterialTheme {
            DesktopTheme {
                RootUi(root()) // Render the Root and its children
            }
        }
    }
}

@Composable
private fun root(): Root =
    // The rememberRootComponent function provides the root ComponentContext and remembers the instance or Root
    rememberRootComponent { componentContext ->
        Root(
            componentContext = componentContext,
            database = DatabaseImpl() // Supply dependencies
        )
    }
```

## Managing navigation inside @Composable world

By using this pattern, the navigation logic is kept and managed inside `@Composable` functions. For example, this pattern is used by the Jetpack Compose `navigation-compose` library. In practice there is normally a function like `@Composable fun Navigator(...)` or `@Composable fun NavHost(...)` that manages the back stack and renders the currently active child. The way how the function renders children depends on its API.

This pattern should be chosen if you prefer to use Compose for more than just UI, and none of the first pattern's points apply.

Decompose does not provide any out-of-the-box `@Composable` navigation API. But it is pretty easy to write your own with it. You can experiment and come up with your own API.

Please refer to the following article for implementation details: "[A comprehensive hundred-line navigation for Jetpack/Desktop Compose](https://proandroiddev.com/a-comprehensive-hundred-line-navigation-for-jetpack-desktop-compose-5b723c4f256e)". It also explains some additional features, like back button handling, transition animations, etc.

### A very basic example:

``` kotlin
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.Router
import com.arkivanov.decompose.statekeeper.Parcelable

@Composable
inline fun <reified C : Parcelable> rememberRouter(
    noinline initialConfiguration: () -> C
): Router<C, Any> =
    TODO("See the article mentioned above for the implementation")
```

First of all we need the `Router` from the Decompose library. Once we have it, all we need to do is to use the `Children` function. The `Children` function listens for the `Router` state changes, and renders the currently active child using the provided callback. The article mentioned above explains the implementation details.

Using the `Router`:

``` kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arkivanov.composenavigatorexample.navigator.rememberRouter
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.pop
import com.arkivanov.decompose.push


@Composable
fun ItemList(
    database: Database,
    onItemClick: (itemId: Long) -> Unit
) {
    // No concurrency involved just for simplicity. The state can be updated if needed.
    val items = remember { mutableStateOf(database.getAll()) }

    ItemListScreen(
        items = items.value,
        onItemClick = onItemClick
    )
}

@Composable
fun ItemDetails(
    itemId: Long,
    database: Database,
    onBackClick: () -> Unit
) {
    // No concurrency involved just for simplicity. The state can be updated if needed.
    val item = remember { mutableStateOf(database.getById(id = itemId)) }

    ItemDetailsScreen(
        item = item.value,
        onBackClick = onBackClick
    )
}

@Composable
fun Root(database: Database) {
    // Create and remember the Router
    val router =
        rememberRouter<Configuration>(
            initialConfiguration = { Configuration.List } // Start with the List screen
        )

    // Render children
    Children(routerState = router.state) { screen ->
        when (val configuration = screen.configuration) {
            is Configuration.List ->
                ItemList(
                    database = database, // Supply dependencies
                    onItemClick = { router.push(Configuration.Details(itemId = it)) } // Push Details on item click
                )

            is Configuration.Details ->
                ItemDetails(
                    itemId = configuration.itemId, // Safely pass arguments
                    database = database, // Supply dependencies
                    onBackClick = router::pop // Go back to List
                )
        }.let {} // Ensure exhaustiveness
    }
}
```

