/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.samples

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.viewbinding.samples.R
import androidx.compose.ui.viewbinding.samples.databinding.TestFragmentLayoutBinding
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentRecreateTest {

    @Test
    fun testRecreateFragment() {
        with(ActivityScenario.launch(InflatedFragmentActivity::class.java)) {
            val fragment = withActivity {
                supportFragmentManager.findFragmentById(R.id.fragment_container)!!
            }
            assertThat(fragment.requireView().parent).isNotNull()

            recreate()

            val recreatedFragment = withActivity {
                supportFragmentManager.findFragmentById(R.id.fragment_container)!!
            }
            assertThat(recreatedFragment.requireView().parent).isNotNull()
        }
    }
}

class InflatedFragmentActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidViewBinding(TestFragmentLayoutBinding::inflate)
        }
    }
}