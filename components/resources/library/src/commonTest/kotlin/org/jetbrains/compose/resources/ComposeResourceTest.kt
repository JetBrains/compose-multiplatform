package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.*

@OptIn(ExperimentalTestApi::class, ExperimentalResourceApi::class, InternalResourceApi::class)
class ComposeResourceTest {

    init {
        dropStringsCache()
        dropImageCache()
        getResourceEnvironment = ::getTestEnvironment
    }

    @Test
    fun testCountRecompositions() = runComposeUiTest {
        runBlockingTest {
            val imagePathFlow = MutableStateFlow(DrawableResource("1.png"))
            val recompositionsCounter = RecompositionsCounter()
            setContent {
                CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                    val res by imagePathFlow.collectAsState()
                    val imgRes = imageResource(res)
                    recompositionsCounter.content {
                        Image(bitmap = imgRes, contentDescription = null)
                    }
                }
            }
            awaitIdle()
            imagePathFlow.emit(DrawableResource("2.png"))
            awaitIdle()
            assertEquals(2, recompositionsCounter.count)
        }
    }

    @Test
    fun testImageResourceCache() = runComposeUiTest {
        runBlockingTest {
            val testResourceReader = TestResourceReader()
            val imagePathFlow = MutableStateFlow(DrawableResource("1.png"))
            setContent {
                CompositionLocalProvider(
                    LocalResourceReader provides testResourceReader,
                    LocalComposeEnvironment provides TestComposeEnvironment
                ) {
                    val res by imagePathFlow.collectAsState()
                    Image(painterResource(res), null)
                }
            }
            awaitIdle()
            imagePathFlow.emit(DrawableResource("2.png"))
            awaitIdle()
            imagePathFlow.emit(DrawableResource("1.png"))
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
                CompositionLocalProvider(
                    LocalResourceReader provides testResourceReader,
                    LocalComposeEnvironment provides TestComposeEnvironment
                ) {
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
                CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                    assertEquals(
                        "Compose Resources App",
                        stringResource(TestStringResource("app_name"))
                    )
                    assertEquals(
                        "Créer une table",
                        stringResource(TestStringResource("accentuated_characters"))
                    )
                    assertEquals(
                        "Hello, test-name! You have 42 new messages.",
                        stringResource(TestStringResource("str_template"), "test-name", 42)
                    )
                    assertEquals(
                        listOf("item 1", "item 2", "item 3"),
                        stringArrayResource(TestStringResource("str_arr"))
                    )
                }
            }
            awaitIdle()
        }
    }

    @Test
    fun testLoadStringResource() = runBlockingTest {
        assertEquals("Compose Resources App", getString(TestStringResource("app_name")))
        assertEquals(
            "Hello, test-name! You have 42 new messages.",
            getString(TestStringResource("str_template"), "test-name", 42)
        )
        assertEquals(listOf("item 1", "item 2", "item 3"), getStringArray(TestStringResource("str_arr")))
    }

    @Test
    fun testMissingResource() = runBlockingTest {
        assertFailsWith<MissingResourceException> {
            readResourceBytes("missing.png")
        }
        val error = assertFailsWith<IllegalStateException> {
            getString(TestStringResource("unknown_id"))
        }
        assertEquals("String ID=`unknown_id` is not found!", error.message)
    }

    @Test
    fun testReadFileResource() = runBlockingTest {
        val bytes = readResourceBytes("strings.xml")
        assertEquals(
            """
                <resources>
                    <string name="app_name">Compose Resources App</string>
                    <string name="hello">😊 Hello world!</string>
                    <string name="accentuated_characters">Créer une table</string>
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
