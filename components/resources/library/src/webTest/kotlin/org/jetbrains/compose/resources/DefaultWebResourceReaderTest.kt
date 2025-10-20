package org.jetbrains.compose.resources

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class DefaultWebResourceReaderTest {

    private val reader = DefaultWebResourceReader()

    private val appNameStringRes = TestStringResource("app_name")

    @Test
    fun stringResource() = runComposeUiTest {

        var appName: String by mutableStateOf("")

        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides reader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                appName = stringResource(appNameStringRes)
                Text(appName)
            }
        }

        awaitUntil {
            appName == "Compose Resources App"
        }

        assertEquals("Compose Resources App", appName)
    }

    private suspend fun ComposeUiTest.awaitUntil(
        timeout: Duration = 100.milliseconds,
        block: suspend () -> Boolean
    ) {
        withContext(Dispatchers.Default) {
            withTimeout(timeout) {
                while (!block()) {
                    delay(10)
                    awaitIdle()
                }
            }
        }
    }
}

// Until we have common w3c api between k/js and k/wasm we need to have this expect/actual
internal expect fun DefaultWebResourceReader(): ResourceReader