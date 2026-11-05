/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.model

data class CatalogData(
    val title: String,
    val platforms: List<PlatformFilter>,
    val sections: List<CatalogSection>
)

data class PlatformFilter(
    val id: String,
    val name: String,
    val active: Boolean
)

data class CatalogSection(
    val title: String,
    val projects: List<CatalogProject>
)

data class CatalogProject(
    val author: String,
    val name: String,
    val description: String,
    val starCount: Int,
    val badges: List<String>
)

// FormData
data class FormData(
    val title: String,
    val actionUrl: String,
    val formLabel: String,
    val isSubmitting: Boolean,
    val sections: List<FormSection>
)

data class FormSection(
    val id: String,
    val title: String,
    val fields: List<FormField>
)

enum class FormFieldType { TEXT, EMAIL, PASSWORD, NUMBER, TEL, SELECT, TEXTAREA, RADIO, CHECKBOX }

data class FormField(
    val id: String,
    val name: String,
    val label: String,
    val type: FormFieldType,
    val value: String?,
    val placeholder: String?,
    val helpText: String?,
    val errorMessage: String?,
    val required: Boolean,
    val disabled: Boolean,
    val readOnly: Boolean,
    val checked: Boolean,
    val multiple: Boolean,
    val autoComplete: String?,
    val min: String?,
    val max: String?,
    val minLength: Int?,
    val maxLength: Int?,
    val pattern: String?,
    val step: Number?,
    val options: List<SelectOption>?
)

data class SelectOption(
    val value: String,
    val label: String,
    val selected: Boolean,
    val disabled: Boolean
)

// DataTableData
data class DataTableData(
    val title: String,
    val description: String,
    val searchQuery: String?,
    val activeFilters: Map<String, String>,
    val selectedRowCount: Int,
    val totalRowCount: Int,
    val headers: List<TableHeader>,
    val sortColumnId: String,
    val sortDirection: String,
    val rows: List<DataRow>,
    val pages: List<PageItem>
)

data class TableHeader(
    val id: String,
    val label: String,
    val sortable: Boolean
)

data class DataRow(
    val id: String,
    val status: String,
    val selected: Boolean,
    val cells: List<DataCell>
)

data class DataCell(
    val columnId: String,
    val rawValue: String,
    val displayValue: String,
    val tooltip: String?,
    val isStatus: Boolean
)

data class PageItem(
    val number: Int,
    val isCurrent: Boolean
)

// SvgDashboardData
data class SvgDashboardData(
    val title: String,
    val lineChart: LineChart,
    val barChart: BarChart,
    val pieChart: PieChart,
    val gauge: GaugeChart
)

data class LineChart(
    val title: String,
    val pathData: String,
    val points: List<SvgPoint>,
    val xLabels: List<SvgLabel>
)

data class SvgPoint(val x: Double, val y: Double)
data class SvgLabel(val x: Double, val text: String)

data class BarChart(
    val title: String,
    val bars: List<Bar>
)

data class Bar(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val value: Double
)

data class PieChart(
    val title: String,
    val slices: List<PieSliceData>
)

data class PieSliceData(
    val pathData: String,
    val color: String
)

data class GaugeChart(
    val title: String,
    val valuePathData: String,
    val needleX: Double,
    val needleY: Double,
    val valueLabel: String
)

// ArticleData
data class ArticleData(
    val title: String,
    val author: String,
    val excerpt: String,
    val publishDateIso: String,
    val publishDateFormatted: String,
    val readingTimeMin: Int,
    val heroImage: HeroImage,
    val breadcrumbs: List<Breadcrumb>,
    val toc: List<TocItem>,
    val sections: List<ContentSection>,
    val tags: List<String>,
    val relatedArticles: List<RelatedArticle>
)

data class HeroImage(
    val url: String,
    val alt: String,
    val width: Int,
    val height: Int
)

data class Breadcrumb(
    val label: String,
    val url: String,
    val isCurrent: Boolean
)

data class TocItem(
    val targetId: String,
    val title: String
)

data class ContentSection(
    val id: String,
    val title: String,
    val blocks: List<ContentBlock>
)

enum class BlockType { PARAGRAPH, CODE, IMAGE, LINK }

data class ContentBlock(
    val type: BlockType,
    val content: String,
    val language: String?,
    val url: String?
)

data class RelatedArticle(
    val url: String,
    val title: String
)

// preact-render-to-string benchmark fixtures
data class SearchResultsData(
    val items: List<SearchResultItem>,
    val footerSections: List<SearchFooterSection>
)

data class SearchResultItem(
    val id: Int,
    val title: String,
    val price: String,
    val image: String
)

data class SearchFooterSection(
    val title: String,
    val links: List<SearchFooterLink>,
    val headingHref: String? = null
)

data class SearchFooterLink(
    val label: String,
    val href: String
)
