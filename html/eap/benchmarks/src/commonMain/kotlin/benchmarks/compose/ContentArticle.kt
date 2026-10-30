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
fun ContentArticleDocument(data: ArticleData) {
    Html(attrs = { lang("en") }) {
        Head {
            Meta(attrs = { attr("charset", "UTF-8") })
            Title { Text(data.title) }
            Meta(attrs = {
                attr("name", "description")
                attr("content", data.excerpt)
            })
        }
        Body {
            ContentArticle(data)
        }
    }
}

@Composable
fun ContentArticle(data: ArticleData) {
    // Breadcrumbs
    Nav(attrs = { attr("aria-label", "Breadcrumb") }) {
        Ol {
            data.breadcrumbs.forEach { crumb ->
                Li {
                    A(href = crumb.url, attrs = {
                        if (crumb.isCurrent) attr("aria-current", "page")
                    }) { Text(crumb.label) }
                }
            }
        }
    }

    Main {
        Article {
            // Header
            Header {
                H1 { Text(data.title) }
                Div(attrs = { classes("meta") }) {
                    Span { Text("By ${data.author}") }
                    TagElement(
                        elementBuilder = org.jetbrains.compose.web.dom.ElementBuilder.createBuilder("time"),
                        applyAttrs = { attr("datetime", data.publishDateIso) },
                        content = { Text(data.publishDateFormatted) }
                    )
                    Span { Text("${data.readingTimeMin} min read") }
                }
                Img(src = data.heroImage.url, alt = data.heroImage.alt, attrs = {
                    attr("width", data.heroImage.width.toString())
                    attr("height", data.heroImage.height.toString())
                    attr("loading", "lazy")
                })
            }

            // Table of Contents
            Nav(attrs = { attr("aria-label", "Table of contents") }) {
                H2 { Text("Table of Contents") }
                Ul {
                    data.toc.forEach { item ->
                        Li { A(href = "#${item.targetId}") { Text(item.title) } }
                    }
                }
            }

            // Content Sections
            data.sections.forEach { section ->
                Section(attrs = { id(section.id) }) {
                    H2 { Text(section.title) }
                    section.blocks.forEach { block ->
                        when (block.type) {
                            BlockType.PARAGRAPH -> P { Text(block.content) }
                            BlockType.CODE -> Pre {
                                Code(attrs = { attr("data-language", block.language ?: "text") }) {
                                    Text(block.content)
                                }
                            }
                            BlockType.IMAGE -> Img(src = block.url ?: "", alt = block.content, attrs = {
                                attr("loading", "lazy")
                            })
                            BlockType.LINK -> P {
                                A(href = block.url ?: "", attrs = {
                                    attr("target", "_blank")
                                    attr("rel", "noreferrer")
                                }) { Text(block.content) }
                            }
                        }
                    }
                }
            }

            // Footer / Tags
            Footer {
                Ul(attrs = { classes("tags") }) {
                    data.tags.forEach { tag ->
                        Li { A(href = "/tags/$tag") { Text(tag) } }
                    }
                }
            }
        }

        Aside(attrs = { attr("aria-label", "Related articles") }) {
            H3 { Text("Related Articles") }
            Ul {
                data.relatedArticles.forEach { article ->
                    Li {
                        A(href = article.url) { Text(article.title) }
                    }
                }
            }
        }
    }

    // Site Footer
    Footer {
        P { Text("© 2024 Benchmarks") }
    }
}
