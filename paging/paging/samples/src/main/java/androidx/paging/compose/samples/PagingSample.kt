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

package androidx.paging.compose.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.ExperimentalLazyDsl
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import androidx.paging.compose.itemsIndexed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.math.ceil

class MyBackend {
    private val backendDataList = (0..60).toList().map { "[Item $it is from backend]" }
    val DataBatchSize = 5

    class DesiredLoadResultPageResponse(
        val data: List<String>
    )

    /**
     * Returns [DataBatchSize] items for a key
     */
    fun searchItemsByKey(key: Int): DesiredLoadResultPageResponse {
        val maxKey = ceil(backendDataList.size.toFloat() / DataBatchSize).toInt()

        if (key >= maxKey) {
            return DesiredLoadResultPageResponse(emptyList())
        }

        val from = key * DataBatchSize
        val to = minOf( (key + 1) * DataBatchSize, backendDataList.size)
        val currentSublist = backendDataList.subList(from, to)

        return DesiredLoadResultPageResponse(currentSublist)
    }

    fun getAllData(): PagingSource<Int, String> {
        // Example from https://developer.android.com/reference/kotlin/androidx/paging/PagingSource
        return object : PagingSource<Int, String>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
                // Simulate latency
                delay(2000)

                val pageNumber = params.key ?: 0

                val response = searchItemsByKey(pageNumber)

                // Since 0 is the lowest page number, return null to signify no more pages should
                // be loaded before it.
                val prevKey = if (pageNumber > 0) pageNumber - 1 else null

                // This API defines that it's out of data when a page returns empty. When out of
                // data, we return `null` to signify no more pages should be loaded
                val nextKey = if (response.data.isNotEmpty()) pageNumber + 1 else null

                return LoadResult.Page(
                    data = response.data,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            }
        }
    }
}

@OptIn(ExperimentalLazyDsl::class)
@Sampled
@Composable
fun PagingBackendSample() {
    val myBackend = remember { MyBackend() }

    val pager = remember {
        Pager(
            PagingConfig(
                pageSize = myBackend.DataBatchSize,
                enablePlaceholders = true,
                maxSize = 200
            )
        ) { myBackend.getAllData() }
    }

    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

    LazyColumn {
        if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
            item {
                Text(
                    text = "Waiting for items to load from the backend",
                    modifier = Modifier.fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }

        itemsIndexed(lazyPagingItems) { index, item ->
            Text("Index=$index: $item", fontSize = 20.sp)
        }

        if (lazyPagingItems.loadState.append == LoadState.Loading) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@OptIn(ExperimentalLazyDsl::class)
@Sampled
@Composable
fun ItemsDemo(flow: Flow<PagingData<String>>) {
    val lazyPagingItems = flow.collectAsLazyPagingItems()
    LazyColumn {
        items(lazyPagingItems) {
            Text("Item is $it")
        }
    }
}

@OptIn(ExperimentalLazyDsl::class)
@Sampled
@Composable
fun ItemsIndexedDemo(flow: Flow<PagingData<String>>) {
    val lazyPagingItems = flow.collectAsLazyPagingItems()
    LazyColumn {
        itemsIndexed(lazyPagingItems) { index, item ->
            Text("Item at index $index is $item")
        }
    }
}