package org.jetbrains.compose.test.tests.unit

import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import org.jetbrains.compose.resources.ResourceHolder
import org.jetbrains.compose.resources.ResourceType
import org.jetbrains.compose.test.tests.integration.ResourcesTest
import org.jetbrains.compose.test.utils.assertThrowsWithMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Tests for [ResourceHolder] validation logic.
 * Integration tests are found in [ResourcesTest].
 */
class ResourceHolderValidationTest {

    @TempDir
    lateinit var tempDir: File

    private fun root(): File = tempDir.resolve("composeResources").apply { mkdirs() }

    @Test
    fun unsupportedResourceType() {
        val resourceFolder = root().createDir("foo")
        assertThrowsWithMessage<GradleException>("Unknown resource type: 'foo'.") {
            ResourceHolder(resourceFolder)
        }
    }

    @Test
    fun testOverlappingResourceNames() {
        val resourceFolder = root()
            .createDir("drawable") {
                createFile("compose.png")
                createFile("compose.jpg")
            }
        assertThrowsWithMessage<GradleException>("Resource file conflicts with another resource file or directory") {
            ResourceHolder(resourceFolder)
        }
    }

    @Test
    fun testWhitespaceInFileName() {
        val resourceFolder = root()
            .createDir("drawable") {
                createFile("com pose.png")
            }
        assertThrowsWithMessage<GradleException>("Whitespace not allowed in resource file names: drawable/com pose.png") {
            ResourceHolder(resourceFolder)
        }
    }

    @Test
    fun testDotInFileName() {
        val resourceFolder = root()
            .createDir("drawable") {
                createFile("com.pose.png")
            }
        assertThrowsWithMessage<GradleException>("`.` not allowed in resource file names: drawable/com.pose.png") {
            ResourceHolder(resourceFolder)
        }
    }

    // When collapsing resource names, normalized (lowercase name) is used.
    @Test
    fun testResourceNamesIgnoreCasingWhenMatchingResourceNames() {
        val resourceFolder = root()
            .createDir("drawable") {
                createFile("compose.png")
            }
            .createDir("drawable-dark") {
                createFile("COMPOSE.JPG")
            }
            .createDir("drawable-en") {
                createFile("Compose.gif")
            }
        val drawables = ResourceHolder(resourceFolder).resources[ResourceType.DRAWABLE]!!
        assertEquals(1, drawables.size)
        assertTrue(drawables.containsKey("compose"))
        assertEquals(3, drawables["compose"]?.size)
    }

    // When generating accessors, casing from non-qualified dir is used
    @Test
    fun testResourceNameUseNonQualifiedCasing() {
        val resourceFolder = root()
            .createDir("drawable") {
                createFile("COMPOSE.png")
            }
            .createDir("drawable-dark") {
                createFile("compose.JPG")
            }
        val drawables = ResourceHolder(resourceFolder).resources[ResourceType.DRAWABLE]!!
        assertEquals(1, drawables.size)
        assertTrue(drawables.containsKey("COMPOSE"))
        assertEquals(2, drawables["COMPOSE"]?.size)
    }

    @Test
    fun testSubdirsThrowIfNotEnabled() {
        val resourceFolder = root()
            .createDir("drawable") {
                createDir("subdir")
            }
        assertThrowsWithMessage<GradleException>("Resource subdirectory is not allowed: drawable/subdir") {
            ResourceHolder(resourceFolder, allowAccessorSubDirs = false)
        }
    }


    @Test
    fun testWhitespaceInSubdirNotAllowed() {
        val resourceFolder = root()
            .createDir("drawable") {
                createDir("sub dir")
            }
        assertThrowsWithMessage<GradleException>("Whitespace not allowed in resource directory names: drawable/sub dir") {
            ResourceHolder(resourceFolder)
        }
    }

    @Test
    fun testDotInSubdirNotAllowed() {
        val resourceFolder = root()
            .createDir("drawable") {
                createDir("sub.dir")
            }
        assertThrowsWithMessage<GradleException>("`.` not allowed in resource directory names: drawable/sub.dir") {
            ResourceHolder(resourceFolder)
        }
    }

    @Test
    fun testDigitInSubdirNotAllowed() {
        val resourceFolder = root()
            .createDir("drawable") {
                createDir("1subdir")
            }
        assertThrowsWithMessage<GradleException>("Resource directory names cannot start with a digit: drawable/1subdir") {
            ResourceHolder(resourceFolder)
        }
    }

    @Test
    fun testSubDirAccessorNamesSeparatedByDot() {
        val resourceFolder = root()
            .createDir("drawable") {
                createDir("subdir") {
                    createDir("subdir2") {
                        createFile("compose.png")
                    }
                }
            }

        val drawables = ResourceHolder(resourceFolder).resources[ResourceType.DRAWABLE]!!
        assertEquals("subdir.subdir2.compose", drawables.keys.first())
    }

    @Test
    fun testSubDirCasingRetained() {
        val resourceFolder = root()
            .createDir("drawable") {
                createDir("suBdir") {
                    createDir("subDir2") {
                        createFile("compose.png")
                    }
                }
            }

        val drawables = ResourceHolder(resourceFolder).resources[ResourceType.DRAWABLE]!!
        assertEquals("suBdir.subDir2.compose", drawables.keys.first())
    }

    @Test
    fun testSubDirCollapsingIgnoreCasing() {
        val resourceFolder = root()
            .createDir("drawable") {
                createDir("subdir") {
                    createFile("compose.png")
                }
            }
            .createDir("drawable-en") {
                createDir("SUBDIR") {
                    createFile("compose.png")
                }
            }

        val drawables = ResourceHolder(resourceFolder).resources[ResourceType.DRAWABLE]!!
        assertEquals("subdir.compose", drawables.keys.single())
        assertEquals(2, drawables["subdir.compose"]?.size)
    }

    @Test
    fun testSubDirCollapsingUseNonQualifiedName() {
        val resourceFolder = root()
            .createDir("drawable") {
                createDir("SUBDIR") {
                    createFile("compose.png")
                }
            }
            .createDir("drawable-en") {
                createDir("subdir") {
                    createFile("compose.png")
                }
            }

        val drawables = ResourceHolder(resourceFolder).resources[ResourceType.DRAWABLE]!!
        assertEquals("SUBDIR.compose", drawables.keys.single())
        assertEquals(2, drawables["SUBDIR.compose"]?.size)
    }

    @Test
    fun testDuplicateStringKeys() {
        val resourceFolder = root()
            .createDir("values") {
                createFile("strings.commonMain.cvr").writeText("""
                    version:0
                    string|app_name|Q29tcG9zZSBSZXNvdXJjZXMgQXBw
                """.trimIndent())
                createFile("strings2.commonMain.cvr").writeText("""
                    version:0
                    string|app_name|Q29tcG9zZSBSZXNvdXJjZXMgQXBwIDIK
                """.trimIndent())
            }

        assertThrowsWithMessage<GradleException>("Duplicate resource key 'app_name' found in directory: values") {
            ResourceHolder(resourceFolder)
        }
    }


    private fun File.createDir(name: String, dirAction: File.() -> Unit = {}): File {
        this.resolve(name).apply {
            mkdir()
            dirAction(this)
        }
        return this
    }

    private fun File.createFile(name: String): File {
        val file = this.resolve(name)
        file.createNewFile()
        return file
    }
}
