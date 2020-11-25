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

import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

enum class RallyScreenState(
    val icon: ImageVector,
    val body: @Composable () -> Unit
) {
    Overview(Icons.Filled.PieChart, { OverviewBody() }),
    Accounts(Icons.Filled.AttachMoney, { AccountsBody(UserData.accounts) }),
    Bills(Icons.Filled.MoneyOff, { BillsBody(UserData.bills) })
}