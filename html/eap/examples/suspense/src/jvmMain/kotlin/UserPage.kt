/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.web.HydrationRoot
import org.jetbrains.compose.web.Suspense
import org.jetbrains.compose.web.SuspensePending
import org.jetbrains.compose.web.dom.*

sealed interface LoadState {
    data object Loading : LoadState
    data class Success(val user: User) : LoadState
    data class Failure(val cause: Exception) : LoadState
}

@Composable
fun UserPage(id: Long, load: suspend (Long) -> User = ::loadUser) {
    Main(attrs = { classes(PageStyles.page) }) {
        PageHeader()
        UserDemo(id, load)
        PageExplanation()
        CodeSample("The server owns the async work", serverExample)
        CodeSample("The browser receives the successful state", clientExample)
        PageFooter()
    }
}

// JVM only. The loader can call a database or private service.
@Composable
fun UserDetails(id: Long, load: suspend (Long) -> User = ::loadUser) {
    val state by produceState<LoadState>(LoadState.Loading, id, load) {
        value = try {
            LoadState.Success(load(id))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            LoadState.Failure(failure)
        }
    }
    when (val result = state) {
        LoadState.Loading -> SuspensePending()
        is LoadState.Success -> HydrationRoot(result.user, ::serializeUser, hydrationId = UserHydrationId) {
            UserView(it)
        }
        // Send a public error message. Keep the exception and stack trace on the server.
        is LoadState.Failure -> UserLoadError("No user was found for this ID. Choose the sample user and try again.")
    }
}

@Composable
private fun PageHeader() {
    Header(attrs = { classes(PageStyles.masthead) }) {
        Span { Text("COMPOSE HTML · SUSPENSE") }
    }
    Section(attrs = { classes(PageStyles.hero) }) {
        H1 {
            Text("The page arrives. ")
            Span(attrs = { classes(PageStyles.accent) }) { Text("The user follows.") }
        }
        P { Text("The server sends the page immediately, looks up a user, then streams the profile. The browser brings its button to life.") }
    }
}

@Composable
private fun UserDemo(id: Long, load: suspend (Long) -> User) {
    Section(attrs = { classes(PageStyles.demo) }) {
        Div(attrs = { classes(PageStyles.demoHeader) }) {
            Span { Text("THE LIVE EXAMPLE") }
            A(href = "/?id=$id") { Text("Replay lookup ↻") }
        }
        Suspense(fallback = { UserSkeleton() }) {
            UserDetails(id, load)
        }
        Div(attrs = { classes(PageStyles.demoFooter) }) {
            Span { Text("MOCK QUERY · 1 SECOND · JVM") }
            A(href = if (id > 0) "/?id=0" else "/?id=42") {
                Text(if (id > 0) "Try a failed lookup ↗" else "Try a successful lookup ↗")
            }
        }
    }
}

@Composable
private fun PageExplanation() {
    Section(attrs = { classes(PageStyles.explanation) }) {
        P(attrs = { classes(PageStyles.eyebrow) }) { Text("WHERE THE CODE RUNS") }
        H2 { Text("Shared UI. Separate responsibilities.") }
        Div(attrs = { classes(PageStyles.grid) }) {
            Responsibility(
                label = "01 / SERVER",
                sourceSet = "jvmMain",
                functions = "UserPage, UserDetails, loadUser",
                explanation = "produceState runs the suspend lookup. Suspense streams the skeleton, then the profile or an error. The loader stays on the JVM.",
            )
            Responsibility(
                label = "02 / SHARED",
                sourceSet = "commonMain",
                functions = "User, UserView, public JSON",
                explanation = "The same UserView renders on the server and hydrates in the browser. The public user DTO travels alongside the HTML in an inert script element.",
            )
            Responsibility(
                label = "03 / BROWSER",
                sourceSet = "webMain",
                functions = "hydrateRoot + click handlers",
                explanation = "After the response finishes, hydrateRoot reads the transferred user and adopts the existing profile. Clicking Follow updates browser state without another lookup.",
            )
        }
    }
}

@Composable
private fun PageFooter() {
    Footer(attrs = { classes(PageStyles.footer) }) {
        Text("Header and footer arrive with the skeleton. Only the profile becomes interactive.")
    }
}

@Composable
private fun Responsibility(label: String, sourceSet: String, functions: String, explanation: String) {
    Article(attrs = { classes(PageStyles.card) }) {
        P(attrs = { classes(PageStyles.eyebrow) }) { Text(label) }
        H3 { Text(sourceSet) }
        Code { Text(functions) }
        P { Text(explanation) }
    }
}

@Composable
private fun CodeSample(title: String, sample: String) {
    Section(attrs = { classes(PageStyles.codeSection) }) {
        H2 { Text(title) }
        Pre { Code { Text(sample) } }
    }
}

private val serverExample = """
    // jvmMain: effects run in the request's scope.
    @Composable
    fun UserDetails(id: Long) {
        val state by produceState<LoadState>(LoadState.Loading, id) {
            value = try {
                LoadState.Success(loadUser(id)) // Suspends for the mock lookup.
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                LoadState.Failure(failure)
            }
        }
        when (val result = state) {
            LoadState.Loading -> SuspensePending()
            is LoadState.Success -> HydrationRoot(
                result.user, ::serializeUser, hydrationId = "user-details"
            ) { UserView(it) }
            is LoadState.Failure -> UserLoadError("User unavailable")
        }
    }

    // In UserDemo, between the header and footer:
    Suspense(fallback = { UserSkeleton() }) { UserDetails(id) }
""".trimIndent()

private val clientExample = """
    // webMain: run after the streamed response is parsed.
    hydrateRoot(::deserializeUser, hydrationId = "user-details") { user ->
        UserView(user)
    }

    // commonMain: shared by server and browser.
    @Composable
    fun UserView(user: User) {
        var following by remember(user.id) { mutableStateOf(false) }
        // Profile markup and Button(onClick = { following = !following })
    }

    // HydrationRoot emits the public JSON beside the profile:
    // <script type="text/plain" data-compose-hydration-for="user-details" ...>
    //   {"id":"42","name":"Maya Chen", ...}
    // </script>
""".trimIndent()
