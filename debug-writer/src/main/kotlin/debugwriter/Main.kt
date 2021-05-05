package debugwriter

import androidx.compose.desktop.LocalAppWindow
import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import debugwriter.decoration.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

private val fileName = "${System.getProperty("user.home")}/.debug-writer/debug-info.txt"
private var output by mutableStateOf("")
private var isReady by mutableStateOf(true)

fun main() {
    val result = enableDebugWritingTo(fileName)
    if (!result) {
        output = "Failed to cteate file: $fileName"
    }

    Window(
        title = "DebugWriter",
        size = IntSize(650, 450)
    ) {
        val window = LocalAppWindow.current

        AppTheme {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Header("Click [Refresh] to refresh info or [Open file] to see the output file.")
                        if (isReady) {
                            TextBox(output, Modifier.weight(1f).padding(start = 30.dp, end = 30.dp))
                        } else {
                            Loader(Modifier.weight(1f).fillMaxWidth())
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button("Refresh", Modifier.weight(1f), { writeDebugInfo() })
                            Button(
                                text = "Open file",
                                modifier = Modifier.weight(1f),
                                action = {
                                    if(!revealDebugOutput(fileName)) {
                                        output = "Failed to open file: $fileName"
                                    }
                                }
                            )
                            Button("Close", Modifier.weight(1f), { window.close() })
                        }
                    }
                }
            }
        }

        if (result) {
            writeDebugInfo()
        }
    }
}

private fun writeDebugInfo() {
    isReady = false
    GlobalScope.async {
        delay(2000L)
        isReady = true
        val result = readDebugOutput(fileName)
        output = if (result.isEmpty())
                "Something went wrong and $fileName is empty."
            else result
    }
}