@file:OptIn(ExperimentalTestApi::class)

package org.jetbrains.compose.resources

import androidx.compose.ui.test.ExperimentalTestApi
import kotlinx.coroutines.test.runTest
import java.net.URLClassLoader
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertEquals


class CustomClassloaderTest {

    val RESOURCES_PATH
        get() = System.getenv("RESOURCES_PATH")
            ?.let { Path(it) }
            ?.takeIf { it.isDirectory() }
            ?: error("RESOURCES_PATH environment variable is not set or is not a directory")

    @Test
    fun testCustomClassloader() = runTest {
        val actualResourceText =
            CustomClassloaderTest::class
                .java
                .classLoader
                .getResourceAsStream("hello.txt")
                ?.readAllBytes()
                ?.toString(Charsets.UTF_8)

        val classloader = URLClassLoader(arrayOf(RESOURCES_PATH.toUri().toURL()), null)
        val reader = JvmResourceReader(classloader)
        assertEquals(
            expected = actualResourceText,
            actual = reader.read("hello.txt").toString(Charsets.UTF_8)
        )
    }
}