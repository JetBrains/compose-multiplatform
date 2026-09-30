import org.jetbrains.compose.web.hydrateRoot

fun main() {
    hydrateRoot(String::toInt, hydrationId = "island-weight") { WeightIsland(it) }
    hydrateRoot(String::toInt, hydrationId = "island-size") { SizeIsland(it) }
    hydrateRoot({ it }, hydrationId = "island-text") { TextIsland(it) }
}
