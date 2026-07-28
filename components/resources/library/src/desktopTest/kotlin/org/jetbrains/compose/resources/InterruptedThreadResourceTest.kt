package org.jetbrains.compose.resources

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class InterruptedThreadResourceTest {

    init {
        getResourceEnvironment = ::getTestEnvironment
    }

    @AfterTest
    fun tearDown() {
        //don't leak the interruption flag into other tests
        Thread.interrupted()
    }

    @Test
    fun testReadStringResourceOnAnInterruptedThread() = clearResourceCachesAndRunUiTest {
        var str = ""
        var interruptionFlagKept = false

        setContent {
            CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                Thread.currentThread().interrupt()
                str = stringResource(TestStringResource("app_name"))
                interruptionFlagKept = Thread.interrupted()
                Text(str)
            }
        }
        waitResources()

        assertEquals("Compose Resources App", str)
        assertTrue(interruptionFlagKept, "The interruption flag must be kept for its real addressee")
    }
}
