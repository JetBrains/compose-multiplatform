/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.*

// Render on the JVM, then hydrate the same markup in the browser.
@Composable
fun UserView(user: User) {
    var following by remember(user.id) { mutableStateOf(false) }
    Article(attrs = { classes(PageStyles.profile); attr("data-user-id", user.id.toString()) }) {
        Div(attrs = { classes(PageStyles.avatar); attr("aria-hidden", "true") }) {
            Text(user.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""))
        }
        Div(attrs = { classes(PageStyles.profileBody) }) {
            P(attrs = { classes(PageStyles.eyebrow) }) { Text("USER ${user.id.toString().padStart(3, '0')} / PUBLIC PROFILE") }
            H2 { Text(user.name) }
            P(attrs = { classes(PageStyles.role) }) { Text("${user.role} · ${user.location}") }
            P { Text(user.bio) }
            Button(attrs = {
                classes(PageStyles.button)
                type(ButtonType.Button)
                attr("aria-pressed", following.toString())
                onClick { following = !following }
            }) { Text(if (following) "Following ✓" else "Follow ${user.name.substringBefore(' ')} +") }
            Small(attrs = { classes(PageStyles.hint) }) {
                Text("The follow button changes local browser state after hydration.")
            }
        }
    }
}

@Composable
fun UserSkeleton() {
    Div(attrs = { classes(PageStyles.profile); attr("role", "status"); attr("data-user-loading", "true") }) {
        Div(attrs = { classes(PageStyles.avatar, PageStyles.skeleton); attr("aria-hidden", "true") }) {}
        Div(attrs = { classes(PageStyles.profileBody) }) {
            P(attrs = { classes(PageStyles.eyebrow) }) { Text("LOOKING UP USER / ON THE SERVER") }
            Div(attrs = { classes(PageStyles.skeletonLine); attr("aria-hidden", "true") }) {}
            Div(attrs = { classes(PageStyles.skeletonLine); attr("aria-hidden", "true") }) {}
            P { Text("The page is here. The profile will arrive in about a second.") }
        }
    }
}

@Composable
fun UserLoadError(message: String) {
    Div(attrs = { classes(PageStyles.profile); attr("role", "alert") }) {
        Div {
            P(attrs = { classes(PageStyles.eyebrow) }) { Text("LOOKUP FINISHED / USER UNAVAILABLE") }
            H2 { Text("This profile couldn't be loaded.") }
            P { Text(message) }
            A(href = "/?id=42", attrs = { classes(PageStyles.button) }) { Text("Try the sample user ↗") }
        }
    }
}
