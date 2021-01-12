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

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun OverviewBody() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(RallyDefaultPadding)
    ) {
        item {
            AlertCard()
        }
        item {
            AccountsCard()
        }
        item {
            BillsCard()
        }
    }
}

/**
 * The Alerts card within the Rally Overview screen.
 */
@Composable
private fun AlertCard() {
    var openDialog by remember { mutableStateOf(false) }
    val alertMessage = "Heads up, you've used up 90% of your Shopping budget for this month."

    if (openDialog) {
        RallyAlertDialog(
            onDismiss = {
                openDialog = false
            },
            bodyText = alertMessage,
            buttonText = "Dismiss".toUpperCase(Locale.getDefault())
        )
    }
    Card {
        Column {
            AlertHeader { openDialog = true }
            RallyDivider(
                modifier = Modifier.padding(start = RallyDefaultPadding, end = RallyDefaultPadding)
            )
            AlertItem(alertMessage)
        }
    }
}

@Composable
private fun AlertHeader(onClickSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.padding(RallyDefaultPadding).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Alerts",
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        TextButton(
            onClick = onClickSeeAll,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("SEE ALL")
        }
    }
}

@Composable
private fun AlertItem(message: String) {
    // TODO: Make alerts into a data structure
    Row(
        modifier = Modifier.padding(RallyDefaultPadding),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            style = MaterialTheme.typography.h3,
            modifier = Modifier.weight(1f),
            text = message
        )
        IconButton(
            onClick = {},
            modifier = Modifier.align(Alignment.Top)
        ) {
            Icon(Icons.Filled.Sort)
        }
    }
}

/**
 * Base structure for cards in the Overview screen.
 */
@SuppressLint("UnnecessaryLambdaCreation")
@Composable
private fun <T> OverviewScreenCard(
    title: String,
    amount: Float,
    onClickSeeAll: () -> Unit,
    data: List<T>,
    content: @Composable (T) -> Unit
) {
    Card {
        Column {
            Column(Modifier.padding(RallyDefaultPadding)) {
                Text(text = title, style = MaterialTheme.typography.subtitle2)
                val amountText = "$" + formatAmount(amount)
                Text(text = amountText, style = MaterialTheme.typography.h2)
            }
            Divider(color = rallyGreen, thickness = 1.dp)
            Column(Modifier.padding(start = 16.dp, top = 4.dp, end = 8.dp)) {
                data.take(3).forEach { content(it) }
                SeeAllButton(onClick = onClickSeeAll)
            }
        }
    }
}

/**
 * The Accounts card within the Rally Overview screen.
 */
@Composable
private fun AccountsCard() {
    val amount = UserData.accounts.map { account -> account.balance }.sum()
    OverviewScreenCard(
        title = "Accounts",
        amount = amount,
        onClickSeeAll = {
            // TODO: Figure out navigation
        },
        data = UserData.accounts
    ) { account ->
        AccountRow(
            name = account.name,
            number = account.number,
            amount = account.balance,
            color = account.color
        )
    }
}

/**
 * The Bills card within the Rally Overview screen.
 */
@Composable
private fun BillsCard() {
    val amount = UserData.bills.map { bill -> bill.amount }.sum()
    OverviewScreenCard(
        title = "Bills",
        amount = amount,
        onClickSeeAll = {
            // TODO: Figure out navigation
        },
        data = UserData.bills
    ) { bill ->
        BillRow(
            name = bill.name,
            due = bill.due,
            amount = bill.amount,
            color = bill.color
        )
    }
}

@Composable
private fun SeeAllButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.preferredHeight(44.dp).fillMaxWidth()
    ) {
        Text("SEE ALL")
    }
}

private val RallyDefaultPadding = 12.dp
