package androidx.ui.examples.jetissues

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.activity.compose.setContent
import androidx.ui.examples.jetissues.view.JetIssuesView
import androidx.ui.examples.jetissues.view.Repository
import androidx.ui.examples.jetissues.data.IssuesRepositoryImpl
import androidx.ui.examples.jetissues.data.defaultAuth
import androidx.ui.examples.jetissues.data.defaultRepo

val repo = IssuesRepositoryImpl(defaultRepo.first, defaultRepo.second, System.getenv("GITHUB_TOKEN") ?: defaultAuth)

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
