package androidx.ui.examples.jetissues

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.ui.examples.jetissues.view.JetIssuesView
import androidx.ui.examples.jetissues.view.Repository
import androidx.ui.examples.jetissues.data.IssuesRepositoryImpl
import androidx.ui.examples.jetissues.data.defaultAuth
import androidx.ui.examples.jetissues.data.defaultRepo
import kotlin.system.exitProcess

val repo = IssuesRepositoryImpl(defaultRepo.first, defaultRepo.second, System.getenv("GITHUB_TOKEN") ?: defaultAuth)

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "JetIssues",
            state = WindowState(size = DpSize(1440.dp, 768.dp))
        ) {
            CompositionLocalProvider(Repository provides repo) {
                JetIssuesView()
            }
        }
    }
    exitProcess(0) // force close Apollo Client, that is used inside IssuesRepositoryImpl
}