import org.jetbrains.compose.web.hydrateRoot

fun main() {
    hydrateRoot(String::toInt) { initialCount ->
        HydrationDemo(initialCount)
    }
}
