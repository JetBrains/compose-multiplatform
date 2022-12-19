package example.imageviewer.model

fun getNameURL(url: String): String = url.substring(url.lastIndexOf('/') + 1, url.length)

val Picture.name get() = getNameURL(big)
val Picture.width get():Int = 123//todo
val Picture.height get():Int = 123//todo
