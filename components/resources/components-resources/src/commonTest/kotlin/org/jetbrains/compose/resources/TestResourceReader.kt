package org.jetbrains.compose.resources

internal class TestResourceReader : ResourceReader {
    private val readPathsList = mutableListOf<String>()
    val readPaths: List<String> get() = readPathsList

    override suspend fun read(path: String): ByteArray {
        readPathsList.add(path)
        return DefaultResourceReader.read(path)
    }
}