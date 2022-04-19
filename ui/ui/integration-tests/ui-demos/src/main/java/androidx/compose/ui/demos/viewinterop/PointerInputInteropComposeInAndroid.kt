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

package androidx.compose.ui.demos.viewinterop

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.integration.demos.common.ActivityDemo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.demos.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

@OptIn(ExperimentalComposeUiApi::class)
val ComposeInAndroidDemos = DemoCategory(
    "Compose in Android Interop",
    listOf(
        ActivityDemo(
            "Compose with no gestures in Android tap",
            ComposeNothingInAndroidTap::class
        ),
        ActivityDemo(
            "Compose tap in Android tap",
            ComposeTapInAndroidTap::class
        ),
        ActivityDemo(
            "Compose tap in Android scroll",
            ComposeTapInAndroidScroll::class
        ),
        ActivityDemo(
            "Compose scroll in Android scroll (same orientation, vertical)",
            ComposeScrollInAndroidScrollSameOrientation::class
        ),
        ActivityDemo(
            "Compose scroll in Android scroll (horizontal pager)",
            ComposeScrollInAndroidScrollSameOrientationHorizontal::class
        ),
        ActivityDemo(
            "Compose scroll in Android scroll (different orientations)",
            ComposeScrollInAndroidScrollDifferentOrientation::class
        ),
        ActivityDemo(
            "Compose in Android dialog dismisses dialog during dispatch",
            ComposeInAndroidDialogDismissDialogDuringDispatch::class
        )
    )
)

open class ComposeNothingInAndroidTap : ComponentActivity() {

    private var currentColor = Color.DarkGray

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_in_android_tap)

        findViewById<TextView>(R.id.text1).text =
            "Intended to Demonstrate that when no gestureFilterModifiers are added to compose, " +
                "Compose will not interact with the pointer input stream. This currently " +
                "isn't actually the case however. "

        findViewById<TextView>(R.id.text2).text =
            "When you tap anywhere within the bounds of the colored, including the grey box in " +
                "the middle, the color is supposed to change.  This currently does not occur " +
                "when you tap on the grey box however."

        val container = findViewById<ComposeView>(R.id.clickableContainer)
        container.isClickable = true
        container.setBackgroundColor(currentColor.toArgb())
        container.setOnClickListener {
            currentColor = if (currentColor == Color.Green) {
                Color.Red
            } else {
                Color.Green
            }
            container.setBackgroundColor(currentColor.toArgb())
        }
        container.setContent {
            Box(Modifier.background(color = Color.LightGray).fillMaxSize())
        }
    }
}

open class ComposeTapInAndroidTap : ComponentActivity() {

    private var currentColor = Color.DarkGray

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_in_android_tap)

        findViewById<TextView>(R.id.text1).text =
            "Demonstrates correct interop with simple tapping"
        findViewById<TextView>(R.id.text2).text =
            "The inner box is Compose, the outer is Android.  When you tap on the inner box, " +
                "only it changes colors. When you tap on the outer box, only the outer box " +
                "changes colors."

        val container = findViewById<ComposeView>(R.id.clickableContainer)
        container.isClickable = true
        container.setBackgroundColor(currentColor.toArgb())
        container.setOnClickListener {
            currentColor = if (currentColor == Color.Green) {
                Color.Red
            } else {
                Color.Green
            }
            container.setBackgroundColor(currentColor.toArgb())
        }

        container.setContent {
            val currentColor = remember { mutableStateOf(Color.LightGray) }

            val tap =
                Modifier.pointerInput(Unit) {
                    detectTapGestures {
                        currentColor.value =
                            if (currentColor.value == Color.Blue) Color.Yellow else Color.Blue
                    }
                }

            Column {
                Box(
                    tap.then(Modifier.background(color = currentColor.value).fillMaxSize())
                )
            }
        }
    }
}

