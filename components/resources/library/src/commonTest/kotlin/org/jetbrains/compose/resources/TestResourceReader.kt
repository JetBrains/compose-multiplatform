package org.jetbrains.compose.resources

internal class TestResourceReader : ResourceReader {
    private val readPathsList = mutableListOf<String>()
    private val missingPathsList = mutableListOf<String>()
    val readPaths: List<String> get() = readPathsList
    val missingPaths: List<String> get() = missingPathsList

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun read(path: String, defaultPath: String?): ByteArray {
        readPathsList.add(path)
        return try {
            DefaultResourceReader.read(path, defaultPath)
        } catch (e: MissingResourceException) {
            missingPathsList.add(path)
            DefaultResourceReader.read("1.png", defaultPath)
        }
    }
}