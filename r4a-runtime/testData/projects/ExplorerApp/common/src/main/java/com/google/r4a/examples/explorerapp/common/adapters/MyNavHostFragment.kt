/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.r4a.examples.explorerapp.common.adapters

import android.content.Context
import android.os.Bundle
import android.support.annotation.NavigationRes
import android.support.v4.app.Fragment
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavHost
import androidx.navigation.Navigation
import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.R
import com.google.r4a.Ambient

/**
 * NOTE(lmr): This is a stripped down version of NavHostFragment that essentially doesn't really do anything extra
 * except that it allows for the NavController to be passed to it instead of created privately. ComposeActivity ends
 * up needing this inversion. When we build ComposeNavigator we can get rid of this... but potentially we should add
 * the setter to
 */
class MyNavHostFragment : Fragment(), NavHost {

    var controller: NavController? = null
    var reference: Ambient.Reference? = null

    // State that will be saved and restored
    private var mDefaultNavHost: Boolean = false

    /**
     * Returns the [navigation controller][NavController] for this navigation host.
     * This method will return null until this host fragment's [.onCreate]
     * has been called and it has had an opportunity to restore from a previous instance state.
     *
     * @return this host's navigation controller
     * @throws IllegalStateException if called before [.onCreate]
     */
    override fun getNavController(): NavController = controller!!

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        // TODO This feature should probably be a first-class feature of the Fragment system,
        // but it can stay here until we can add the necessary attr resources to
        // the fragment lib.
        if (mDefaultNavHost) {
            requireFragmentManager().beginTransaction()
                    .setPrimaryNavigationFragment(this)
                    .commit()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = requireContext()

        controller = NavController(context)
        controller!!.navigatorProvider.addNavigator(createFragmentNavigator())

        var navState: Bundle? = null
        if (savedInstanceState != null) {
            navState = savedInstanceState.getBundle(KEY_NAV_CONTROLLER_STATE)
            if (savedInstanceState.getBoolean(KEY_DEFAULT_NAV_HOST, false)) {
                mDefaultNavHost = true
                requireFragmentManager().beginTransaction()
                        .setPrimaryNavigationFragment(this)
                        .commit()
            }
        }

        if (navState != null) {
            // Navigation controller state overrides arguments
            controller!!.restoreState(navState)
        } else {
            controller!!.setMetadataGraph()
        }
    }

    /**
     * Create the FragmentNavigator that this NavHostFragment will use. By default, this uses
     * [FragmentNavigator], which replaces the entire contents of the NavHostFragment.
     *
     *
     * This is only called once in [.onCreate] and should not be called directly by
     * subclasses.
     * @return a new instance of a FragmentNavigator
     */
    private fun createFragmentNavigator(): Navigator<out FragmentNavigator.Destination> {
        return FragmentNavigator(requireContext(), childFragmentManager, id)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val frameLayout = FrameLayout(inflater.context)
        // When added via XML, this has no effect (since this FrameLayout is given the ID
        // automatically), but this ensures that the View exists as part of this Fragment's View
        // hierarchy in cases where the NavHostFragment is added programmatically as is required
        // for child fragment transactions
        frameLayout.id = id
        return frameLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view !is ViewGroup) {
            throw IllegalStateException("created host view $view is not a ViewGroup")
        }
        // When added via XML, the parent is null and our view is the root of the NavHostFragment
        // but when added programmatically, we need to set the NavController on the parent - i.e.,
        // the View that has the ID matching this NavHostFragment.
        val rootView = if (view.getParent() != null) view.getParent() as View else view
        Navigation.setViewNavController(rootView, controller)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val navState = controller!!.saveState()
        if (navState != null) {
            outState.putBundle(KEY_NAV_CONTROLLER_STATE, navState)
        }
        if (mDefaultNavHost) {
            outState.putBoolean(KEY_DEFAULT_NAV_HOST, true)
        }
    }

    companion object {
        private val KEY_NAV_CONTROLLER_STATE = "android-support-nav:fragment:navControllerState"
        private val KEY_DEFAULT_NAV_HOST = "android-support-nav:fragment:defaultHost"
    }
}
