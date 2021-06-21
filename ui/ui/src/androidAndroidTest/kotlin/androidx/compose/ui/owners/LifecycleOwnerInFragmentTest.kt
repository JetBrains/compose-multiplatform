/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.owners

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class LifecycleOwnerInFragment {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<FragmentActivity>(
        FragmentActivity::class.java
    )
    private lateinit var activity: FragmentActivity

    @Before
    fun setup() {
        activity = activityTestRule.activity
    }

    @Test
    fun lifecycleOwnerIsAvailable() {
        val fragment = TestFragment()

        activityTestRule.runOnUiThread {
            val view = FragmentContainerView(activity)
            view.id = 100
            activity.setContentView(view)
            activity.supportFragmentManager.beginTransaction()
                .replace(100, fragment)
                .commit()
        }

        assertTrue(fragment.latch.await(1, TimeUnit.SECONDS))
        assertEquals(fragment.viewLifecycleOwner, fragment.owner)
    }

    @Test
    fun lifecycleOwnerReplaced() {
        val fragment = TestFragmentFrameLayout()

        activityTestRule.runOnUiThread {
            val view = FragmentContainerView(activity)
            view.id = 100
            activity.setContentView(view)
            activity.supportFragmentManager.beginTransaction()
                .replace(100, fragment)
                .commit()
        }

        assertTrue(fragment.latch.await(1, TimeUnit.SECONDS))

        val frameLayout = fragment.frameLayout!!
        var latch = CountDownLatch(1)
        var owner: LifecycleOwner? = null

        activityTestRule.runOnUiThread {
            frameLayout.addView(
                ComposeView(frameLayout.context).apply {
                    setContent {
                        owner = LocalLifecycleOwner.current
                        latch.countDown()
                    }
                }
            )
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(fragment.viewLifecycleOwner, owner)

        val composeView = frameLayout.getChildAt(0)
        val fragment2 = TestFragmentFrameLayout()

        activityTestRule.runOnUiThread {
            frameLayout.removeView(composeView)
            owner = null
            activity.supportFragmentManager.beginTransaction()
                .replace(100, fragment2)
                .commit()
        }
        assertTrue(fragment2.latch.await(1, TimeUnit.SECONDS))

        activityTestRule.runOnUiThread {
            val frameLayout2 = fragment2.frameLayout!!
            latch = CountDownLatch(1)
            frameLayout2.addView(composeView)
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(fragment2.viewLifecycleOwner, owner)
    }
}

class TestFragment : Fragment() {

    var owner: LifecycleOwner? = null
    val latch = CountDownLatch(1)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            owner = LocalLifecycleOwner.current
            latch.countDown()
        }
    }
}

class TestFragmentFrameLayout : Fragment() {

    val latch = CountDownLatch(1)
    var frameLayout: FrameLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = FrameLayout(requireContext())
        frameLayout = view
        latch.countDown()
        return view
    }
}
