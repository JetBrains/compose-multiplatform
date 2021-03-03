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

package androidx.compose.animation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun HorizontalTransitionSample() {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(
            // Set the start width to 20 (pixels), 0 by default
            initialWidth = { 20 }
        ),
        exit = shrinkHorizontally(
            // Shrink towards the end (i.e. right edge for LTR, left edge for RTL). The default
            // direction for the shrink is towards [Alignment.Start]
            shrinkTowards = Alignment.End,
            // Set the end width for the shrink animation to a quarter of the full width.
            targetWidth = { fullWidth -> fullWidth / 4 },
            // Overwrites the default animation with tween for this shrink animation.
            animationSpec = tween()
        )
    ) {
        // Content that needs to appear/disappear goes here:
        Box(Modifier.fillMaxWidth().requiredHeight(200.dp))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun SlideTransition() {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            // Offsets the content by 1/3 of its width to the left, and slide towards right
            initialOffsetX = { fullWidth -> -fullWidth / 3 },
            // Overwrites the default animation with tween for this slide animation.
            animationSpec = tween(durationMillis = 200)
        ) + fadeIn(
            // Overwrites the default animation with tween
            animationSpec = tween(durationMillis = 200)
        ),
        exit = slideOutHorizontally(
            // Overwrites the ending position of the slide-out to 200 (pixels) to the right
            targetOffsetX = { 200 },
            animationSpec = spring(stiffness = Spring.StiffnessHigh)
        ) + fadeOut()
    ) {
        // Content that needs to appear/disappear goes here:
        Box(Modifier.fillMaxWidth().requiredHeight(200.dp)) {}
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun FadeTransition() {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            // Overwrites the initial value of alpha to 0.4f for fade in, 0 by default
            initialAlpha = 0.4f
        ),
        exit = fadeOut(
            // Overwrites the default animation with tween
            animationSpec = tween(durationMillis = 250)
        )
    ) {
        // Content that needs to appear/disappear goes here:
        Text("Content to appear/disappear", Modifier.fillMaxWidth().requiredHeight(200.dp))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun FullyLoadedTransition() {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            // Start the slide from 40 (pixels) above where the content is supposed to go, to
            // produce a parallax effect
            initialOffsetY = { -40 }
        ) + expandVertically(
            expandFrom = Alignment.Top
        ) + fadeIn(initialAlpha = 0.3f),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        // Content that needs to appear/disappear goes here:
        Text("Content to appear/disappear", Modifier.fillMaxWidth().requiredHeight(200.dp))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun AnimatedVisibilityWithBooleanVisibleParamNoReceiver() {
    var visible by remember { mutableStateOf(true) }
    Box(modifier = Modifier.clickable { visible = !visible }) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.Center),
            enter = slideInVertically(
                // Start the slide from 40 (pixels) above where the content is supposed to go, to
                // produce a parallax effect
                initialOffsetY = { -40 }
            ) + expandVertically(
                expandFrom = Alignment.Top
            ) + fadeIn(initialAlpha = 0.3f),
            exit = shrinkVertically() + fadeOut(animationSpec = tween(200))
        ) { // Content that needs to appear/disappear goes here:
            // Here we can optionally define a custom enter/exit animation by creating an animation
            // using the Transition<EnterExitState> object from AnimatedVisibilityScope:
            val scale by transition.animateFloat {
                when (it) {
                    // Scale will be animating from 0.8f to 1.0f during enter transition.
                    EnterExitState.PreEnter -> 0.8f
                    EnterExitState.Visible -> 1f
                    // Scale will be animating from 1.0f to 1.2f during exit animation. If the
                    // targetValue specified for PreEnter is the same as PostExit, the enter and
                    // exit animation for this property will be symmetric.
                    EnterExitState.PostExit -> 1.2f
                }
            }
            Text(
                "Content to appear/disappear",
                Modifier.fillMaxWidth().requiredHeight(100.dp).background(Color(0xffa1feff))
                    .graphicsLayer {
                        // Apply our custom enter/exit transition
                        scaleX = scale
                        scaleY = scale
                    }.padding(top = 20.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun ColumnScope.AnimatedFloatingActionButton() {
    var expanded by remember { mutableStateOf(true) }
    FloatingActionButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp)) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Favorite",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            AnimatedVisibility(
                expanded,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(modifier = Modifier.padding(start = 12.dp), text = "Favorite")
            }
        }
    }
    Spacer(Modifier.requiredHeight(20.dp))
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun SlideInOutSample() {
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible,
        enter = slideIn(
            // Specifies the starting offset of the slide-in to be 1/4 of the width to the right,
            // 100 (pixels) below the content position, which results in a simultaneous slide up
            // and slide left.
            { fullSize -> IntOffset(fullSize.width / 4, 100) },
            tween(100, easing = LinearOutSlowInEasing)
        ),
        exit = slideOut(
            // The offset can be entirely independent of the size of the content. This specifies
            // a target offset 180 pixels to the left of the content, and 50 pixels below. This will
            // produce a slide-left combined with a slide-down.
            { IntOffset(-180, 50) },
            tween(100, easing = FastOutSlowInEasing)
        )
    ) {
        // Content that needs to appear/disappear goes here:
        Text("Content to appear/disappear", Modifier.fillMaxWidth().requiredHeight(200.dp))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun ExpandShrinkVerticallySample() {
    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible,
        // Sets the initial height of the content to 20, revealing only the top of the content at
        // the beginning of the expanding animation.
        enter = expandVertically(
            expandFrom = Alignment.Top,
            initialHeight = { 20 }
        ),
        // Shrinks the content to half of its full height via an animation.
        exit = shrinkVertically(
            targetHeight = { fullHeight -> fullHeight / 2 },
            animationSpec = tween()
        )
    ) {
        // Content that needs to appear/disappear goes here:
        Text("Content to appear/disappear", Modifier.fillMaxWidth().requiredHeight(200.dp))
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Sampled
@Composable
fun ExpandInShrinkOutSample() {
    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible,
        enter = expandIn(
            // Overwrites the corner of the content that is first revealed
            expandFrom = Alignment.BottomStart,
            // Overwrites the initial size to 50 pixels by 50 pixels
            initialSize = { IntSize(50, 50) },
            // Overwrites the default spring animation with tween
            animationSpec = tween(100, easing = LinearOutSlowInEasing)
        ),
        exit = shrinkOut(
            // Overwrites the area of the content that the shrink animation will end on. The
            // following parameters will shrink the content's clip bounds from the full size of the
            // content to 1/10 of the width and 1/5 of the height. The shrinking clip bounds will
            // always be aligned to the CenterStart of the full-content bounds.
            shrinkTowards = Alignment.CenterStart,
            // Overwrites the target size of the shrinking animation.
            targetSize = { fullSize -> IntSize(fullSize.width / 10, fullSize.height / 5) },
            animationSpec = tween(100, easing = FastOutSlowInEasing)
        )
    ) {
        // Content that needs to appear/disappear goes here:
        Text("Content to appear/disappear", Modifier.fillMaxWidth().requiredHeight(200.dp))
    }
}

@Sampled
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ColumnAnimatedVisibilitySample() {
    var itemIndex by remember { mutableStateOf(0) }
    val colors = listOf(Color.Red, Color.Green, Color.Blue)
    Column(
        Modifier.fillMaxWidth().clickable {
            itemIndex = (itemIndex + 1) % colors.size
        }
    ) {
        colors.forEachIndexed { i, color ->
            // By default ColumnScope.AnimatedVisibility expands and shrinks new content while
            // fading.
            AnimatedVisibility(i <= itemIndex) {
                Box(Modifier.requiredHeight(40.dp).fillMaxWidth().background(color))
            }
        }
    }
}

@Sampled
@OptIn(ExperimentalAnimationApi::class, ExperimentalTransitionApi::class)
@Composable
fun AVScopeAnimateEnterExit() {
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun AnimatedVisibilityScope.Item(
        modifier: Modifier,
        backgroundColor: Color
    ) {
        // Creates a custom enter/exit animation for scale property.
        val scale by transition.animateFloat { enterExitState ->
            // Enter transition will be animating the scale from 0.9f to 1.0f
            // (i.e. PreEnter -> Visible). Exit transition will be from 1.0f to
            // 0.5f (i.e. Visible -> PostExit)
            when (enterExitState) {
                EnterExitState.PreEnter -> 0.9f
                EnterExitState.Visible -> 1.0f
                EnterExitState.PostExit -> 0.5f
            }
        }

        // Since we defined `Item` as an extension function on AnimatedVisibilityScope, we can use
        // the `animateEnterExit` modifier to produce an enter/exit animation for it. This will
        // run simultaneously with the `AnimatedVisibility`'s enter/exit.
        Box(
            modifier.fillMaxWidth().padding(5.dp).animateEnterExit(
                // Slide in from below,
                enter = slideInVertically(initialOffsetY = { it }),
                // No slide on the way out. So the exit animation will be scale (from the custom
                // scale animation defined above) and fade (from AnimatedVisibility)
                exit = ExitTransition.None
            ).graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.clip(RoundedCornerShape(20.dp)).background(backgroundColor).fillMaxSize()
        ) {
            // Content of the item goes here...
        }
    }

    @OptIn(ExperimentalAnimationApi::class, ExperimentalTransitionApi::class)
    @Composable
    fun AnimateMainContent(mainContentVisible: MutableTransitionState<Boolean>) {
        Box {
            // Use the `MutableTransitionState<Boolean>` to specify whether AnimatedVisibility
            // should be visible. This will also allow AnimatedVisibility animation states to be
            // observed externally.
            AnimatedVisibility(
                visibleState = mainContentVisible,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box {
                    Column(Modifier.fillMaxSize()) {
                        // We have created `Item`s below as extension functions on
                        // AnimatedVisibilityScope in this example. So they can define their own
                        // enter/exit to run alongside the enter/exit defined in AnimatedVisibility.
                        Item(Modifier.weight(1f), backgroundColor = Color(0xffff6f69))
                        Item(Modifier.weight(1f), backgroundColor = Color(0xffffcc5c))
                    }
                    // This FAB will be simply fading in/out as specified by the AnimatedVisibility
                    FloatingActionButton(
                        onClick = {},
                        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                        backgroundColor = MaterialTheme.colors.primary
                    ) { Icon(Icons.Default.Favorite, contentDescription = null) }
                }
            }

            // Here we can get a signal for when the Enter/Exit animation of the content above
            // has finished by inspecting the MutableTransitionState passed to the
            // AnimatedVisibility. This allows sequential animation after the enter/exit.
            AnimatedVisibility(
                // Once the main content is visible (i.e. targetState == true), and no pending
                // animations. We will start another enter animation sequentially.
                visible = mainContentVisible.targetState && mainContentVisible.isIdle,
                modifier = Modifier.align(Alignment.Center),
                enter = expandVertically(),
                exit = fadeOut(animationSpec = tween(50))
            ) {
                Text("Transition Finished")
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
@Sampled
fun AddAnimatedVisibilityToGenericTransitionSample() {
    @Composable
    fun ItemMainContent() {
        Row(Modifier.height(100.dp).fillMaxWidth(), Arrangement.SpaceEvenly) {
            Box(
                Modifier.size(60.dp).align(Alignment.CenterVertically)
                    .background(Color(0xffcdb7f6), CircleShape)
            )
            Column(Modifier.align(Alignment.CenterVertically)) {
                Box(Modifier.height(30.dp).width(300.dp).padding(5.dp).background(Color.LightGray))
                Box(Modifier.height(30.dp).width(300.dp).padding(5.dp).background(Color.LightGray))
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun SelectableItem() {
        // This sample animates a number of properties, including AnimatedVisibility, as a part of
        // the Transition going between selected and unselected.
        Box(Modifier.padding(15.dp)) {
            var selected by remember { mutableStateOf(false) }
            // Creates a transition to animate visual changes when `selected` is changed.
            val selectionTransition = updateTransition(selected)
            // Animates the border color as a part of the transition
            val borderColor by selectionTransition.animateColor { isSelected ->
                if (isSelected) Color(0xff03a9f4) else Color.White
            }
            // Animates the background color when selected state changes
            val contentBackground by selectionTransition.animateColor { isSelected ->
                if (isSelected) Color(0xffdbf0fe) else Color.White
            }
            // Animates elevation as a part of the transition
            val elevation by selectionTransition.animateDp { isSelected ->
                if (isSelected) 10.dp else 2.dp
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, borderColor),
                modifier = Modifier.clickable { selected = !selected },
                color = contentBackground,
                elevation = elevation,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    ItemMainContent()
                    // Creates an AnimatedVisibility as a part of the transition, so that when
                    // selected it's visible. This will hoist all the animations that are internal
                    // to AnimatedVisibility (i.e. fade, slide, etc) to the transition. As a result,
                    // `selectionTransition` will not finish until all the animations in
                    // AnimatedVisibility as well as animations added directly to it have finished.
                    selectionTransition.AnimatedVisibility(
                        visible = { it },
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Box(Modifier.fillMaxWidth().padding(10.dp)) {
                            Text(
                                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed" +
                                    " eiusmod tempor incididunt labore et dolore magna aliqua. " +
                                    "Ut enim ad minim veniam, quis nostrud exercitation ullamco " +
                                    "laboris nisi ut aliquip ex ea commodo consequat. Duis aute " +
                                    "irure dolor."
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalTransitionApi::class)
@Sampled
@Composable
fun AnimatedVisibilityLazyColumnSample() {
    val turquoiseColors = listOf(
        Color(0xff07688C),
        Color(0xff1986AF),
        Color(0xff50B6CD),
        Color(0xffBCF8FF),
        Color(0xff8AEAE9),
        Color(0xff46CECA)
    )

    // MyModel class handles the data change of the items that are displayed in LazyColumn.
    class MyModel {
        private val _items: MutableList<ColoredItem> = mutableStateListOf()
        private var lastItemId = 0
        val items: List<ColoredItem> = _items

        // Each item has a MutableTransitionState field to track as well as to mutate the item's
        // visibility. When the MutableTransitionState's targetState changes, corresponding
        // transition will be fired. MutableTransitionState allows animation lifecycle to be
        // observed through it's [currentState] and [isIdle]. See below for details.
        inner class ColoredItem(val visible: MutableTransitionState<Boolean>, val itemId: Int) {
            val color: Color
                get() = turquoiseColors.let {
                    it[itemId % it.size]
                }
        }

        fun addNewItem() {
            lastItemId++
            _items.add(
                ColoredItem(
                    // Here the initial state of the MutableTransitionState is set to false, and
                    // target state is set to true. This will result in an enter transition for
                    // the newly added item.
                    MutableTransitionState(false).apply { targetState = true },
                    lastItemId
                )
            )
        }

        fun removeItem(item: ColoredItem) {
            // By setting the targetState to false, this will effectively trigger an exit
            // animation in AnimatedVisibility.
            item.visible.targetState = false
        }

        @OptIn(ExperimentalTransitionApi::class)
        fun pruneItems() {
            // Inspect the animation status through MutableTransitionState. If isIdle == true,
            // all animations have finished for the transition.
            _items.removeAll(
                items.filter {
                    // This checks that the animations have finished && the animations are exit
                    // transitions. In other words, the item has finished animating out.
                    it.visible.isIdle && !it.visible.targetState
                }
            )
        }

        fun removeAll() {
            _items.forEach {
                it.visible.targetState = false
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class, ExperimentalTransitionApi::class)
    @Composable
    fun AnimatedVisibilityInLazyColumn() {
        Column {
            val model = remember { MyModel() }
            Row(Modifier.fillMaxWidth()) {
                Button({ model.addNewItem() }, modifier = Modifier.padding(15.dp).weight(1f)) {
                    Text("Add")
                }
            }

            // This sets up a flow to check whether any item has finished animating out. If yes,
            // notify the model to prune the list.
            LaunchedEffect(model) {
                snapshotFlow {
                    model.items.firstOrNull { it.visible.isIdle && !it.visible.targetState }
                }.collect {
                    if (it != null) {
                        model.pruneItems()
                    }
                }
            }
            LazyColumn {
                items(model.items, key = { it.itemId }) { item ->
                    AnimatedVisibility(
                        item.visible,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Box(Modifier.fillMaxWidth().requiredHeight(90.dp).background(item.color)) {
                            Button(
                                { model.removeItem(item) },
                                modifier = Modifier.align(Alignment.CenterEnd).padding(15.dp)
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }

            Button(
                { model.removeAll() },
                modifier = Modifier.align(Alignment.End).padding(15.dp)
            ) {
                Text("Clear All")
            }
        }
    }
}

@Sampled
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AVColumnScopeWithMutableTransitionState() {
    var visible by remember { mutableStateOf(true) }
    val colors = remember { listOf(Color(0xff2a9d8f), Color(0xffe9c46a), Color(0xfff4a261)) }
    Column {
        repeat(3) {
            AnimatedVisibility(
                visibleState = remember {
                    // This sets up the initial state of the AnimatedVisibility to false to
                    // guarantee an initial enter transition. In contrast, initializing this as
                    // `MutableTransitionState(visible)` would result in no initial enter
                    // transition.
                    MutableTransitionState(initialState = false)
                }.apply {
                    // This changes the target state of the visible state. If it's different than
                    // the initial state, an enter/exit transition will be triggered.
                    targetState = visible
                },
            ) { // Content that needs to appear/disappear goes here:
                Box(Modifier.fillMaxWidth().height(100.dp).background(colors[it]))
            }
        }
    }
}

@Sampled
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimateEnterExitPartialContent() {
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun FullScreenNotification(visible: Boolean) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(), exit = fadeOut()
        ) {
            // Fade in/out the background and foreground
            Box(Modifier.fillMaxSize().background(Color(0x88000000))) {
                Box(
                    Modifier.align(Alignment.TopStart).animateEnterExit(
                        // Slide in/out the rounded rect
                        enter = slideInVertically(),
                        exit = slideOutVertically()
                    ).clip(RoundedCornerShape(10.dp)).requiredHeight(100.dp)
                        .fillMaxWidth().background(Color.White)
                ) {
                    // Content of the notification goes here
                }
            }
        }
    }
}
