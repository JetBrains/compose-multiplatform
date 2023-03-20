package example.imageviewer.view

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import example.imageviewer.Localization
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

internal object CameraScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        CameraScreen(
            localization = LocalDependencies.current.localization,
            onBack = navigator::pop,
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun CameraScreen(localization: Localization, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        CameraView(Modifier.fillMaxSize())
        TopLayout(
            alignLeftContent = {
                Tooltip(localization.back) {
                    CircularButton(
                        painterResource("arrowleft.png"),
                        onClick = { onBack() }
                    )
                }
            },
            alignRightContent = {},
        )
    }
}
