/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.benchmark.dbmonster

import androidx.compose.Composable
import androidx.compose.Model
import androidx.ui.core.Text
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row

import kotlin.random.Random

private fun randomQuery(random: Random): String = random.nextDouble().let {
    when {
        it < 0.1 -> "Idle"
        it < 0.2 -> "vacuum"
        else -> "SELECT blah FROM something"
    }
}

private const val MAX_ELAPSED = 15.0

@Model
class Query(random: Random, var elapsed: Double = random.nextDouble() * MAX_ELAPSED) {
    var query = randomQuery(random)
}

@Model
class Database(var name: String, val random: Random) {
    var queries: List<Query> = (1..10).map {
        Query(random)
    }
    fun topQueries(n: Int): List<Query> {
        return queries/*.sortedByDescending { it.elapsed }*/.take(n)
    }
    fun update() {
        val r = random.nextInt(queries.size)
        (0..r).forEach {
            queries[it].elapsed = random.nextDouble() * MAX_ELAPSED
        }
    }
}

class DatabaseList(n: Int, val random: Random) {
    val databases: List<Database> = (0..n).flatMap {
        listOf(
            Database("cluster $it", random),
            Database("cluster $it slave", random)
        )
    }
    fun update(n: Int) {
        // update n random databases in the list
        databases.shuffled(random).take(n).forEach { it.update() }
    }
}

@Composable
fun QueryColumn(query: Query) {
    // TODO: we could do some conditional styling here which would make the test better
    Column(LayoutHeight.Fill) {
        Text(text = "${query.elapsed}")
        Text(text = query.query)
    }
}

@Composable
fun DatabaseRow(db: Database) {
    println(db)
    val columns = 5
    val topQueries = db.topQueries(columns)
    Row(LayoutWidth.Fill) {
        Column(LayoutHeight.Fill) { Text(text = db.name) }
        Column(LayoutHeight.Fill) { Text(text = "${db.queries.size}") }
        topQueries.forEach { query ->
            QueryColumn(query = query)
        }
    }
}
