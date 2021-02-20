package androidx.ui.examples.jetissues

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.activity.compose.setContent
import androidx.ui.examples.jetissues.view.JetIssuesView
import androidx.ui.examples.jetissues.view.Repository
import androidx.ui.examples.jetissues.data.IssuesRepositoryImpl

val repo = IssuesRepositoryImpl("ktorio", "ktor", System.getenv("GITHUB_TOKEN") ?: "8c5097718c3108397f035fd6a6f307fa0b70f9b1")

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(Repository provides repo) {
                JetIssuesView()
            }
        }
    }
}
