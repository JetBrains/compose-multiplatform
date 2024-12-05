package benchmarks.complexlazylist.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import benchmarks.complexlazylist.components.refresh.LOADING_MORE
import benchmarks.complexlazylist.components.refresh.LoadingIndicatorDefault
import benchmarks.complexlazylist.components.refresh.NORMAL
import benchmarks.complexlazylist.components.refresh.REFRESHING
import benchmarks.complexlazylist.components.refresh.SwipeRefreshLayout
import benchmarks.complexlazylist.components.refresh.rememberSwipeRefreshState
import benchmarks.complexlazylist.models.IBaseViewModel
import benchmarks.complexlazylist.models.ICompositionModel
import benchmarks.complexlazylist.models.fetchCompositionModels

// just for demo
internal var models: MutableList<IBaseViewModel> = mutableStateListOf()

@Composable
fun MainUiNoImageUseModel() {
    MaterialTheme {
        MainLazyColumnItemsList(noImage = true, useJson = false)
        DisposableEffect(Unit) {
            onDispose {
                models.clear()
            }
        }
    }
}

@Composable
fun MainLazyColumnItemsList(noImage: Boolean, useJson: Boolean) {
    val scope = rememberCoroutineScope()
    val state = rememberSwipeRefreshState(NORMAL)

    LaunchedEffect(scope) {
        scope.launch(Dispatchers.Default) {
            fetchCompositionModels(false) { list ->
                for (item in list)
                    models.add(item)
            }
        }
    }

    SwipeRefreshLayout(
        state = state,
        indicator = { modifier, s, indicatorHeight ->
            LoadingIndicatorDefault(modifier, s, indicatorHeight)
        },
        onRefresh = {
            scope.launch {
                state.loadState = REFRESHING
                delay(2000)
                fetchCompositionModels(useJson) {
                    models.clear()
                    for (item in it)
                        models.add(item)
                    state.loadState = NORMAL
                }
            }

        },
        onLoadMore = {
            scope.launch {
                state.loadState = LOADING_MORE
                delay(2000L)
                fetchCompositionModels(useJson) {
                    for (item in it)
                        models.add(item)
                    state.loadState = NORMAL
                }
            }
        }
    ) { modifier ->
        run {
            val state = rememberLazyListState()
            LaunchedEffect(Unit) {
                while (true) {
                    withFrameMillis { }
                    state.scrollBy(20f)
                }
            }
            LazyColumn(modifier, state = state) {
                itemsIndexed(
                    items = models,
                    key = { index, _ ->
                        models[index]
                    }
                ) { _, item ->
                    when (item) {
                        is ICompositionModel -> MultiCellUI(item)
                        // .. todo need more types
                        else -> throw RuntimeException("Unexpected")
                    }
                }
            }
        }
    }
}

@Composable
fun MultiCellUI(item: IBaseViewModel) {
    DecoratedCell(item)
}