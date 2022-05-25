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

package androidx.compose.integration.demos

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.integration.demos.common.ActivityDemo
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.Demo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.integration.demos.common.FragmentDemo
import androidx.compose.integration.demos.common.allLaunchableDemos
import androidx.compose.integration.demos.settings.DecorFitsSystemWindowsSetting
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoApp(
    currentDemo: Demo,
    backStackTitle: String,
    isFiltering: Boolean,
    onStartFiltering: () -> Unit,
    onEndFiltering: () -> Unit,
    onNavigateToDemo: (Demo) -> Unit,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    launchSettings: () -> Unit
) {
    val navigationIcon = (@Composable { AppBarIcons.Back(onNavigateUp) }).takeIf { canNavigateUp }

    var filterText by rememberSaveable { mutableStateOf("") }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarScrollState())

    // Only handle window insets when the system isn't doing it for us.
    val insetsModifier = if (!DecorFitsSystemWindowsSetting.asState().value) {
        Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    } else Modifier

    Scaffold(
        topBar = {
            DemoAppBar(
                title = backStackTitle,
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon ?: {},
                launchSettings = launchSettings,
                isFiltering = isFiltering,
                filterText = filterText,
                onFilter = { filterText = it },
                onStartFiltering = onStartFiltering,
                onEndFiltering = onEndFiltering
            )
        },
        modifier = Modifier
            .then(insetsModifier)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        DemoContent(modifier, currentDemo, isFiltering, filterText, onNavigateToDemo, onNavigateUp)
    }
}

@Composable
private fun DemoContent(
    modifier: Modifier,
    currentDemo: Demo,
    isFiltering: Boolean,
    filterText: String,
    onNavigate: (Demo) -> Unit,
    onNavigateUp: () -> Unit
) {
    Crossfade(isFiltering to currentDemo) { (filtering, demo) ->
        Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (filtering) {
                DemoFilter(
                    launchableDemos = AllDemosCategory.allLaunchableDemos(),
                    filterText = filterText,
                    onNavigate = onNavigate
                )
            } else {
                DisplayDemo(demo, onNavigate, onNavigateUp)
            }
        }
    }
}

@Composable
fun Material2LegacyTheme(content: @Composable () -> Unit) {
    val material2Colors =
        if (isSystemInDarkTheme()) {
            androidx.compose.material.darkColors()
        } else {
            androidx.compose.material.lightColors()
        }
    androidx.compose.material.MaterialTheme(
        colors = material2Colors,
        content = {
            CompositionLocalProvider(
                LocalContentColor provides androidx.compose.material.MaterialTheme.colors.onSurface,
                content = content
            )
        }
    )
}

@Composable
private fun DisplayDemo(demo: Demo, onNavigate: (Demo) -> Unit, onNavigateUp: () -> Unit) {
    when (demo) {
        is ActivityDemo<*> -> {
            /* should never get here as activity demos are not added to the backstack*/
        }
        is ComposableDemo ->
            // provide material 2 as well for interop, find a way to
            // remove it when all demos migrated to m3
            Material2LegacyTheme { demo.content(onNavigateUp) }
        is DemoCategory -> DisplayDemoCategory(demo, onNavigate)
        is FragmentDemo<*> -> {
            lateinit var view: FragmentContainerView
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    view = FragmentContainerView(context).also {
                        it.id = R.id.fragment_container
                    }
                    view
                }
            )
            DisposableEffect(demo) {
                // TODO: This code could be cleaner using FragmentContainerView.getFragment().
                //  Update this code once it appears in a released artifact.
                val fm = (view.context as FragmentActivity).supportFragmentManager
                fm.beginTransaction()
                    .add(R.id.fragment_container, demo.fragmentClass.java, null, null)
                    .commit()
                onDispose {
                    fm.beginTransaction().remove(fm.findFragmentById(R.id.fragment_container)!!)
                        .commit()
                }
            }
        }
    }
}

@Composable
private fun DisplayDemoCategory(category: DemoCategory, onNavigate: (Demo) -> Unit) {
    // TODO: migrate to LazyColumn after DemoTests are rewritten to accommodate laziness
    Column(Modifier.verticalScroll(rememberScrollState())) {
        category.demos.forEach { demo ->
            ListItem(onClick = { onNavigate(demo) }) {
                Text(
                    modifier = Modifier
                        .height(56.dp)
                        .wrapContentSize(Alignment.Center),
                    text = demo.title
                )
            }
        }
    }
}

@Suppress("ComposableLambdaParameterNaming", "ComposableLambdaParameterPosition")
@Composable
private fun DemoAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    navigationIcon: @Composable () -> Unit,
    isFiltering: Boolean,
    filterText: String,
    onFilter: (String) -> Unit,
    onStartFiltering: () -> Unit,
    onEndFiltering: () -> Unit,
    launchSettings: () -> Unit
) {
    if (isFiltering) {
        FilterAppBar(
            filterText = filterText,
            onFilter = onFilter,
            onClose = onEndFiltering,
            scrollBehavior = scrollBehavior
        )
    } else {
        SmallTopAppBar(
            title = {
                Text(title, Modifier.testTag(Tags.AppBarTitle))
            },
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIcon,
            actions = {
                AppBarIcons.Filter(onClick = onStartFiltering)
                AppBarIcons.Settings(onClick = launchSettings)
            }
        )
    }
}

private object AppBarIcons {
    @Composable
    fun Back(onClick: () -> Unit) {
        val icon = when (LocalLayoutDirection.current) {
            LayoutDirection.Ltr -> Icons.Filled.ArrowBack
            LayoutDirection.Rtl -> Icons.Filled.ArrowForward
        }
        IconButton(onClick = onClick) {
            Icon(icon, null)
        }
    }

    @Composable
    fun Filter(onClick: () -> Unit) {
        IconButton(modifier = Modifier.testTag(Tags.FilterButton), onClick = onClick) {
            Icon(Icons.Filled.Search, null)
        }
    }

    @Composable
    fun Settings(onClick: () -> Unit) {
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.Settings, null)
        }
    }
}

@Composable
internal fun ListItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)
) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .wrapContentHeight(Alignment.CenterVertically),
        contentAlignment = Alignment.CenterStart
    ) { content() }
}