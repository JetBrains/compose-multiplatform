package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalResourceApi::class, ExperimentalTestApi::class)
class ComposeResourceTest {

    @Before
    fun dropCaches() {
        dropStringsCache()
        dropBytesCache()
    }

    @Test
    fun testCountRecompositions() = runComposeUiTest {
        runBlockingTest {
            val imagePathFlow = MutableStateFlow("1.png")
            val recompositionsCounter = RecompositionsCounter()
            setContent {
                val path by imagePathFlow.collectAsState()
                val res = imageResource(path)
                recompositionsCounter.content {
                    Image(bitmap = res, contentDescription = null)
                }
            }
            awaitIdle()
            imagePathFlow.emit("2.png")
            awaitIdle()
            assertEquals(2, recompositionsCounter.count)
        }
    }

    @Test
    fun testImageResourceCache() = runComposeUiTest {
        runBlockingTest {
            val testResourceReader = TestResourceReader()
            val imagePathFlow = MutableStateFlow("1.png")
            setContent {
                CompositionLocalProvider(LocalResourceReader provides testResourceReader) {
                    val path by imagePathFlow.collectAsState()
                    Image(painterResource(path), null)
                }
            }
            awaitIdle()
            imagePathFlow.emit("2.png")
            awaitIdle()
            imagePathFlow.emit("1.png")
            awaitIdle()

            assertEquals(
                expected = listOf("1.png", "2.png"), //no second read of 1.png
                actual = testResourceReader.readPaths
            )
        }
    }

    @Test
    fun testFontResourceCache() = runComposeUiTest {
        runBlockingTest {
            val testResourceReader = TestResourceReader()
            setContent {
                CompositionLocalProvider(LocalResourceReader provides testResourceReader) {
                    Text(text = "F1", fontFamily = FontFamily(Font("font_awesome.otf")))
                    Text(text = "F2", fontFamily = FontFamily(Font("font_awesome.otf")))
                    Text(text = "F3", fontFamily = FontFamily(Font("font_awesome.otf")))
                }
            }
            awaitIdle()

            assertEquals(
                expected = listOf(), //android caches fonts by android specific logic
                actual = testResourceReader.readPaths
            )
        }
    }

    @Test
    fun testStringResourceCache() = runComposeUiTest {
        runBlockingTest {
            val testResourceReader = TestResourceReader()
            val stringIdFlow = MutableStateFlow("app_name")
            setContent {
                CompositionLocalProvider(LocalResourceReader provides testResourceReader) {
                    val textId by stringIdFlow.collectAsState()
                    Text(getString(textId))
                    Text(getStringArray("str_arr").joinToString())
                }
            }
            awaitIdle()
            stringIdFlow.emit("hello")
            awaitIdle()
            stringIdFlow.emit("app_name")
            awaitIdle()

            assertEquals(
                expected = listOf("strings.xml"), //just one string.xml read
                actual = testResourceReader.readPaths
            )
        }
    }

    @Test
    fun testReadStringResource() = runComposeUiTest {
        runBlockingTest {
            setContent {
                assertEquals("Compose Resources App", getString("app_name"))
                assertEquals(
                    "Hello, test-name! You have 42 new messages.",
                    getString("str_template", "test-name", 42)
                )
                assertEquals(listOf("item 1", "item 2", "item 3"), getStringArray("str_arr"))
            }
            awaitIdle()
        }
    }
}
