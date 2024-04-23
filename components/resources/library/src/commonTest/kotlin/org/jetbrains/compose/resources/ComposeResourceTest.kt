package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalTestApi::class, InternalResourceApi::class)
class ComposeResourceTest {

    init {
        dropStringItemsCache()
        dropImageCache()
        getResourceEnvironment = ::getTestEnvironment
    }

    @Test
    fun testCountRecompositions() = runComposeUiTest {
        var res by mutableStateOf(TestDrawableResource("1.png"))
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
        res = TestDrawableResource("2.png")
        waitForIdle()
        assertEquals(2, recompositionsCounter.count)
    }

    @Test
    fun testImageResourceCache() = runComposeUiTest {
        val testResourceReader = TestResourceReader()
        var res by mutableStateOf(TestDrawableResource("1.png"))
        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                Image(painterResource(res), null)
            }
        }
        waitForIdle()
        res = TestDrawableResource("2.png")
        waitForIdle()
        res = TestDrawableResource("1.png")
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
                Text(stringArrayResource(TestStringArrayResource("str_arr")).joinToString())
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
        res = TestStringResource("hello")
        waitForIdle()
        assertEquals(str, "\uD83D\uDE0A Hello world!")
        res = TestStringResource("app_name")
        waitForIdle()
        assertEquals(str, "Compose Resources App")
        res = TestStringResource("hello")
        waitForIdle()
        assertEquals(str, "\uD83D\uDE0A Hello world!")
        res = TestStringResource("app_name")
        waitForIdle()
        assertEquals(str, "Compose Resources App")

        assertEquals(
            expected = listOf(
                "strings.cvr/314-44",
                "strings.cvr/211-47",
                "strings.cvr/359-37"
            ), //only three different
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
                str_arr = stringArrayResource(TestStringArrayResource("str_arr"))
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
        assertEquals(listOf("item 1", "item 2", "item 3"), getStringArray(TestStringArrayResource("str_arr")))
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
        val error = assertFailsWith<IllegalStateException> {
            getString(TestStringResource("unknown_id"))
        }
        assertEquals("String ID=`unknown_id` is not found!", error.message)
    }

    @Test
    fun testReadFileResource() = runTest {
        val bytes = readResourceBytes("strings.cvr")
        assertEquals(
            """
                version:0
                plurals|another_plurals|ONE:YW5vdGhlciBvbmU=,OTHER:YW5vdGhlciBvdGhlcg==
                plurals|messages|ONE:JTEkZCBtZXNzYWdlIGZvciAlMiRz,OTHER:JTEkZCBtZXNzYWdlcyBmb3IgJTIkcw==
                plurals|plurals|ONE:b25l,OTHER:b3RoZXI=
                string-array|str_arr|aXRlbSAx,aXRlbSAy,aXRlbSAz
                string|accentuated_characters|Q3LDqWVyIHVuZSB0YWJsZQ==
                string|app_name|Q29tcG9zZSBSZXNvdXJjZXMgQXBw
                string|hello|8J+YiiBIZWxsbyB3b3JsZCE=
                string|str_template|SGVsbG8sICUxJHMhIFlvdSBoYXZlICUyJGQgbmV3IG1lc3NhZ2VzLg==
                
            """.trimIndent(),
            bytes.decodeToString()
        )
    }

    @Test
    fun testGetResourceUri() = runComposeUiTest {
        var uri1 = ""
        var uri2 = ""
        setContent {
            CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                val resourceReader = LocalResourceReader.current
                uri1 = resourceReader.getUri("1.png")
                uri2 = resourceReader.getUri("2.png")
            }
        }
        waitForIdle()

        assertTrue(uri1.endsWith("/1.png"))
        assertTrue(uri2.endsWith("/2.png"))
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun testGetResourceBytes() = runTest {
        val env = getSystemEnvironment()
        val imageBytes = getDrawableResourceBytes(env, TestDrawableResource("1.png"))
        assertEquals(946, imageBytes.size)
        val fontBytes = getFontResourceBytes(env, TestFontResource("font_awesome.otf"))
        assertEquals(134808, fontBytes.size)
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun testGetResourceEnvironment() = runComposeUiTest {
        var environment: ResourceEnvironment? = null
        setContent {
            environment = rememberResourceEnvironment()
        }
        waitForIdle()

        val systemEnvironment = getSystemEnvironment()
        assertEquals(systemEnvironment, environment)
    }
}
