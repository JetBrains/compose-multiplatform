package org.jetbrains.compose.resources

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.font.Font
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalEncodingApi::class)
class TestResourcePreloading {

    @Test
    fun testPreloadFont() = runComposeUiTest {
        var loadContinuation: CancellableContinuation<ByteArray>? = null

        val resLoader = object : ResourceReader {
            override suspend fun read(path: String): ByteArray {
                return suspendCancellableCoroutine {
                    loadContinuation = it
                }
            }

            override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
                TODO("Not yet implemented")
            }

            override fun getUri(path: String): String {
                TODO("Not yet implemented")
            }
        }

        var font: Font? = null
        var font2: Font? = null
        var condition by mutableStateOf(false)


        setContent {
            CompositionLocalProvider(
                LocalComposeEnvironment provides TestComposeEnvironment,
                LocalResourceReader provides resLoader
            ) {
                font = preloadFont(TestFontResource("sometestfont")).value

                if (condition) {
                    font2 = Font(TestFontResource("sometestfont"))
                }
            }
        }
        waitForIdle()
        assertEquals(null, font)
        assertEquals(null, font2)

        assertNotEquals(null, loadContinuation)
        loadContinuation!!.resumeWith(Result.success(ByteArray(0)))
        loadContinuation = null

        waitForIdle()
        assertNotEquals(null, font)
        assertEquals(null, font2) // condition was false for now, so font2 should be not initialized

        condition = true
        waitForIdle()
        assertNotEquals(null, font)
        assertEquals(font, font2, "font2 is expected to be loaded from cache")
        assertEquals(null, loadContinuation, "expected no more ResourceReader usages")
    }

    @Test
    fun testIsDefaultCheck() = runComposeUiTest {
        val resLoader = object : ResourceReader {
            override suspend fun read(path: String): ByteArray {
                return suspendCancellableCoroutine {
                    // suspend indefinitely for test purpose
                }
            }

            override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
                TODO("Not yet implemented")
            }

            override fun getUri(path: String): String {
                TODO("Not yet implemented")
            }
        }

        var font: Font? = null

        setContent {
            CompositionLocalProvider(
                LocalComposeEnvironment provides TestComposeEnvironment,
                LocalResourceReader provides resLoader
            ) {
                font = Font(TestFontResource("sometestfont2"))
            }
        }

        waitForIdle()
        assertNotNull(font)
        assertTrue(font.isDefault)
    }
}