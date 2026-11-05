/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.model

object Fixtures {

    fun generateCatalogData(): CatalogData {
        val platforms = listOf(
            PlatformFilter("jvm", "JVM", true),
            PlatformFilter("js", "JS", false),
            PlatformFilter("native", "Native", false),
            PlatformFilter("wasm", "Wasm", true)
        )

        val sections = (1..8).map { s ->
            CatalogSection(
                title = "Section $s",
                projects = (1..6).map { p ->
                    CatalogProject(
                        author = "Author $p",
                        name = "Project $s-$p",
                        description = "This is a detailed description for project $s-$p.",
                        starCount = s * p * 100,
                        badges = listOf("Kotlin", "Multiplatform", "Library")
                    )
                }
            )
        }

        return CatalogData("KMP Catalog", platforms, sections)
    }

    fun generateFormData(): FormData {
        return FormData(
            title = "Advanced Form",
            actionUrl = "/api/submit",
            formLabel = "User Registration",
            isSubmitting = false,
            sections = listOf(
                FormSection(
                    id = "personal",
                    title = "Personal Information",
                    fields = listOf(
                        FormField("f1", "firstName", "First Name", FormFieldType.TEXT, "John", "Enter first name", null, null, true, false, false, false, false, "given-name", null, null, 2, 50, null, null, null),
                        FormField("f2", "lastName", "Last Name", FormFieldType.TEXT, "Doe", "Enter last name", null, null, true, false, false, false, false, "family-name", null, null, 2, 50, null, null, null),
                        FormField("f3", "email", "Email", FormFieldType.EMAIL, "john.doe@example.com", "Email address", "We'll never share your email.", null, true, false, false, false, false, "email", null, null, null, null, null, null, null),
                        FormField("f4", "phone", "Phone", FormFieldType.TEL, null, "+1", null, null, false, false, false, false, false, "tel", null, null, null, null, null, null, null),
                        FormField("f5", "age", "Age", FormFieldType.NUMBER, "30", null, null, null, false, false, false, false, false, null, "18", "120", null, null, null, 1, null),
                        FormField("f6", "bio", "Bio", FormFieldType.TEXTAREA, "A brief bio...", null, null, null, false, false, false, false, false, null, null, null, null, 500, null, null, null)
                    )
                ),
                FormSection(
                    id = "preferences",
                    title = "Preferences",
                    fields = listOf(
                        FormField("p1", "theme", "Theme", FormFieldType.SELECT, null, null, null, null, false, false, false, false, false, null, null, null, null, null, null, null, listOf(
                            SelectOption("light", "Light", true, false),
                            SelectOption("dark", "Dark", false, false),
                            SelectOption("system", "System", false, false)
                        )),
                        FormField("p2", "notifications", "Enable Notifications", FormFieldType.CHECKBOX, "true", null, null, null, false, false, false, true, false, null, null, null, null, null, null, null, null),
                        FormField("p3", "frequency", "Frequency", FormFieldType.RADIO, "daily", null, null, null, false, false, false, true, false, null, null, null, null, null, null, null, null)
                    )
                ),
                FormSection(
                    id = "security",
                    title = "Security Settings",
                    fields = listOf(
                        FormField("s1", "password", "Password", FormFieldType.PASSWORD, null, "Min 8 characters", null, null, true, false, false, false, false, "new-password", null, null, 8, null, null, null, null),
                        FormField("s2", "confirmPassword", "Confirm Password", FormFieldType.PASSWORD, null, null, null, null, true, false, false, false, false, "new-password", null, null, 8, null, null, null, null),
                        FormField("s3", "twoFactor", "Two-Factor Auth", FormFieldType.CHECKBOX, "enabled", null, null, null, false, false, false, false, false, null, null, null, null, null, null, null, null)
                    )
                ),
                FormSection(
                    id = "additional",
                    title = "Additional Data",
                    fields = (1..10).map { i ->
                        FormField("a$i", "field$i", "Extra Field $i", FormFieldType.TEXT, "Value $i", null, null, null, false, false, false, false, false, null, null, null, null, null, null, null, null)
                    }
                )
            )
        )
    }

