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
fun TailwindCatalogDocument(data: CatalogData) {
    Html(attrs = { lang("en") }) {
        Head {
            Meta(attrs = { attr("charset", "UTF-8") })
            Title { Text(data.title) }
            Link(attrs = {
                attr("rel", "stylesheet")
                href("/styles.css")
            })
        }
        Body(attrs = { classes("bg-slate-50", "text-slate-900", "font-sans", "antialiased", "min-h-screen") }) {
            TailwindCatalog(data)
        }
    }
}

@Composable
fun TailwindCatalog(data: CatalogData) {
    Div(attrs = { classes("max-w-7xl", "mx-auto", "px-4", "sm:px-6", "lg:px-8", "py-12") }) {
        // Header & Search
        Div(attrs = { classes("flex", "flex-col", "md:flex-row", "justify-between", "items-center", "mb-8") }) {
            H1(attrs = { classes("text-3xl", "font-extrabold", "tracking-tight", "text-slate-900") }) { Text(data.title) }
            Form(action = "/search", attrs = { classes("mt-4", "md:mt-0", "flex", "w-full", "md:w-auto") }) {
                Input(type = InputType.Search) {
                    classes("shadow-sm", "focus:ring-indigo-500", "focus:border-indigo-500", "block", "w-full", "sm:text-sm", "border-gray-300", "rounded-md")
                    name("q")
                    placeholder("Search projects...")
                }
                Button(attrs = {
                    type(ButtonType.Submit)
                    classes("ml-3", "inline-flex", "justify-center", "py-2", "px-4", "border", "border-transparent", "shadow-sm", "text-sm", "font-medium", "rounded-md", "text-white", "bg-indigo-600", "hover:bg-indigo-700")
                }) { Text("Search") }
            }
        }

        // Filters
        Div(attrs = { classes("flex", "space-x-4", "mb-8", "overflow-x-auto", "pb-2") }) {
            data.platforms.forEach { platform ->
                A(href = "?platform=${platform.id}", attrs = {
                    classes("px-3", "py-2", "font-medium", "text-sm", "rounded-md")
                    if (platform.active) {
                        classes("bg-indigo-100", "text-indigo-700")
                    } else {
                        classes("text-slate-500", "hover:text-slate-700", "bg-slate-100", "hover:bg-slate-200")
                    }
                }) { Text(platform.name) }
            }
        }

        // Project Grid
        data.sections.forEach { section ->
            Div(attrs = { classes("mb-12") }) {
                H2(attrs = { classes("text-2xl", "font-bold", "text-slate-900", "mb-6") }) { Text(section.title) }
                Div(attrs = { classes("grid", "grid-cols-1", "gap-6", "sm:grid-cols-2", "lg:grid-cols-3") }) {
                    section.projects.forEach { project ->
                        Div(attrs = { classes("bg-white", "overflow-hidden", "shadow", "rounded-lg", "border", "border-slate-200", "flex", "flex-col") }) {
                            Div(attrs = { classes("px-4", "py-5", "sm:p-6", "flex-grow") }) {
                                Div(attrs = { classes("flex", "items-center", "justify-between", "mb-4") }) {
                                    Span(attrs = { classes("text-sm", "font-medium", "text-indigo-600") }) { Text(project.author) }
                                    Span(attrs = { classes("inline-flex", "items-center", "px-2.5", "py-0.5", "rounded-full", "text-xs", "font-medium", "bg-yellow-100", "text-yellow-800") }) {
                                        Text("★ ${project.starCount}")
                                    }
                                }
                                H3(attrs = { classes("text-lg", "font-medium", "text-slate-900", "mb-2") }) { Text(project.name) }
                                P(attrs = { classes("text-sm", "text-slate-500", "line-clamp-3") }) { Text(project.description) }
                            }
                            Div(attrs = { classes("bg-slate-50", "px-4", "py-4", "sm:px-6", "flex", "flex-wrap", "gap-2") }) {
                                project.badges.forEach { badge ->
                                    Span(attrs = { classes("inline-flex", "items-center", "px-2", "py-0.5", "rounded", "text-xs", "font-medium", "bg-slate-100", "text-slate-800", "border", "border-slate-200") }) {
                                        Text(badge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Footer
        Footer(attrs = { classes("mt-12", "border-t", "border-slate-200", "pt-8", "text-center", "pb-12") }) {
            P(attrs = { classes("text-sm", "text-slate-500") }) { Text("© Compose HTML Benchmarks.") }
        }
    }
}
