package org.jetbrains.compose.resources

import java.io.File

private object AndroidResourceReader

@OptIn(ExperimentalResourceApi::class)
@InternalResourceApi
actual suspend fun readResourceBytes(path: String): ByteArray {
    val classLoader = Thread.currentThread().contextClassLoader ?: AndroidResourceReader.javaClass.classLoader
    val resource = classLoader.getResourceAsStream(path) ?: run {
        //try to find a font in the android assets
        if (File(path).parentFile?.name.orEmpty().startsWith("font")) {
            classLoader.getResourceAsStream("assets/$path")
        } else null
    } ?: throw MissingResourceException(path)
    return resource.readBytes()
}