// READ ME FIRST!
//
// Code in this file is shared between the Android and Desktop JVM targets.
// Kotlin's hierarchical multiplatform projects currently
// don't support sharing code depending on JVM declarations.
//
// You can follow the progress for HMPP JVM & Android intermediate source sets here:
// https://youtrack.jetbrains.com/issue/KT-42466
//
// The workaround used here to access JVM libraries causes IntelliJ IDEA to not
// resolve symbols in this file properly.
//
// Resolution errors in your IDE do not indicate a problem with your setup.

package example.imageviewer.model

import androidx.compose.ui.graphics.ImageBitmap

data class NetworkRequest(//todo simplify to String
    val url: String
)

fun getNameURL(url: String): String = url.substring(url.lastIndexOf('/') + 1, url.length)

val Picture.name get() = getNameURL(big)
val Picture.width get():Int = 123//todo
val Picture.height get():Int = 123//todo
