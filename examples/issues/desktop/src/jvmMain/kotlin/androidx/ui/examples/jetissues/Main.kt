package androidx.ui.examples.jetissues

import androidx.compose.desktop.Window
import androidx.compose.runtime.Providers
import androidx.compose.ui.unit.IntSize
import androidx.ui.examples.jetissues.view.JetIssuesView
import androidx.ui.examples.jetissues.view.Repository
import androidx.ui.examples.jetissues.data.IssuesRepositoryImpl

val repo = IssuesRepositoryImpl("ktorio", "ktor", System.getenv("GITHUB_TOKEN") ?: "fa7774306d1b9ab4af76652599841ee69a88fb6f")

fun main() = Window(
    title = "JetIssues",
    size = IntSize(1440, 768)
) {
    Providers(Repository provides repo) {
        JetIssuesView()
    }
}
