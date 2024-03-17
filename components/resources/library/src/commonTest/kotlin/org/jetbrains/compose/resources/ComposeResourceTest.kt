package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
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
        var res by mutableStateOf(DrawableResource("1.png"))
        val recompositionsCounter = RecompositionsCounter()
        setContent {
            CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                val imgRes = imageResource(res)
                recompositionsCounter.content {
                    Image(bitmap = imgRes, contentDescription = null)
                }
            }
        }
        waitForIdle()
        res = DrawableResource("2.png")
        waitForIdle()
        assertEquals(2, recompositionsCounter.count)
    }

    @Test
    fun testImageResourceCache() = runComposeUiTest {
        val testResourceReader = TestResourceReader()
        var res by mutableStateOf(DrawableResource("1.png"))
        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                Image(painterResource(res), null)
            }
        }
        waitForIdle()
        res = DrawableResource("2.png")
        waitForIdle()
        res = DrawableResource("1.png")
        waitForIdle()

        assertEquals(
            expected = listOf("1.png", "2.png"), //no second read of 1.png
            actual = testResourceReader.readPaths
        )
    }

    @Test
    fun testStringResourceCache() = runComposeUiTest {
        val testResourceReader = TestResourceReader()
        var res by mutableStateOf(TestStringResource("app_name"))
        var str = ""
        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                str = stringResource(res)
                Text(str)
                Text(stringArrayResource(TestStringResource("str_arr")).joinToString())
            }
        }
        waitForIdle()
        assertEquals(str, "Compose Resources App")
        res = TestStringResource("hello")
        waitForIdle()
        assertEquals(str, "\uD83D\uDE0A Hello world!")
        res = TestStringResource("app_name")
        waitForIdle()
        assertEquals(str, "Compose Resources App")

        assertEquals(
            expected = listOf("strings.xml"), //just one string.xml read
            actual = testResourceReader.readPaths
        )
    }

    @Test
    fun testReadStringResource() = runComposeUiTest {
        var app_name = ""
        var accentuated_characters = ""
        var str_template = ""
        var str_arr = emptyList<String>()
        setContent {
            CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                app_name = stringResource(TestStringResource("app_name"))
                accentuated_characters = stringResource(TestStringResource("accentuated_characters"))
                str_template = stringResource(TestStringResource("str_template"), "test-name", 42)
                str_arr = stringArrayResource(TestStringResource("str_arr"))
            }
        }
        waitForIdle()

        assertEquals("Compose Resources App", app_name)
        assertEquals("Créer une table", accentuated_characters)
        assertEquals("Hello, test-name! You have 42 new messages.", str_template)
        assertEquals(listOf("item 1", "item 2", "item 3"), str_arr)
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/4325
    @Test
    fun testReadStringFromDifferentArgs() = runComposeUiTest {
        var arg by mutableStateOf(42)
        var str1 = ""
        var str2 = ""
        setContent {
            CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                str1 = stringResource(TestStringResource("str_template"), "test1", arg)
                str2 = stringResource(TestStringResource("str_template"), "test2", arg)
            }
        }

        waitForIdle()
        assertEquals("Hello, test1! You have 42 new messages.", str1)
        assertEquals("Hello, test2! You have 42 new messages.", str2)

        arg = 31415
        waitForIdle()
        assertEquals("Hello, test1! You have 31415 new messages.", str1)
        assertEquals("Hello, test2! You have 31415 new messages.", str2)
    }

    @Test
    fun testLoadStringResource() = runTest {
        assertEquals("Compose Resources App", getString(TestStringResource("app_name")))
        assertEquals(
            "Hello, test-name! You have 42 new messages.",
            getString(TestStringResource("str_template"), "test-name", 42)
        )
        assertEquals(listOf("item 1", "item 2", "item 3"), getStringArray(TestStringResource("str_arr")))
    }

    @Test
    fun testMissingResource() = runTest {
        assertFailsWith<MissingResourceException> {
            readResourceBytes("missing.png")
        }
        assertFailsWith<MissingResourceException> {
            getResourceAsFlow("missing.png").collect()
        }
        val error = assertFailsWith<IllegalStateException> {
            getString(TestStringResource("unknown_id"))
        }
        assertEquals("String ID=`unknown_id` is not found!", error.message)
    }

    @Test
    fun testReadFileResource() = runTest {
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

    @Test
    fun testGetFileResourceAsSource() = runTest {
        val bytes = readResourceBytes("strings.xml")
        val source = mutableListOf<Byte>()
        getResourceAsFlow("strings.xml").collect { chunk ->
            source.addAll(chunk.asList())
        }
        assertContentEquals(bytes, source.toByteArray())
    }
}
