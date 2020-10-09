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

package androidx.compose.paging.demos.room

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.ExperimentalLazyDsl
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.paging.collectAsLazyPagingItems
import androidx.compose.paging.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.unit.sp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalLazyDsl::class)
@Composable
fun PagingRoomDemo() {
    val context = ContextAmbient.current
    val dao: UserDao = AppDatabase.getInstance(context).userDao()
    val scope = rememberCoroutineScope()

    val pageSize = 15
    val pager = remember {
        Pager(
            PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = true,
                maxSize = 200
            )
        ) {
            dao.allUsers()
        }
    }

    Column {
        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val name = Names[Random.nextInt(Names.size)]
                    dao.insert(User(id = 0, name = name))
                }
            }
        ) {
            Text("Add random user")
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    dao.clearAll()
                }
            }
        ) {
            Text("Clear all users")
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val randomUser = dao.getRandomUser()
                    if (randomUser != null) {
                        dao.delete(randomUser)
                    }
                }
            }
        ) {
            Text("Remove random user")
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val randomUser = dao.getRandomUser()
                    if (randomUser != null) {
                        val updatedUser = User(
                            randomUser.id,
                            randomUser.name + " updated"
                        )
                        dao.update(updatedUser)
                    }
                }
            }
        ) {
            Text("Update random user")
        }

        val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
        LazyColumn {
            itemsIndexed(lazyPagingItems) { index, user ->
                Text("$index " + user?.name, fontSize = 50.sp)
            }
        }
    }
}

val Names = listOf(
    "John",
    "Jack",
    "Ben",
    "Sally",
    "Tom",
    "Jinny",
    "Mark",
    "Betty",
    "Liam",
    "Noah",
    "Olivia",
    "Emma",
    "Ava"
)