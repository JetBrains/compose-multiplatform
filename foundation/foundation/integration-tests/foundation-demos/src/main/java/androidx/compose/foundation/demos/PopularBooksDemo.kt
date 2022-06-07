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

package androidx.compose.foundation.demos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Preview
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopularBooksDemo() {
    MaterialTheme {
        var comparator by remember { mutableStateOf(TitleComparator) }
        Column {
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Title",
                    Modifier.clickable { comparator = TitleComparator }
                        .weight(5f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Author",
                    Modifier.clickable { comparator = AuthorComparator }
                        .weight(2f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Year",
                    Modifier.clickable { comparator = YearComparator }
                        .width(50.dp)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Sales (M)",
                    Modifier.clickable { comparator = SalesComparator }
                        .width(65.dp)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                    textAlign = TextAlign.Center
                )
            }
            Divider(color = Color.LightGray, thickness = Dp.Hairline)
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val sortedList = PopularBooksList.sortedWith(comparator)
                items(sortedList, key = { it.title }) {
                    Row(
                        Modifier.animateItemPlacement()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            it.title,
                            Modifier.weight(5f)
                                .fillMaxHeight()
                                .padding(4.dp)
                                .wrapContentHeight(Alignment.CenterVertically),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            it.author,
                            Modifier.weight(2f)
                                .fillMaxHeight()
                                .padding(4.dp)
                                .wrapContentHeight(Alignment.CenterVertically),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "${it.published}",
                            Modifier.width(55.dp)
                                .fillMaxHeight()
                                .padding(4.dp)
                                .wrapContentHeight(Alignment.CenterVertically),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "${it.salesInMillions}",
                            Modifier.width(65.dp)
                                .fillMaxHeight()
                                .padding(4.dp)
                                .wrapContentHeight(Alignment.CenterVertically),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private val TitleComparator = Comparator<Book> { left, right ->
    left.title.compareTo(right.title)
}

private val AuthorComparator = Comparator<Book> { left, right ->
    left.author.compareTo(right.author)
}

private val YearComparator = Comparator<Book> { left, right ->
    right.published.compareTo(left.published)
}

private val SalesComparator = Comparator<Book> { left, right ->
    right.salesInMillions.compareTo(left.salesInMillions)
}

private val PopularBooksList = listOf(
    Book("The Hobbit", "J. R. R. Tolkien", 1937, 140),
    Book("Harry Potter and the Philosopher's Stone", "J. K. Rowling", 1997, 120),
    Book("Dream of the Red Chamber", "Cao Xueqin", 1800, 100),
    Book("And Then There Were None", "Agatha Christie", 1939, 100),
    Book("The Little Prince", "Antoine de Saint-Exupéry", 1943, 100),
    Book("The Lion, the Witch and the Wardrobe", "C. S. Lewis", 1950, 85),
    Book("The Adventures of Pinocchio", "Carlo Collodi", 1881, 80),
    Book("The Da Vinci Code", "Dan Brown", 2003, 80),
    Book("Harry Potter and the Chamber of Secrets", "J. K. Rowling", 1998, 77),
    Book("The Alchemist", "Paulo Coelho", 1988, 65),
    Book("Harry Potter and the Prisoner of Azkaban", "J. K. Rowling", 1999, 65),
    Book("Harry Potter and the Goblet of Fire", "J. K. Rowling", 2000, 65),
    Book("Harry Potter and the Order of the Phoenix", "J. K. Rowling", 2003, 65)
)

private class Book(
    val title: String,
    val author: String,
    val published: Int,
    val salesInMillions: Int
)
