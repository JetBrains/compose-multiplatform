package androidx.ui.examples.jetissues

import androidx.compose.desktop.Window
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.IntSize
import androidx.ui.examples.jetissues.view.JetIssuesView
import androidx.ui.examples.jetissues.view.Repository
import androidx.ui.examples.jetissues.data.IssuesRepositoryImpl
import androidx.ui.examples.jetissues.data.defaultAuth
import androidx.ui.examples.jetissues.data.defaultRepo

val repo = IssuesRepositoryImpl(defaultRepo.first, defaultRepo.second, System.getenv("GITHUB_TOKEN") ?: defaultAuth)

fun main() = Window(
    title = "JetIssues",
    size = IntSize(1440, 768)
) {
    CompositionLocalProvider(Repository provides repo) {
        JetIssuesView()
    }
}
