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
 * The Accounts screen.
 */
@Composable
fun AccountsBody(accounts: List<Account>) {
    Box(Modifier.verticalScroll(rememberScrollState(0f)).padding(16.dp)) {
        val accountsProportion = accounts.extractProportions { it.balance }
        val colors = accounts.map { it.color }
        AnimatedCircle(
            Modifier.preferredHeight(300.dp).align(Alignment.Center).fillMaxWidth(),
            accountsProportion,
            colors
        )
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.body1,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = "$12,132.49",
                style = MaterialTheme.typography.h2,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
    Spacer(Modifier.preferredHeight(10.dp))
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            accounts.forEach { account ->
                AccountRow(
                    name = account.name,
                    number = account.number,
                    amount = account.balance,
                    color = account.color
                )
            }
        }
    }
}