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

package androidx.compose.runtime.saveable

import android.os.Bundle
import android.os.Parcel
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.test.R
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@MediumTest
@RunWith(AndroidJUnit4::class)
class ActivityRecreationTest {

    @Test
    fun valueStoredViaRememberIsLost() {
        val activityScenario: ActivityScenario<RecreationTest1Activity> =
            ActivityScenario.launch(RecreationTest1Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            assertThat(it.array).isEqualTo(intArrayOf(0))
            // change the value
            it.array[0] = 1
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.array).isEqualTo(intArrayOf(0))
        }
    }

    @Test
    fun valueStoredViaRememberSavedInstanceStateRestored() {
        val activityScenario: ActivityScenario<RecreationTest2Activity> =
            ActivityScenario.launch(RecreationTest2Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            assertThat(it.array).isEqualTo(intArrayOf(0))
            // change the value, so we can assert this change will be restored
            it.array[0] = 1
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.array).isEqualTo(intArrayOf(1))
        }
    }

    @Test
    fun valueStoredInTwoViewsBothRestored() {
        val activityScenario: ActivityScenario<RecreationTest3Activity> =
            ActivityScenario.launch(RecreationTest3Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            assertThat(it.array1).isEqualTo(intArrayOf(0))
            assertThat(it.array2).isEqualTo(intArrayOf(0))
            // change the value, so we can assert this change will be restored
            it.array1[0] = 1
            it.array2[0] = 2
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.array1).isEqualTo(intArrayOf(1))
            assertThat(it.array2).isEqualTo(intArrayOf(2))
        }
    }

    @Test
    fun valuesStoredInTwoFragmentsRestored() {
        val activityScenario: ActivityScenario<RecreationTest4Activity> =
            ActivityScenario.launch(RecreationTest4Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            val array1 = it.findFragment(R.id.child1).array
            val array2 = it.findFragment(R.id.child2).array
            assertThat(array1).isEqualTo(intArrayOf(0))
            assertThat(array2).isEqualTo(intArrayOf(0))
            // change the value, so we can assert this change will be restored
            array1[0] = 1
            array2[0] = 2
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            val array1 = it.findFragment(R.id.child1).array
            val array2 = it.findFragment(R.id.child2).array
            assertThat(array1).isEqualTo(intArrayOf(1))
            assertThat(array2).isEqualTo(intArrayOf(2))
        }
    }

    @Test
    fun valueStoredInMutableStateIsRestored() {
        val activityScenario: ActivityScenario<RecreationTest5Activity> =
            ActivityScenario.launch(RecreationTest5Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            assertThat(it.state.value).isEqualTo(0)
            // change the value, so we can assert this change will be restored
            it.state.value = 1
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.state.value).isEqualTo(1)
        }
    }

    @Test
    fun valueStoredInMutableStateInsideSaveableStateHolderIsRestored() {
        val activityScenario: ActivityScenario<RecreationTest6Activity> =
            ActivityScenario.launch(RecreationTest6Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            assertThat(it.state.value).isEqualTo(0)
            // change the value, so we can assert this change will be restored
            it.state.value = 1
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.state.value).isEqualTo(1)
        }
    }

    @Test
    fun valueStoredInMutableStateWithCustomSaverInsideSaveableStateHolderIsRestored() {
        val activityScenario: ActivityScenario<RecreationTest7Activity> =
            ActivityScenario.launch(RecreationTest7Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            assertThat(it.state.value).isEqualTo(Count(0))
            // change the value, so we can assert this change will be restored
            it.state.value = Count(1)
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.state.value).isEqualTo(Count(1))
        }
    }

    @Test
    fun stateStoredViaListSaverOrMapSaverIsRestored() {
        val activityScenario: ActivityScenario<RecreationTest8Activity> =
            ActivityScenario.launch(RecreationTest8Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            assertThat(it.stateAsList.value).isEqualTo(0)
            assertThat(it.stateAsMap.value).isEqualTo(0)
            // change the value, so we can assert this change will be restored
            it.stateAsList.value = 1
            it.stateAsMap.value = 2
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.stateAsList.value).isEqualTo(1)
            assertThat(it.stateAsMap.value).isEqualTo(2)
        }
    }

    @Test
    fun stateInsidePopupIsRestored() {
        val activityScenario: ActivityScenario<PopupsRecreationTestActivity> =
            ActivityScenario.launch(PopupsRecreationTestActivity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        var initialValueInPopup1: Int? = null
        var initialValueInPopup2: Int? = null

        activityScenario.onActivity {
            initialValueInPopup1 = it.valueInPopup1
            initialValueInPopup2 = it.valueInPopup2
            assertThat(initialValueInPopup1).isNotNull()
            assertThat(initialValueInPopup2).isNotNull()
            assertThat(initialValueInPopup1).isNotEqualTo(initialValueInPopup2)
            it.valueInPopup1 = null
            it.valueInPopup2 = null
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.valueInPopup1).isEqualTo(initialValueInPopup1)
            assertThat(it.valueInPopup2).isEqualTo(initialValueInPopup2)
        }
    }

    @Test
    fun stateInsideDialogIsRestored() {
        val activityScenario: ActivityScenario<DialogsRecreationTestActivity> =
            ActivityScenario.launch(DialogsRecreationTestActivity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        var initialValueInDialog1: Int? = null
        var initialValueInDialog2: Int? = null

        activityScenario.onActivity {
            initialValueInDialog1 = it.valueInDialog1
            initialValueInDialog2 = it.valueInDialog2
            assertThat(initialValueInDialog1).isNotNull()
            assertThat(initialValueInDialog2).isNotNull()
            assertThat(initialValueInDialog1).isNotEqualTo(initialValueInDialog2)
            it.valueInDialog1 = null
            it.valueInDialog2 = null
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.valueInDialog1).isEqualTo(initialValueInDialog1)
            assertThat(it.valueInDialog2).isEqualTo(initialValueInDialog2)
        }
    }

    private fun FragmentActivity.findFragment(id: Int) =
        supportFragmentManager.findFragmentById(id) as TestFragment
}

