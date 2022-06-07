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

package androidx.compose.runtime.mock

import androidx.compose.runtime.Composable

@Composable
fun ReportsTo(report: Report) {
    Text(report.from)
    Text("reports to")
    Text(report.to)
}

fun MockViewValidator.ReportsTo(report: Report) {
    Text(report.from)
    Text("reports to")
    Text(report.to)
}

@Composable
fun ReportsReport(reports: Iterable<Report>) {
    Linear {
        Repeated(of = reports) { report ->
            ReportsTo(report)
        }
    }
}

fun MockViewValidator.ReportsReport(reports: Iterable<Report>) {
    Linear {
        Repeated(of = reports) { report ->
            ReportsTo(report)
        }
    }
}
