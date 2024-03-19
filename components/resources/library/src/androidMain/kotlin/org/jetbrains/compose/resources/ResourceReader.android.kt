package org.jetbrains.compose.resources

import java.io.File

private object AndroidResourceReader

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun readResourceBytes(path: String): ByteArray {
    val classLoader = getClassLoader()
    val resource = classLoader.getResourceAsStream(path) ?: run {
        //try to find a font in the android assets
        if (File(path).parentFile?.name.orEmpty() == "font") {
            classLoader.getResourceAsStream("assets/$path")
        } else null
    } ?: throw MissingResourceException(path)
    return resource.readBytes()
}

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun convertPathToUri(path: String): String {
    val resource = getClassLoader()?.getResource(path) ?: throw MissingResourceException(path)
    return resource.toURI().toString()
}

private fun getClassLoader() =
    Thread.currentThread().contextClassLoader ?: AndroidResourceReader.javaClass.classLoader