class RecreationTest1Activity : BaseRestorableActivity() {

    lateinit var array: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            array = remember { intArrayOf(0) }
        }
    }
}

class RecreationTest2Activity : BaseRestorableActivity() {
    lateinit var array: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            arrayOf<Any?>()
            array = rememberSaveable { intArrayOf(0) }
        }
    }
}

class RecreationTest3Activity : BaseRestorableActivity() {

    lateinit var array1: IntArray
    lateinit var array2: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val linear = LinearLayout(this)
        linear.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup
                .LayoutParams.WRAP_CONTENT,
            1f
        )
        val child1 = ComposeView(this)
        child1.id = R.id.child1
        linear.addView(child1, params)
        val child2 = ComposeView(this)
        child2.id = R.id.child2
        linear.addView(child2, params)
        setContentView(linear)

        child1.setContent {
            arrayOf<Any?>()
            array1 = rememberSaveable(key = "key") { intArrayOf(0) }
        }
        child2.setContent {
            arrayOf<Any?>()
            array2 = rememberSaveable(key = "key") { intArrayOf(0) }
        }
    }
}

class RecreationTest4Activity : BaseRestorableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val linear = LinearLayout(this)
        linear.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup
                .LayoutParams.WRAP_CONTENT,
            1f
        )
        val child1 = FrameLayout(this)
        child1.id = R.id.child1
        linear.addView(child1, params)
        val child2 = FrameLayout(this)
        child2.id = R.id.child2
        linear.addView(child2, params)
        setContentView(linear)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.child1, TestFragment())
                .replace(R.id.child2, TestFragment())
                .commitNow()
        }
    }
}

class RecreationTest5Activity : BaseRestorableActivity() {

    lateinit var state: MutableState<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            state = rememberSaveable { mutableStateOf(0) }
        }
    }
}

class RecreationTest6Activity : BaseRestorableActivity() {

    lateinit var state: MutableState<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(0) {
                state = rememberSaveable { mutableStateOf(0) }
            }
        }
    }
}

class RecreationTest7Activity : BaseRestorableActivity() {

    lateinit var state: MutableState<Count>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(0) {
                state = rememberSaveable(stateSaver = CountSaver) { mutableStateOf(Count(0)) }
            }
        }
    }
}

class RecreationTest8Activity : BaseRestorableActivity() {

    lateinit var stateAsList: MutableState<Int>
    lateinit var stateAsMap: MutableState<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(0) {
                stateAsList = rememberSaveable(saver = StateAsListSaver) { mutableStateOf(0) }
                stateAsMap = rememberSaveable(saver = StateAsMapSaver) { mutableStateOf(0) }
            }
        }
    }
}

val StateAsListSaver = listSaver<MutableState<Int>, MutableState<Int>>(
    save = { listOf(it) },
    restore = { it[0] }
)

val StateAsMapSaver = mapSaver(
    save = { mapOf("state" to it) },
    restore = {
        @Suppress("UNCHECKED_CAST")
        it["state"] as MutableState<Int>
    }
)

class PopupsRecreationTestActivity : BaseRestorableActivity() {

    var valueInPopup1: Int? = null
    var valueInPopup2: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Popup {
                valueInPopup1 = rememberSaveable { Random.nextInt() }
            }
            Popup {
                valueInPopup2 = rememberSaveable { Random.nextInt() }
            }
        }
    }
}

class DialogsRecreationTestActivity : BaseRestorableActivity() {

    var valueInDialog1: Int? = null
    var valueInDialog2: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Dialog(onDismissRequest = {}) {
                valueInDialog1 = rememberSaveable { Random.nextInt() }
            }
            Dialog(onDismissRequest = {}) {
                valueInDialog2 = rememberSaveable { Random.nextInt() }
            }
        }
    }
}

abstract class BaseRestorableActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            super.onCreate(null)
        } else {
            // we make sure all the values we stored in bundle are really parcelable as
            // activityScenario.recreate() will not verify that and just reuse the Bundle object
            val parcel = Parcel.obtain()
            parcel.writeBundle(savedInstanceState)
            parcel.setDataPosition(0)
            val restored = parcel.readBundle(classLoader)
            super.onCreate(restored)
        }
    }
}

class TestFragment : Fragment() {

    lateinit var array: IntArray

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            arrayOf<Any?>()
            array = rememberSaveable(key = "key") { intArrayOf(0) }
        }
    }
}

data class Count(val count: Int)

val CountSaver = Saver<Count, Int>(
    save = { it.count },
    restore = { Count(it) }
)
