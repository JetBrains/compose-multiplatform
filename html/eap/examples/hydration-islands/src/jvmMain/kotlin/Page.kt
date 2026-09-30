import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.HydrationRoot
import org.jetbrains.compose.web.dom.*

private val hydrationExample = """
    // JVM: render the islands as part of the page.
    Html {
        Head { ... }
        Body {
            ...

            HydrationRoot(640, Int::toString, hydrationId = "island-weight") {
                WeightIsland(it)
            }

            HydrationRoot(80, Int::toString, hydrationId = "island-size") {
                SizeIsland(it)
            }

            HydrationRoot("Type something worth setting.", { it }, hydrationId = "island-text") {
                TextIsland(it)
            }
        }
    }

    // Browser: only needs to hydrate islands
    hydrateRoot(String::toInt, hydrationId = "island-weight") {
        WeightIsland(it)
    }
    hydrateRoot(String::toInt, hydrationId = "island-size") {
        SizeIsland(it)
    }
    hydrateRoot({ it }, hydrationId = "island-text") {
        TextIsland(it)
    }
""".trimIndent()

@Composable
fun HydrationIslandsPage() {
    Main(attrs = { classes(PageStyles.page) }) {
        Div(attrs = { classes(PageStyles.masthead) }) {
            Div(attrs = { classes(PageStyles.brand) }) {
                Span { Text("HYDRATION ISLANDS") }
            }
            Span { Text("COMPOSE HTML") }
        }

        Header(attrs = { classes(PageStyles.hero) }) {
            P(attrs = { classes(PageStyles.eyebrow) }) { Text("SELECTIVE INTERACTION") }
            H1 {
                Text("Most of this page")
                Br()
                Em { Text("never wakes up.") }
            }
            P(attrs = { classes(PageStyles.lead) }) {
                Text("Render the whole experience on the server.")
                Br()
                Text("Hydrate only the parts that need to respond.")
            }
            Div(attrs = { classes(PageStyles.heroFoot) }) {
                Span { Text("SCROLL TO EXPLORE") }
                Span { Text("↓") }
            }
        }

        Article(attrs = { classes(PageStyles.paper) }) {
            Header(attrs = { classes(PageStyles.paperHeader) }) {
                Span { Text("A TYPE PLAYGROUND") }
                Span { Text("STATIC HTML / THREE ISLANDS") }
            }
            Div(attrs = { classes(PageStyles.islandGrid) }) {
                Section(attrs = { classes(PageStyles.islandSection) }) {
                    HydrationRoot(640, Int::toString, hydrationId = "island-weight") {
                        WeightIsland(it)
                    }
                }

                Section(attrs = { classes(PageStyles.islandSection) }) {
                    HydrationRoot(80, Int::toString, hydrationId = "island-size") {
                        SizeIsland(it)
                    }
                }
            }

            Section(attrs = { classes(PageStyles.islandSection) }) {
                IslandHeading("03", "Your words")
                HydrationRoot("Type something worth setting.", { it }, hydrationId = "island-text") {
                    TextIsland(it)
                }
            }

            Section(attrs = { classes(PageStyles.codeSection) }) {
                P(attrs = { classes(PageStyles.eyebrow) }) { Text("HOW THE ISLANDS WORK") }
                H2 { Text("One ID. One island.") }
                P { Text("The server renders each island. The browser finds its ID and hydrates just that region.") }
                Pre { Code { Text(hydrationExample) } }
            }

            Footer(attrs = { classes(PageStyles.paperFooter) }) {
                Span { Text("COMPOSE HTML / HYDRATION ISLANDS") }
            }
        }
    }
}
