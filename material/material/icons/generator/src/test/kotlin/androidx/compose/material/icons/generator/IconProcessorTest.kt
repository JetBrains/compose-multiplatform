/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.icons.generator

import com.google.common.truth.Truth
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

/**
 * Test for [IconProcessor].
 */
@RunWith(JUnit4::class)
class IconProcessorTest {

    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    /**
     * Tests that the processed icons match what we expect.
     */
    @Test
    fun iconProcessor_noApiChanges() {
        val iconDirectory = temporaryFolder.createIconDirectory()

        // Write the test icon to each theme folder
        iconDirectory.listFiles()!!.forEach { themeDirectory ->
            themeDirectory.resolve("test_icon.xml").writeText(TestIconFile)
        }

        val expectedApiFile = temporaryFolder.newFile("expected-api.txt").apply {
            writeText(ExpectedApiFile)
        }

        val generatedApiFile = temporaryFolder.newFile("generated-api.txt")

        val processor = IconProcessor(
            iconDirectories = iconDirectory.listFiles()!!.toList(),
            expectedApiFile = expectedApiFile,
            generatedApiFile = generatedApiFile
        )

        val icons = processor.process()

        Truth.assertThat(icons.size).isEqualTo(5)

        icons.forEach { icon ->
            Truth.assertThat(icon.kotlinName).isEqualTo("TestIcon")
            val themePackage = icon.theme.themePackageName
            Truth.assertThat(icon.xmlFileName).isEqualTo("${themePackage}_test_icon")
            Truth.assertThat(icon.fileContent).isEqualTo(ExpectedIconFile)
        }
    }

    /**
     * Tests that an exception is thrown, failing the build, when there are changes between the
     * checked in and generated API files.
     */
    @Test
    fun iconProcessor_apiChanges() {
        val iconDirectory = temporaryFolder.createIconDirectory()

        // Write the test icon to each theme folder
        iconDirectory.listFiles()!!.forEach { themeDirectory ->
            themeDirectory.resolve("test_icon.xml").writeText(TestIconFile)
        }

        // Create an empty expected API file
        val expectedApiFile = temporaryFolder.newFile("expected-api.txt").apply {
            writeText("")
        }

        val generatedApiFile = temporaryFolder.newFile("generated-api.txt")

        val processor = IconProcessor(
            iconDirectories = iconDirectory.listFiles()!!.toList(),
            expectedApiFile = expectedApiFile,
            generatedApiFile = generatedApiFile
        )

        // The generated api file conflicts with the expected api file, so we should throw here
        assertIllegalStateContainingMessage("Found differences when comparing API files") {
            processor.process()
        }
    }

    /**
     * Tests that an exception is thrown, failing the build, when not all themes contain icons.
     */
    @Test
    fun iconProcessor_missingTheme() {
        val iconDirectory = temporaryFolder.createIconDirectory()

        // Write the test icon to all but one theme folder
        iconDirectory.listFiles()!!.forEachIndexed { index, themeDirectory ->
            if (index != 0) {
                themeDirectory.resolve("test_icon.xml").writeText(TestIconFile)
            }
        }

        val expectedApiFile = temporaryFolder.newFile("expected-api.txt").apply {
            writeText(ExpectedIconFile)
        }

        val generatedApiFile = temporaryFolder.newFile("generated-api.txt")

        val processor = IconProcessor(
            iconDirectories = iconDirectory.listFiles()!!.toList(),
            expectedApiFile = expectedApiFile,
            generatedApiFile = generatedApiFile
        )

        // Not all icons exist in all themes, so we should throw here
        assertIllegalStateContainingMessage("Some themes were missing") {
            processor.process()
        }
    }

    /**
     * Tests that an exception is thrown, failing the build, when the number of icons in each
     * theme is not the same.
     */
    @Test
    fun iconProcessor_missingIcons() {
        val iconDirectory = temporaryFolder.createIconDirectory()

        // Write the test icon to all themes
        iconDirectory.listFiles()!!.forEach { themeDirectory ->
            themeDirectory.resolve("test_icon.xml").writeText(TestIconFile)
        }

        // Write a new icon to only one theme
        iconDirectory.listFiles()!![0].resolve("unique_test_icon").writeText(TestIconFile)

        val expectedApiFile = temporaryFolder.newFile("expected-api.txt").apply {
            writeText(ExpectedIconFile)
        }

        val generatedApiFile = temporaryFolder.newFile("generated-api.txt")

        val processor = IconProcessor(
            iconDirectories = iconDirectory.listFiles()!!.toList(),
            expectedApiFile = expectedApiFile,
            generatedApiFile = generatedApiFile
        )

        // Not all icons exist in all themes, so we should throw here
        assertIllegalStateContainingMessage("Not all icons were found") {
            processor.process()
        }
    }

    /**
     * Tests that an exception is thrown, failing the build, if there are multiple icons that will
     * have the same name on case insensitive filesystems
     */
    @Test
    fun iconProcessor_duplicateIconNames() {
        val iconDirectory = temporaryFolder.createIconDirectory()

        iconDirectory.listFiles()!!.forEach { themeDirectory ->
            themeDirectory.resolve("testicon.xml").writeText(TestIconFile)
            themeDirectory.resolve("test_icon.xml").writeText(TestIconFile)
        }

        val processor = IconProcessor(
            iconDirectories = iconDirectory.listFiles()!!.toList(),
            // Should crash before reaching this point, so just use an empty file
            expectedApiFile = temporaryFolder.root,
            generatedApiFile = temporaryFolder.root
        )

        // Duplicate icon names, so we should throw here
        assertIllegalStateContainingMessage(
            "Found multiple icons with the same case-insensitive filename"
        ) {
            processor.process()
        }
    }
}

/**
 * Asserts that [body] throws an [IllegalStateException], whose message contains [message].
 */
private fun assertIllegalStateContainingMessage(message: String, body: () -> Unit) {
    try {
        body()
        fail("No exception was thrown")
    } catch (e: IllegalStateException) {
        Truth.assertThat(e)
            .hasMessageThat()
            .contains(message)
    }
}

/**
 * Creates a temporary folder that contains subfolders for each [IconTheme], matching the
 * expected structure.
 */
private fun TemporaryFolder.createIconDirectory(): File {
    val iconDirectory = newFolder("icons")

    IconTheme.values().forEach { theme ->
        val folderName = theme.themePackageName
        iconDirectory.resolve(folderName).mkdir()
    }

    return iconDirectory
}

private val TestIconFile = """
    <vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:width="24dp"
        android:height="24dp"
        android:viewportWidth="24"
        android:viewportHeight="24"
        android:tint="?attr/colorControlNormal">
      <path
          android:fillColor="@android:color/white"
          android:pathData="M16.5,9h3.5v9h-3.5z"/>
    </vector>

""".trimIndent()

private val ExpectedIconFile = """
    <vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:width="24dp"
        android:height="24dp"
        android:viewportWidth="24"
        android:viewportHeight="24">
      <path
          android:fillColor="@android:color/black"
          android:pathData="M16.5,9h3.5v9h-3.5z"/>
    </vector>

""".trimIndent()

private val ExpectedApiFile = """
    Filled.TestIcon
    Outlined.TestIcon
    Rounded.TestIcon
    Sharp.TestIcon
    TwoTone.TestIcon
""".trimIndent()
