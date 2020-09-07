package androidx.ui.examples.jetissues

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Providers
import androidx.compose.ui.platform.setContent
import androidx.ui.examples.jetissues.view.JetIssuesView
import androidx.ui.examples.jetissues.view.Repository
import androidx.ui.examples.jetissues.data.IssuesRepositoryImpl

val repo = IssuesRepositoryImpl("ktorio", "ktor", System.getenv("GITHUB_TOKEN") ?: "fa7774306d1b9ab4af76652599841ee69a88fb6f")

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Providers(Repository provides repo) {
                JetIssuesView()
            }
        }
    }
}