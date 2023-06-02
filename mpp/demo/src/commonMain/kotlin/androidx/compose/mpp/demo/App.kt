package androidx.compose.mpp.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val MainScreen = Screen.Selection(
    "Demo",
    Screen.Example("Example1") { Example1() },
    Screen.Example("ImageViewer") { ImageViewer() },
    Screen.Example("RoundedCornerCrashOnJS") { RoundedCornerCrashOnJS() },
    Screen.Example("TextDirection") { TextDirection() },
    Screen.Example("FontFamilies") { FontFamilies() },
    Screen.Example("LottieAnimation") { LottieAnimation() },
    LazyLayouts,
)

sealed interface Screen {
    val title: String

    class Example(override val title: String, val content: @Composable () -> Unit) : Screen
    class Selection(override val title: String, val screens: List<Screen>) : Screen {
        constructor(title: String, vararg screens: Screen) : this(title, listOf(*screens))

        fun mergedWith(screens: List<Screen>): Selection {
            return Selection(title, this.screens + screens)
        }
    }
}

class App(
    initialScreenName: String? = null,
    extraScreens: List<Screen> = listOf()
) {
    private val navigationStack: SnapshotStateList<Screen> = mutableStateListOf(MainScreen.mergedWith(extraScreens))

    init {
        if (initialScreenName != null) {
            var currentScreen = navigationStack.first()
            initialScreenName.split("/").forEach { target ->
                val selectionScreen = currentScreen as Screen.Selection
                currentScreen = selectionScreen.screens.find { it.title == target }!!
                navigationStack.add(currentScreen)
            }
        }
    }

    @Composable
    fun Content() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title: String = if (navigationStack.size > 1) {
                            navigationStack.drop(1).joinToString("/") { it.title }
                        } else {
                            navigationStack.first().title
                        }
                        Text(title)
                    },
                    navigationIcon = {
                        if (navigationStack.size > 1) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.clickable { navigationStack.removeLast() }
                            )
                        }
                    }
                )
            }
        ) {
            when (val screen = navigationStack.last()) {
                is Screen.Example -> {
                    screen.content()
                }

                is Screen.Selection -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(screen.screens) {
                            Text(it.title, Modifier.clickable {
                                navigationStack.add(it)
                            }.padding(16.dp).fillMaxWidth())
                        }
                    }
                }
            }
        }
    }

}
