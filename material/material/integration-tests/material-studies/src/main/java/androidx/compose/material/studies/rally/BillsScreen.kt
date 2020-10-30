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

package androidx.compose.material.studies.rally

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The Bills screen.
 */
@Composable
fun BillsBody(bills: List<Bill>) {
    Box(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        val accountsProportion = bills.extractProportions { it.amount }
        val colors = bills.map { it.color }
        AnimatedCircle(
            Modifier.align(Alignment.Center).preferredHeight(300.dp).fillMaxWidth(),
            accountsProportion,
            colors
        )
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text(
                text = "Due",
                style = MaterialTheme.typography.body1,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = "$1,810.00",
                style = MaterialTheme.typography.h2,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
    Spacer(Modifier.preferredHeight(10.dp))
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            bills.forEach { bill ->
                BillRow(
                    name = bill.name,
                    due = bill.due,
                    amount = bill.amount,
                    color = bill.color
                )
            }
        }
    }
}