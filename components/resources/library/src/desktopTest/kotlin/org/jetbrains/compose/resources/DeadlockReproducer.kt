package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DeadlockReproducer {

    @Test
    fun deadlockReproducer() {
        val deadlockResourceReader = object : ResourceReader by DefaultResourceReader {
            override suspend fun read(path: String): ByteArray {
                val arr = DefaultResourceReader.read(path)
                return withContext(Dispatchers.Main) { arr }
            }
        }
        val isComposed = AtomicBoolean(false)
        val isDeadlocked = AtomicBoolean(true)

        val testThread = thread {
            runComposeUiTest {
                setContent {
                    CompositionLocalProvider(
                        LocalResourceReader provides deadlockResourceReader,
                        LocalComposeEnvironment provides TestComposeEnvironment
                    ) {
                        isComposed.set(true)
                        Image(painterResource(TestDrawableResource("1.png")), null)
                    }
                }
                isDeadlocked.set(false)
            }
        }

        testThread.join(300)
        assertTrue(isComposed.get(), "Composition is not composed")
        assertTrue(isDeadlocked.get(), "Deadlock was not detected")
    }

    @Test
    fun noDeadlockReproducer() {
        val isComposed = AtomicBoolean(false)
        val isDeadlocked = AtomicBoolean(true)

        val testThread = thread {
            runComposeUiTest {
                setContent {
                    CompositionLocalProvider(
                        LocalResourceReader provides DefaultResourceReader,
                        LocalComposeEnvironment provides TestComposeEnvironment
                    ) {
                        isComposed.set(true)
                        Image(painterResource(TestDrawableResource("1.png")), null)
                    }
                }
                isDeadlocked.set(false)
            }
        }

        testThread.join(300)
        assertTrue(isComposed.get(), "Composition is not composed")
        assertFalse(isDeadlocked.get(), "Deadlock was detected")
    }
}
