package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.ExperimentalTestApi
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalTestApi::class, InternalResourceApi::class)
class ComposeResourceTest {

    init {
        getResourceEnvironment = ::getTestEnvironment
    }

    @Test
    fun testCountRecompositions() = clearResourceCachesAndRunUiTest {
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
    fun testImageResourceCache() = clearResourceCachesAndRunUiTest {
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

        ResourceCaches.clear()

        res = TestDrawableResource("2.png")
        waitForIdle()
        res = TestDrawableResource("1.png")
        waitForIdle()

        assertEquals(
            expected = listOf("1.png", "2.png", "2.png", "1.png"), // read images again
            actual = testResourceReader.readPaths
        )
    }

    @Test
    fun testImageResourceDensity() = clearResourceCachesAndRunUiTest {
        val testResourceReader = TestResourceReader()
        val imgRes = DrawableResource(
            "test_id", setOf(
                ResourceItem(setOf(DensityQualifier.XXXHDPI), "2.png", -1, -1),
                ResourceItem(setOf(DensityQualifier.MDPI), "1.png", -1, -1),
            )
        )
        val mdpiEnvironment = object : ComposeEnvironment {
            @Composable
            override fun rememberEnvironment() = ResourceEnvironment(
                language = LanguageQualifier("en"),
                region = RegionQualifier("US"),
                theme = ThemeQualifier.LIGHT,
                density = DensityQualifier.MDPI
            )
        }

        var environment by mutableStateOf(TestComposeEnvironment)
        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides environment
            ) {
                Image(painterResource(imgRes), null)
            }
        }
        waitForIdle()
        environment = mdpiEnvironment
        waitForIdle()

        assertEquals(
            expected = listOf("2.png", "1.png"), //XXXHDPI - fist, MDPI - next
            actual = testResourceReader.readPaths
        )
    }

    @Test
    fun testStringResourceCache() = clearResourceCachesAndRunUiTest {
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

        ResourceCaches.clear()

        res = TestStringResource("hello")
        waitForIdle()
        assertEquals(str, "\uD83D\uDE0A Hello world!")
        assertEquals(
            expected = listOf(
                "strings.cvr/314-44",
                "strings.cvr/211-47",
                "strings.cvr/359-37",
                "strings.cvr/359-37",
            ), //read hello item again
            actual = testResourceReader.readPaths
        )

    }

    @Test
    fun testReadStringResource() = clearResourceCachesAndRunUiTest {
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
    fun testReadStringFromDifferentArgs() = clearResourceCachesAndRunUiTest {
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
    fun testPluralStringResourceCache() = clearResourceCachesAndRunUiTest {
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
    fun testReadPluralStringResource() = clearResourceCachesAndRunUiTest {
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
    fun testReadQualityStringFromDifferentArgs() = clearResourceCachesAndRunUiTest {
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
    fun testGetResourceUri() = clearResourceCachesAndRunUiTest {
        var uri1 = ""
        var uri2 = ""
        setContent {
            CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                val resourceReader = LocalResourceReader.currentOrPreview
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
        val env = getSystemResourceEnvironment()
        val imageBytes = getDrawableResourceBytes(env, TestDrawableResource("1.png"))
        assertEquals(946, imageBytes.size)
        val fontBytes = getFontResourceBytes(env, TestFontResource("font_awesome.otf"))
        assertEquals(134808, fontBytes.size)
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun testGetResourceEnvironment() = clearResourceCachesAndRunUiTest {
        var environment: ResourceEnvironment? = null
        setContent {
            CompositionLocalProvider(LocalComposeEnvironment provides TestComposeEnvironment) {
                environment = rememberResourceEnvironment()
            }
        }
        waitForIdle()

        val systemEnvironment = getSystemResourceEnvironment()
        assertEquals(systemEnvironment, environment)
    }

    @Test
    fun rememberResourceStateAffectedByEnvironmentChanges() = clearResourceCachesAndRunUiTest {
        val env2 = ResourceEnvironment(
            language = LanguageQualifier("en"),
            region = RegionQualifier("CA"),
            theme = ThemeQualifier.DARK,
            density = DensityQualifier.MDPI
        )

        val envState = mutableStateOf(TestComposeEnvironment)
        var lastEnv1: ResourceEnvironment? = null
        var lastEnv2: ResourceEnvironment? = null
        var lastEnv3: ResourceEnvironment? = null

        setContent {
            CompositionLocalProvider(LocalComposeEnvironment provides envState.value) {
                rememberResourceState(1, { "" }) {
                    lastEnv1 = it
                }
                rememberResourceState(1, 2,  { "" }) {
                    lastEnv2 = it
                }
                rememberResourceState(1, 2, 3,  { "" }) {
                    lastEnv3 = it
                }
            }
        }

        assertNotEquals(null, lastEnv1)
        assertNotEquals(env2, lastEnv1)
        assertEquals(lastEnv1, lastEnv2)
        assertEquals(lastEnv2, lastEnv3)

        val testEnv2 = object : ComposeEnvironment {
            @Composable
            override fun rememberEnvironment() = env2
        }
        envState.value = testEnv2
        waitForIdle()

        assertEquals(env2, lastEnv1)
        assertEquals(env2, lastEnv2)
        assertEquals(env2, lastEnv3)
    }
}
