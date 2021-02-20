package androidx.ui.examples.jetissues

import androidx.compose.desktop.Window
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.IntSize
import androidx.ui.examples.jetissues.view.JetIssuesView
import androidx.ui.examples.jetissues.view.Repository
import androidx.ui.examples.jetissues.data.IssuesRepositoryImpl

val repo = IssuesRepositoryImpl("ktorio", "ktor", System.getenv("GITHUB_TOKEN") ?: " 8c5097718c3108397f035fd6a6f307fa0b70f9b1")

fun main() = Window(
    title = "JetIssues",
    size = IntSize(1440, 768)
) {
    CompositionLocalProvider(Repository provides repo) {
        JetIssuesView()
    }
}
