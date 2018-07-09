package com.google.r4a.examples.explorerapp

import android.support.v4.widget.DrawerLayout
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.NavGraphBuilder
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorDestinationBuilder
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.common.api.RedditApi
import com.google.r4a.examples.explorerapp.common.data.*
import com.google.r4a.examples.explorerapp.ui.components.RedditDrawer
import java.util.concurrent.Executors

/**
 * The single activity for the full app
 */
class MainActivity : ComposeNavigationActivity() {

    /**
     * This is the thread pool that network requests will be issued on.
     */
    @Suppress("PrivatePropertyName")
    private val NETWORK_IO = Executors.newFixedThreadPool(5)

    private val api by lazy {
        RedditApi.create()
    }

    private val repository: RedditRepository by lazy {
        RedditRepositoryImpl(api, NETWORK_IO)
    }

    private val authService: AuthenticationService by lazy {
        object : AuthenticationService {
            override var currentUser: RedditUser? = null
            override var isLoggedIn: Boolean = false

            override fun reauthenticate() {

            }

            override fun login(username: String, password: String, callback: Callback<RedditUser>) {

            }

            override fun signup(username: String, password: String, callback: Callback<RedditUser>) {

            }
        }
    }

    /**
     * The id of the default start destination for the app.
     */
    override val startId: Int = R.id.screen_signup

    /**
     * Here we want to programmatically build our navigation graph for the app. We add destinations and actions.
     *
     * TODO(lmr): We should work on building a ComponentNavigator instead of FragmentNavigator so that we can even
     * avoid fragments
     */
    override fun NavGraphBuilder.buildNavGraph(navigator: FragmentNavigator) {
        destination<LinkListFragment>(R.id.screen_link_list)
        destination<LinkDetailFragment>(R.id.screen_link_detail)
        destination<ExamplesFragment>(R.id.screen_examples)
        destination<ExampleFragment>(R.id.screen_example)
        destination<LoginFragment>(R.id.screen_login)
        destination<SettingsFragment>(R.id.screen_settings)
        destination<SignupFragment>(R.id.screen_signup)

        action(R.id.nav_to_home) { destinationId = R.id.screen_link_list }
        action(R.id.nav_to_detail) { destinationId = R.id.screen_link_detail }
        action(R.id.nav_to_examples) { destinationId = R.id.screen_examples }
        action(R.id.nav_to_example) { destinationId = R.id.screen_example }
        action(R.id.nav_to_login) { destinationId = R.id.screen_login }
        action(R.id.nav_to_settings) { destinationId = R.id.screen_settings }
        action(R.id.nav_to_signup) { destinationId = R.id.screen_signup }

        action(R.id.nav_list_to_detail) {
            destinationId = R.id.screen_link_detail
            navOptions {
                anim {
                    // TODO(lmr): I'd like to eventually work on configuring shared element transitions here
                    enter = R.anim.nav_default_enter_anim
                    exit = R.anim.nav_default_exit_anim
                }
            }
        }
    }

    private val contentParams = DrawerLayout.LayoutParams(
            DrawerLayout.LayoutParams.MATCH_PARENT,
            DrawerLayout.LayoutParams.MATCH_PARENT
    )

    /**
     * Here we are given a "content" lambda that we need to compose inside of this function. This gives us an
     * opportunity to provide any ambients that we want to to the rest of the app. <content /> will end up being
     * whatever the current fragment / screen is.
     *
     * TODO(lmr): It might be a better abstraction if we consolidate this and composeDrawer
     */
    override fun composeContent(content: (ViewGroup.LayoutParams) -> Unit) {
        with(CompositionContext.current) {
            provideAmbient(RedditRepository.Ambient, repository) {
                provideAmbient(AuthenticationService.Ambient, authService) {
                    emitComponent(0, ::RedditDrawer) {
                        val fn = {
                            group(0) {
                                content(contentParams)
                            }
                        }
                        set(fn) { setChildrenBlock(it) }
                    }
                }
            }
        }
    }
}