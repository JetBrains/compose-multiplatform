package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalResourceApi::class, ExperimentalTestApi::class)
class ComposeResourceTest {

    @Before
    fun dropCaches() {
        dropStringsCache()
        dropImageCache()
    }

    @Test
    fun testCountRecompositions() = runComposeUiTest {
        runBlockingTest {
            val imagePathFlow = MutableStateFlow(ImageResource("1.png"))
            val recompositionsCounter = RecompositionsCounter()
            setContent {
                val res by imagePathFlow.collectAsState()
                val imgRes = imageResource(res)
                recompositionsCounter.content {
                    Image(bitmap = imgRes, contentDescription = null)
                }
            }
            awaitIdle()
            imagePathFlow.emit(ImageResource("2.png"))
            awaitIdle()
            assertEquals(2, recompositionsCounter.count)
        }
    }

    @Test
    fun testImageResourceCache() = runComposeUiTest {
        runBlockingTest {
            val testResourceReader = TestResourceReader()
            val imagePathFlow = MutableStateFlow(ImageResource("1.png"))
            setContent {
                CompositionLocalProvider(LocalResourceReader provides testResourceReader) {
                    val res by imagePathFlow.collectAsState()
                    Image(painterResource(res), null)
                }
            }
            awaitIdle()
            imagePathFlow.emit(ImageResource("2.png"))
            awaitIdle()
            imagePathFlow.emit(ImageResource("1.png"))
            awaitIdle()

            assertEquals(
                expected = listOf("1.png", "2.png"), //no second read of 1.png
                actual = testResourceReader.readPaths
            )
        }
    }

    @Test
    fun testStringResourceCache() = runComposeUiTest {
        runBlockingTest {
            val testResourceReader = TestResourceReader()
            val stringIdFlow = MutableStateFlow(TestStringResource("app_name"))
            setContent {
                CompositionLocalProvider(LocalResourceReader provides testResourceReader) {
                    val res by stringIdFlow.collectAsState()
                    Text(stringResource(res))
                    Text(stringArrayResource(TestStringResource("str_arr")).joinToString())
                }
            }
            awaitIdle()
            stringIdFlow.emit(TestStringResource("hello"))
            awaitIdle()
            stringIdFlow.emit(TestStringResource("app_name"))
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
                assertEquals("Compose Resources App", stringResource(TestStringResource("app_name")))
                assertEquals(
                    "Hello, test-name! You have 42 new messages.",
                    stringResource(TestStringResource("str_template"), "test-name", 42)
                )
                assertEquals(listOf("item 1", "item 2", "item 3"), stringArrayResource(TestStringResource("str_arr")))
            }
            awaitIdle()
        }
    }

    @Test
    fun testLoadStringResource() = runBlockingTest {
        kotlin.test.assertEquals("Compose Resources App", getString(TestStringResource("app_name")))
        kotlin.test.assertEquals(
            "Hello, test-name! You have 42 new messages.",
            getString(TestStringResource("str_template"), "test-name", 42)
        )
        kotlin.test.assertEquals(listOf("item 1", "item 2", "item 3"), getStringArray(TestStringResource("str_arr")))
    }

    @Test
    fun testMissingResource() = runBlockingTest {
        assertFailsWith<MissingResourceException> {
            readResourceBytes("missing.png")
        }
        val error = assertFailsWith<IllegalStateException> {
            getString(TestStringResource("unknown_id"))
        }
        kotlin.test.assertEquals("String ID=`unknown_id` is not found!", error.message)
    }

    @Test
    fun testReadFileResource() = runBlockingTest {
        val bytes = readResourceBytes("strings.xml")
        kotlin.test.assertEquals(
            """
                <resources>
                    <string name="app_name">Compose Resources App</string>
                    <string name="hello">😊 Hello world!</string>
                    <string name="str_template">Hello, %1${'$'}s! You have %2${'$'}d new messages.</string>
                    <string-array name="str_arr">
                        <item>item 1</item>
                        <item>item 2</item>
                        <item>item 3</item>
                    </string-array>
                </resources>
                
            """.trimIndent(),
            bytes.decodeToString()
        )
    }
}