    fun generateDataTableData(): DataTableData {
        val headers = listOf(
            TableHeader("id", "ID", true),
            TableHeader("name", "Name", true),
            TableHeader("email", "Email", true),
            TableHeader("role", "Role", true),
            TableHeader("status", "Status", true),
            TableHeader("lastLogin", "Last Login", true)
        )

        val rows = (1..50).map { i ->
            DataRow(
                id = "row-$i",
                status = if (i % 3 == 0) "offline" else "online",
                selected = i % 10 == 0,
                cells = listOf(
                    DataCell("id", "U$i", "U$i", null, false),
                    DataCell("name", "User $i", "User $i", "Full name for User $i", false),
                    DataCell("email", "user$i@example.com", "user$i@example.com", null, false),
                    DataCell("role", if (i % 5 == 0) "Admin" else "User", if (i % 5 == 0) "Admin" else "User", null, false),
                    DataCell("status", if (i % 3 == 0) "offline" else "online", if (i % 3 == 0) "Offline" else "Online", null, true),
                    DataCell("lastLogin", "2023-10-01T12:00:00Z", "Oct 1, 2023", null, false)
                )
            )
        }

        val pages = (1..5).map { PageItem(it, it == 1) }

        return DataTableData(
            title = "User Directory",
            description = "A comprehensive list of all registered users.",
            searchQuery = null,
            activeFilters = mapOf("role" to "User", "status" to "online"),
            selectedRowCount = 5,
            totalRowCount = 250,
            headers = headers,
            sortColumnId = "name",
            sortDirection = "ascending",
            rows = rows,
            pages = pages
        )
    }

    fun generateSvgDashboardData(): SvgDashboardData {
        val points = (0..20).map { i -> SvgPoint(i * 10.0, 100 - (i % 5) * 20.0 + (i % 3) * 5.0) }
        val pathData = "M 0,100 " + points.joinToString(" ") { "L ${it.x},${it.y}" }
        val xLabels = (0..5).map { SvgLabel(it * 40.0, "W$it") }

        val lineChart = LineChart("Weekly Traffic", pathData, points, xLabels)

        val bars = (0..9).map { i ->
            Bar(i * 20.0, 100 - i * 10.0, 15.0, i * 10.0, i * 100.0)
        }
        val barChart = BarChart("Revenue", bars)

        val pieChart = PieChart("Device Types", listOf(
            PieSliceData("M 50 50 L 100 50 A 50 50 0 0 1 50 100 Z", "#FF0000"),
            PieSliceData("M 50 50 L 50 100 A 50 50 0 0 1 0 50 Z", "#00FF00"),
            PieSliceData("M 50 50 L 0 50 A 50 50 0 0 1 100 50 Z", "#0000FF")
        ))

        val gauge = GaugeChart(
            "Server Load",
            "M 10 90 A 40 40 0 0 1 90 90",
            75.0, 25.0, "75%"
        )

        return SvgDashboardData("Analytics Dashboard", lineChart, barChart, pieChart, gauge)
    }

    fun generateArticleData(): ArticleData {
        val sections = (1..6).map { s ->
            ContentSection(
                id = "sec-$s",
                title = "Section $s",
                blocks = listOf(
                    ContentBlock(BlockType.PARAGRAPH, "This is paragraph 1 of section $s. ".repeat(10), null, null),
                    ContentBlock(BlockType.CODE, "fun hello() { println(\"Hello Section $s\") }", "kotlin", null),
                    ContentBlock(BlockType.PARAGRAPH, "This is paragraph 2 of section $s. ".repeat(10), null, null),
                    if (s % 2 == 0) ContentBlock(BlockType.IMAGE, "Image description", null, "https://example.com/img$s.png") else ContentBlock(BlockType.LINK, "Read more", null, "https://example.com")
                )
            )
        }

        return ArticleData(
            title = "The Future of Compose HTML",
            author = "Jane Doe",
            excerpt = "An in-depth look at server-side rendering with Compose HTML.",
            publishDateIso = "2023-10-15T08:00:00Z",
            publishDateFormatted = "October 15, 2023",
            readingTimeMin = 12,
            heroImage = HeroImage("https://example.com/hero.jpg", "Compose Logo", 1200, 600),
            breadcrumbs = listOf(
                Breadcrumb("Home", "/", false),
                Breadcrumb("Blog", "/blog", false),
                Breadcrumb("The Future of Compose HTML", "/blog/compose-html", true)
            ),
            toc = sections.map { TocItem(it.id, it.title) },
            sections = sections,
            tags = listOf("Compose", "Kotlin", "Web", "SSR"),
            relatedArticles = listOf(
                RelatedArticle("/blog/kotlin-wasm", "Getting Started with Kotlin Wasm"),
                RelatedArticle("/blog/kmp", "Kotlin Multiplatform in 2024")
            )
        )
    }
}

// Top-level convenience functions used by renderers, benchmarks, and analyzer
fun catalogFixture(): CatalogData = Fixtures.generateCatalogData()
fun formFixture(): FormData = Fixtures.generateFormData()
fun dataTableFixture(): DataTableData = Fixtures.generateDataTableData()
fun svgDashboardFixture(): SvgDashboardData = Fixtures.generateSvgDashboardData()
fun articleFixture(): ArticleData = Fixtures.generateArticleData()
