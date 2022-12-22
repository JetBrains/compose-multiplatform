package example.imageviewer.model

import kotlinx.serialization.Serializable

@Serializable
data class Picture(val big: String, val small: String)

fun getNameURL(url: String): String = url.substring(url.lastIndexOf('/') + 1, url.length)
val Picture.name get() = getNameURL(big)
val Picture.bigUrl get() = "$BASE_URL/$big"
val Picture.smallUrl get() = "$BASE_URL/$small"
