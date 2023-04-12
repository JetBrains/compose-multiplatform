package example.imageviewer.model


//@Serializable
data class Picture(val big: String, val small: String)

fun getNameURL(url: String): String = url.substring(url.lastIndexOf('/') + 1, url.length)
val Picture.name: String get() {
    val realName = getNameURL(big)
    return mockNames.getOrElse(realName) { realName }
}
val Picture.bigUrl get() = "$BASE_URL/$big"
val Picture.smallUrl get() = "$BASE_URL/$small"

val mockNames = mapOf(
    "1.jpg" to "Gondolas",
    "2.jpg" to "Winter Pier",
    "3.jpg" to "Kitties outside",
    "4.jpg" to "Heap of trees",
    "5.jpg" to "Resilient Cacti",
    "6.jpg" to "Swirls",
    "7.jpg" to "Gradient Descent",
    "8.jpg" to "Sleepy in Seattle",
    "9.jpg" to "Lightful infrastructure",
    "10.jpg" to "Compose Pathway",
    "11.jpg" to "Rotary",
    "12.jpg" to "Towering",
    "13.jpg" to "Vasa"
)