/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.hydration

import androidx.compose.runtime.Composable
import benchmarks.compose.*
import benchmarks.model.*

internal val hydrationWorkloads = listOf(
    "tailwind-catalog", "form-app", "data-table", "svg-dashboard", "content-article", "hydrate1k",
)

// JVM fixture generation and browser hydration use exactly the same content and data.
internal fun hydrationContent(workload: String): @Composable () -> Unit = when (workload) {
    "tailwind-catalog" -> catalogFixture().let { data -> { TailwindCatalog(data) } }
    "form-app" -> formFixture().let { data -> { FormApp(data) } }
    "data-table" -> dataTableFixture().let { data -> { DataTable(data) } }
    "svg-dashboard" -> svgDashboardFixture().let { data -> { SvgDashboard(data) } }
    "content-article" -> articleFixture().let { data -> { ContentArticle(data) } }
    "hydrate1k" -> hydrate1kFixture().let { data -> { Hydrate1kTable(data) } }
    else -> error("Unknown hydration workload: $workload")
}
