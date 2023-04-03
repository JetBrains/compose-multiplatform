package androidx.compose.mpp.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class App(
    private val initialScreenName: String? = null
) {
    private val MainScreen = Screen("Demo") { Main() }

    private val screens = listOf(
        Screen("Example1") { Example1() },
        Screen("ImageViewer") { ImageViewer() },
    )

    private class Screen(val title: String, val content: @Composable () -> Unit)

    private var screen: Screen by mutableStateOf(
        if (initialScreenName != null) {
            screens.find { it.title == initialScreenName }!!
        } else {
            MainScreen
        }
    )

    @Composable
    fun Content() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(screen.title) },
                    navigationIcon = if (screen != MainScreen) {
                        {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.clickable { screen = MainScreen }
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        ) {
            screen.content()
        }
    }

    @Composable
    fun Main() {
        LazyColumn(Modifier.fillMaxSize()) {
            items(screens) {
                Text(it.title, Modifier.clickable { screen = it }.padding(16.dp).fillMaxWidth())
            }
        }
    }
}