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
    fun testPluralStringResourceCache() = runComposeUiTest {
        val testResourceReader = TestResourceReader()
        var res by mutableStateOf(TestPluralStringResource("plurals"))
        var quantity by mutableStateOf(0)
        var str = ""
        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                str = pluralStringResource(res, quantity)
            }
        }

        waitForIdle()
        assertEquals("other", str)

        quantity = 1
        waitForIdle()
        assertEquals("one", str)
        assertEquals(1, quantity)

        quantity = 2
        waitForIdle()
        assertEquals("other", str)
        assertEquals(2, quantity)

        quantity = 3
        waitForIdle()
        assertEquals("other", str)
        assertEquals(3, quantity)

        res = TestPluralStringResource("another_plurals")
        quantity = 0
        waitForIdle()
        assertEquals("another other", str)

        quantity = 1
        waitForIdle()
        assertEquals("another one", str)
    }

    @Test
    fun testReadPluralStringResource() = runComposeUiTest {
        var plurals = ""
        var another_plurals = ""
        setContent {
            CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                plurals = pluralStringResource(TestPluralStringResource("plurals"), 1)
                another_plurals = pluralStringResource(TestPluralStringResource("another_plurals"), 1)
            }
        }
        waitForIdle()

        assertEquals("one", plurals)
        assertEquals("another one", another_plurals)
    }

    @Test
    fun testReadQualityStringFromDifferentArgs() = runComposeUiTest {
        // we're putting different integers to arguments and the quantity
        var quantity by mutableStateOf(0)

        var arg by mutableStateOf("me")
        var str1 = ""
        var str2 = ""
        setContent {
            CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                str1 = pluralStringResource(TestPluralStringResource("messages"), quantity, 3, arg)
                str2 = pluralStringResource(TestPluralStringResource("messages"), quantity, 5, arg)
            }
        }
        waitForIdle()
        assertEquals("3 messages for me", str1)
        assertEquals("5 messages for me", str2)

        arg = "you"
        waitForIdle()
        assertEquals("3 messages for you", str1)
        assertEquals("5 messages for you", str2)

        quantity = 1
        waitForIdle()
        assertEquals("3 message for you", str1)
        assertEquals("5 message for you", str2)
    }

    @Test
    fun testLoadPluralStringResource() = runTest {
        assertEquals("one", getPluralString(TestPluralStringResource("plurals"), 1))
        assertEquals("other", getPluralString(TestPluralStringResource("plurals"), 5))
        assertEquals("another one", getPluralString(TestPluralStringResource("another_plurals"), 1))
        assertEquals("another other", getPluralString(TestPluralStringResource("another_plurals"), 5))
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
                    <plurals name="plurals">
                        <item quantity="one">one</item>
                        <item quantity="other">other</item>
                    </plurals>
                    <plurals name="another_plurals">
                        <item quantity="one">another one</item>
                        <item quantity="other">another other</item>
                    </plurals>
                    <plurals name="messages">
                        <item quantity="one">%1${'$'}d message for %2${'$'}s</item>
                        <item quantity="other">%1${'$'}d messages for %2${'$'}s</item>
                    </plurals>
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
            println("p")
            source.addAll(chunk.asList())
            println("q")
        }
        println("r")
        assertContentEquals(bytes, source.toByteArray())
    }
}
