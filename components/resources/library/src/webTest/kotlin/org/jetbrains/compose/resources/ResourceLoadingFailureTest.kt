package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that a failure while loading a single resource is handled gracefully on web:
 * the loading exception is swallowed by [rememberResourceState] (see ResourceState.web.kt),
 * the composable keeps its default value and the rest of the UI keeps working.
 *
 * Resource paths are substituted via [LocalResourceReader]: a [FailingResourceReader] throws
 * for the chosen path and delegates everything else to the real reader.
 */
@OptIn(ExperimentalTestApi::class)
class ResourceLoadingFailureTest {

    /** A [ResourceReader] that fails for [failingPath] and delegates the rest. */
    private class FailingResourceReader(
        private val failingPath: String,
        private val delegate: ResourceReader = DefaultResourceReader,
    ) : ResourceReader {
        private val attempted = mutableListOf<String>()
        val attemptedPaths: List<String> get() = attempted

        override suspend fun read(path: String): ByteArray {
            attempted.add(path)
            if (path == failingPath) error("Failed to load resource: $path")
            return delegate.read(path)
        }

        override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
            attempted.add(path)
            if (path == failingPath) error("Failed to load resource: $path")
            return delegate.readPart(path, offset, size)
        }

        override fun getUri(path: String): String = delegate.getUri(path)
    }

    @Test
    fun testStringResourceLoadingFailureKeepsDefault() = clearResourceCachesAndRunUiTest {
        // Strings are read from the packed "strings.cvr" file via readPart.
        val failingReader = FailingResourceReader(failingPath = "strings.cvr")
        var text = "<initial>"

        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides failingReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                text = stringResource(TestStringResource("app_name"))
                Text(text)
            }
        }

        waitResources()

        assertTrue("strings.cvr" in failingReader.attemptedPaths, "Loading should have been attempted")
        assertEquals("", text, "A failed string resource must fall back to the empty default value")
    }

    @Test
    fun testDrawableResourceLoadingFailureDoesNotCrash() = clearResourceCachesAndRunUiTest {
        val failingReader = FailingResourceReader(failingPath = "1.png")

        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides failingReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                // A failed image load must not crash the composition; it stays an empty painter.
                Image(painterResource(TestDrawableResource("1.png")), null)
            }
        }

        waitResources()

        assertTrue("1.png" in failingReader.attemptedPaths, "Loading should have been attempted")
    }

    /**
     * A [ResourceReader] that fails while [shouldFail] is `true` and succeeds once it is flipped.
     * Lets a test reproduce a transient loading error and then a successful retry.
     */
    private class FlakyResourceReader(
        var shouldFail: Boolean = true,
        private val delegate: ResourceReader = DefaultResourceReader,
    ) : ResourceReader {
        var readAttempts = 0
            private set

        override suspend fun read(path: String): ByteArray {
            readAttempts++
            if (shouldFail) throw MissingResourceException(path)
            return delegate.read(path)
        }

        override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
            readAttempts++
            if (shouldFail) throw MissingResourceException(path)
            return delegate.readPart(path, offset, size)
        }

        override fun getUri(path: String): String = delegate.getUri(path)
    }

    @Test
    fun testResourceReloadsSuccessfullyAfterFailure() = clearResourceCachesAndRunUiTest {
        val flakyReader = FlakyResourceReader(shouldFail = true)
        val res = TestStringResource("app_name")
        var text = "<initial>"
        // Bumping this key remounts the content, which forces rememberResourceState to load again.
        var reloadKey by mutableStateOf(0)

        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides flakyReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                key(reloadKey) {
                    text = stringResource(res)
                    Text(text)
                }
            }
        }

        // First pass: the reader fails, so the value falls back to the empty default.
        waitResources()
        assertEquals("", text, "While loading fails the value must stay the empty default")
        val attemptsAfterFirstPass = flakyReader.readAttempts
        assertTrue(attemptsAfterFirstPass > 0, "The resource should have been read on the first pass")

        // Flip the flag and trigger a second loading attempt of the same resource.
        flakyReader.shouldFail = false
        reloadKey = 1

        // Second pass: loading now succeeds and the real value appears.
        waitResources()
        assertTrue(
            flakyReader.readAttempts > attemptsAfterFirstPass,
            "The resource should have been re-read on the second pass"
        )
        assertEquals(
            "Compose Resources App",
            text,
            "After the flag is flipped the resource must load successfully on the retry"
        )
    }

    @Test
    fun testOneFailingResourceDoesNotAffectOthers() = clearResourceCachesAndRunUiTest {
        // Only "1.png" fails; the string resource (from "strings.cvr") must still load fine.
        val failingReader = FailingResourceReader(failingPath = "1.png")
        var text = "<initial>"

        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides failingReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                Image(painterResource(TestDrawableResource("1.png")), null) // fails
                text = stringResource(TestStringResource("app_name"))       // succeeds
                Text(text)
            }
        }

        waitResources()

        assertTrue("1.png" in failingReader.attemptedPaths, "The failing resource should have been attempted")
        assertEquals(
            "Compose Resources App",
            text,
            "A failure of one resource must not prevent other resources from loading"
        )
    }
}
