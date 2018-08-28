package com.google.r4a.examples.explorerapp.common.adapters

import android.os.Bundle
import android.os.PersistableBundle
import android.support.annotation.IdRes
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.createGraph
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorDestinationBuilder
import com.google.r4a.CompositionContext
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.R

/**
 * An activity class tht contains some of our app-level configuration. Meant to be used as a "single app activity".
 */
abstract class ComposeNavigationActivity: AppCompatActivity() {
    abstract fun composeContent(content: (ViewGroup.LayoutParams) -> Unit)
    abstract val startId: Int
    abstract fun NavGraphBuilder.buildNavGraph(navigator: FragmentNavigator)

    private lateinit var root: LinearLayout

    lateinit var controller: NavController
    lateinit var navigator: FragmentNavigator

    @Suppress("PLUGIN_WARNING")
    private val rootCompose = {
        with(CompositionContext.current) {
            provideAmbient(Ambients.Activity, this@ComposeNavigationActivity) {
                provideAmbient(Ambients.Context, this@ComposeNavigationActivity) {
                    provideAmbient(Ambients.FragmentManager, supportFragmentManager) {
                        provideAmbient(Ambients.Application, application) {
                            provideAmbient(Ambients.NavController, controller) {
                                provideAmbient(Ambients.Configuration, this@ComposeNavigationActivity.resources.configuration) {
                                    group(0) {
                                        composeContent { params ->
                                            emitComponent(0, ::FragmentComponent) {
                                                set({ MyNavHostFragment().apply { controller = this@ComposeNavigationActivity.controller } }) { construct = it }
                                                set(params) { layoutParams = it }
                                                set(R.id.container) { id = it }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
//            id = R.id.container
        }

        controller = NavController(this)
        // TODO(lmr): we are using FragmentNavigator right now. I think we could/should make a ComponentNavigator
        navigator = FragmentNavigator(this, supportFragmentManager, R.id.container)

        // should id be 0 or R.id.nav_graph?
        controller.graph = controller.createGraph(0, startId) {
            buildNavGraph(navigator)
        }

        root.composeInto(rootCompose)

//        val nhf = NavHostFragment()

        setContentView(root)
    }

    inline fun <reified T: Fragment> NavGraphBuilder.destination(@IdRes id: Int) {
        addDestination(FragmentNavigatorDestinationBuilder(this@ComposeNavigationActivity.navigator, id, T::class).build())
    }


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
    }

}