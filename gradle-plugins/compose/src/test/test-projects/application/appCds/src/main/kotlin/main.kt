import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.singleWindowApplication
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

fun main() {
    val creatingAppCdsArchive = System.getProperty("compose.appcds.create-archive") != null
    println("Running app to create archive: $creatingAppCdsArchive")
    application {
        Window(onCloseRequest = ::exitApplication) {
            MaterialTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Running to create AppCDS archive: $creatingAppCdsArchive")
                }
            }
        }

        LaunchedEffect(Unit) {
            delay(2.seconds)
            exitApplication()
        }
    }
}
