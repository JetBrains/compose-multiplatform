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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.integration.demos.common.ActivityDemo
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.Demo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.integration.demos.common.allLaunchableDemos
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

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

    var filterText by savedInstanceState { "" }

    Scaffold(
        topBar = {
            DemoAppBar(
                title = backStackTitle,
                navigationIcon = navigationIcon,
                launchSettings = launchSettings,
                isFiltering = isFiltering,
                filterText = filterText,
                onFilter = { filterText = it },
                onStartFiltering = onStartFiltering,
                onEndFiltering = onEndFiltering
            )
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        DemoContent(modifier, currentDemo, isFiltering, filterText, onNavigateToDemo)
    }
}

@Composable
private fun DemoContent(
    modifier: Modifier,
    currentDemo: Demo,
    isFiltering: Boolean,
    filterText: String,
    onNavigate: (Demo) -> Unit
) {
    Crossfade(isFiltering to currentDemo) { (filtering, demo) ->
        Surface(modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            if (filtering) {
                DemoFilter(
                    launchableDemos = AllDemosCategory.allLaunchableDemos(),
                    filterText = filterText,
                    onNavigate = onNavigate
                )
            } else {
                DisplayDemo(demo, onNavigate)
            }
        }
    }
}

@Composable
private fun DisplayDemo(demo: Demo, onNavigate: (Demo) -> Unit) {
    when (demo) {
        is ActivityDemo<*> -> {
            /* should never get here as activity demos are not added to the backstack*/
        }
        is ComposableDemo -> demo.content()
        is DemoCategory -> DisplayDemoCategory(demo, onNavigate)
    }
}

@Composable
private fun DisplayDemoCategory(category: DemoCategory, onNavigate: (Demo) -> Unit) {
    // TODO: migrate to LazyColumn after b/175671850
    Column(Modifier.verticalScroll(rememberScrollState())) {
        category.demos.forEach { demo ->
            ListItem(
                text = {
                    Text(
                        modifier = Modifier.preferredHeight(56.dp)
                            .wrapContentSize(Alignment.Center),
                        text = demo.title
                    )
                },
                modifier = Modifier.clickable { onNavigate(demo) }
            )
        }
    }
}

@Suppress("ComposableLambdaParameterNaming", "ComposableLambdaParameterPosition")
@Composable
private fun DemoAppBar(
    title: String,
    navigationIcon: @Composable (() -> Unit)?,
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
            onClose = onEndFiltering
        )
    } else {
        TopAppBar(
            title = {
                Text(title, Modifier.testTag(Tags.AppBarTitle))
            },
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
        val icon = when (AmbientLayoutDirection.current) {
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
