/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.compose

import benchmarks.model.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import androidx.compose.runtime.Composable

@Composable
fun DataTableDocument(data: DataTableData) {
    Html(attrs = { lang("en") }) {
        Head {
            Meta(attrs = { attr("charset", "UTF-8") })
            Title { Text(data.title) }
        }
        Body {
            DataTable(data)
        }
    }
}

@Composable
fun DataTable(data: DataTableData) {
    Div(attrs = {
        id("dashboard")
        attr("data-page", "data-table")
    }) {
        // Header
        Header(attrs = { classes("page-header") }) {
            H1 { Text(data.title) }
            P { Text(data.description) }
        }

        // Filters
        Div(attrs = {
            classes("filters-bar")
            attr("role", "search")
        }) {
            Input(type = InputType.Search) {
                id("table-search")
                placeholder("Search rows...")
                attr("aria-label", "Search rows")
                data.searchQuery?.let { attr("value", it) }
            }
            Div(attrs = { classes("active-filters") }) {
                data.activeFilters.forEach { filter ->
                    Span(attrs = {
                        classes("filter-badge")
                        attr("data-filter-key", filter.key)
                        attr("data-filter-val", filter.value)
                    }) { Text("${filter.key}: ${filter.value}") }
                }
            }
        }

        // Selection Status Bar
        Div(attrs = {
            classes("selection-status")
            attr("role", "status")
            attr("aria-live", "polite")
        }) {
            Text("${data.selectedRowCount} items selected")
        }

        // Table
        Table(attrs = {
            attr("role", "grid")
            attr("aria-rowcount", data.totalRowCount.toString())
            attr("aria-colcount", data.headers.size.toString())
            attr("aria-label", "Data Table")
        }) {
            Thead {
                Tr {
                    Th(attrs = { attr("scope", "col") }) {
                        Input(type = InputType.Checkbox) {
                            attr("aria-label", "Select all rows")
                        }
                    }
                    data.headers.forEach { header ->
                        Th(attrs = {
                            attr("scope", "col")
                            attr("data-column-id", header.id)
                            attr("role", "columnheader")
                            if (header.sortable) {
                                val sortDir = if (header.id == data.sortColumnId) data.sortDirection else "none"
                                classes("sortable")
                                attr("aria-sort", sortDir)
                                if (sortDir != "none") {
                                    classes("sorted-$sortDir")
                                }
                            }
                        }) { Text(header.label) }
                    }
                }
            }
            Tbody {
                data.rows.forEachIndexed { _, row ->
                    Tr(attrs = {
                        attr("data-row-id", row.id)
                        attr("data-status", row.status)
                        if (row.selected) {
                            attr("aria-selected", "true")
                            classes("row-selected")
                        } else {
                            attr("aria-selected", "false")
                        }
                    }) {
                        Td {
                            Input(type = InputType.Checkbox) {
                                attr("aria-label", "Select row ${row.id}")
                                if (row.selected) attr("checked", "")
                            }
                        }
                        row.cells.forEach { cell ->
                            Td(attrs = {
                                attr("data-column", cell.columnId)
                                attr("data-value", cell.rawValue)
                                if (cell.tooltip != null) {
                                    val tooltipId = "tt-${row.id}-${cell.columnId}"
                                    attr("aria-describedby", tooltipId)
                                }
                            }) {
                                if (cell.isStatus) {
                                    Span(attrs = {
                                        classes("status-badge", "status-${cell.rawValue}")
                                        attr("role", "status")
                                    }) { Text(cell.displayValue) }
                                } else {
                                    Text(cell.displayValue)
                                }
                                cell.tooltip?.let { tooltip ->
                                    Span(attrs = {
                                        id("tt-${row.id}-${cell.columnId}")
                                        classes("tooltip-text")
                                        style { property("display", "none") }
                                    }) { Text(tooltip) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pagination
        Nav(attrs = { attr("aria-label", "Pagination") }) {
            Ul(attrs = { classes("pagination") }) {
                data.pages.forEach { page ->
                    Li {
                        A(href = "?page=${page.number}", attrs = {
                            attr("data-page", page.number.toString())
                            attr("aria-label", "Page ${page.number}")
                            if (page.isCurrent) {
                                attr("aria-current", "page")
                                classes("current-page")
                            }
                        }) { Text(page.number.toString()) }
                    }
                }
            }
        }
    }
}