open class ComposeTapInAndroidScroll : ComponentActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_in_android_scroll)

        findViewById<View>(R.id.container).setBackgroundColor(Color.DarkGray.toArgb())

        findViewById<TextView>(R.id.text1).text =
            "Demonstrates that press gestures and movement gestures interact correctly between " +
                "Android and Compose when Compose is inside of Android."

        findViewById<TextView>(R.id.text2).text =
            "The inner box is Compose, the rest is Android.  Tapping the inner box will change " +
                "it's color.  Putting a finger down on the inner box and dragging vertically," +
                " will cause the outer Android ScrollView to scroll and removing the finger " +
                "from the screen will not cause the Compose box to change colors. "

        val container = findViewById<ViewGroup>(R.id.container)
        container.addView(
            ComposeView(this).apply {
                setContent {
                    val currentColor = remember { mutableStateOf(Color.LightGray) }

                    Box(
                        Modifier
                            .background(color = Color.Gray)
                            .fillMaxWidth()
                            .height(456.dp)
                            .wrapContentSize()
                            .clickable {
                                currentColor.value = if (currentColor.value == Color.Blue) {
                                    Color.Yellow
                                } else {
                                    Color.Blue
                                }
                            }
                            .background(currentColor.value, RectangleShape)
                            .size(192.dp)
                    )
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }
}

open class ComposeScrollInAndroidScrollSameOrientation : ComponentActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_in_android_scroll)

        findViewById<View>(R.id.container).setBackgroundColor(Color.DarkGray.toArgb())

        findViewById<TextView>(R.id.text1).text =
            "Intended to demonstrate that scrolling between 2 scrollable things interops " +
                "\"correctly\" between Compose and Android when Compose is inside Android. " +
                "This currently does not actually work because nested scrolling interop is " +
                "not complete."

        findViewById<TextView>(R.id.text2).text =
            "The outer scrollable container always wins because it always intercepts the scroll " +
                "before the child scrolling container can start scrolling."

        val container = findViewById<ViewGroup>(R.id.container)
        container.addView(
            ComposeView(this).apply {
                setContent {
                    Column(
                        modifier = Modifier
                            .padding(48.dp)
                            .background(color = Color.Gray)
                            .fillMaxWidth()
                            .height(456.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(
                            Modifier
                                .padding(48.dp)
                                .background(color = Color.LightGray)
                                .fillMaxWidth()
                                .height(456.dp)
                        )
                    }
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }
}

open class ComposeScrollInAndroidScrollSameOrientationHorizontal : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_in_android_scroll_horizontal_pager)

        findViewById<ViewPager2>(R.id.pager).apply {
            adapter = ViewPager2Adapter(context)
        }
    }
}

internal class ViewPager2Adapter(private val ctx: Context) :
    RecyclerView.Adapter<ViewPager2Adapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(ctx).inflate(R.layout.pager_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = "ViewPager page: $position"

        holder.composeContainer.setContent {
            LazyRow(
                modifier = Modifier.border(4.dp, Color.DarkGray),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(24.dp)
            ) {
                items(5) { index ->
                    Card(
                        modifier = Modifier.height(240.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(12.dp),
                            text = "LazyRow Item: $index",
                            style = MaterialTheme.typography.h6
                        )
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return 10
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var text: TextView = itemView.findViewById(R.id.text)
        var composeContainer: ComposeView = itemView.findViewById(R.id.composeContainer)
    }
}

open class ComposeScrollInAndroidScrollDifferentOrientation : ComponentActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_in_android_scroll)

        findViewById<View>(R.id.container).setBackgroundColor(Color.DarkGray.toArgb())

        findViewById<TextView>(R.id.text1).text =
            "Demonstrates that scrolling in Compose and scrolling in Android interop correctly " +
                "when Compose is inside of Android."

        findViewById<TextView>(R.id.text2).text =
            "The inner scrollable container is Compose, the other one is Android. You can only " +
                "scroll in one orientation at a time."

        val container = findViewById<ViewGroup>(R.id.container)
        container.addView(
            ComposeView(this).apply {
                setContent {
                    Row(
                        modifier = Modifier
                            .padding(48.dp)
                            .background(color = Color.Gray)
                            .height(700.dp)
                            .width(456.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Box(
                            Modifier
                                .padding(48.dp)
                                .background(color = Color.LightGray)
                                .width(360.dp)
                                .fillMaxHeight()
                        )
                    }
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }
}

open class ComposeInAndroidDialogDismissDialogDuringDispatch : FragmentActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.compose_in_android_dialog_dismiss_dialog_during_dispatch)

        findViewById<TextView>(R.id.text1).text =
            "Demonstrates that a synchronous touch even that causes itself to be removed from " +
                "the hierarchy is safe."

        findViewById<TextView>(R.id.text2).text =
            "Open the dialog, then click the compose button in the dialog to remove the compose " +
                "button from the hierarchy synchronously."

        findViewById<Button>(R.id.showDialogButton).setOnClickListener { showDialog() }
    }

    private fun showDialog() {
        // Create and show the dialog.
        val newFragment: DialogFragment = MyDialogFragment.newInstance()
        newFragment.show(supportFragmentManager.beginTransaction(), "dialog")
    }
}

class MyDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val frameLayout = ComposeView(inflater.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        frameLayout.setContent {
            Button({ this@MyDialogFragment.dismiss() }) {
                Text("Close me")
            }
        }

        return frameLayout
    }

    companion object {
        fun newInstance(): MyDialogFragment {
            return MyDialogFragment()
        }
    }
}