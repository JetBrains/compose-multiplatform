/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.ExperimentalComposeWebSvgApi::class)

package benchmarks.compose

import benchmarks.model.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.svg.*
import androidx.compose.runtime.Composable

@Composable
fun SvgDashboardDocument(data: SvgDashboardData) {
    Html(attrs = { lang("en") }) {
        Head {
            Meta(attrs = { attr("charset", "UTF-8") })
            Title { Text(data.title) }
        }
        Body {
            SvgDashboard(data)
        }
    }
}

@Composable
fun SvgDashboard(data: SvgDashboardData) {
    Div(attrs = { classes("dashboard-container") }) {
        Header(attrs = { classes("dashboard-header") }) {
            H1 { Text(data.title) }
        }

        Div(attrs = { classes("charts-grid") }) {
            // Line Chart
            Div(attrs = {
                classes("chart-wrapper")
                attr("aria-label", "Line chart showing ${data.lineChart.title}")
                attr("role", "img")
            }) {
                H2 { Text(data.lineChart.title) }
                Svg(viewBox = "0 0 800 400", attrs = {
                    attr("width", "100%")
                    attr("height", "100%")
                }) {
                    // Axes
                    Line(x1 = 50, y1 = 350, x2 = 750, y2 = 350, attrs = {
                        attr("stroke", "#000")
                        attr("stroke-width", "2")
                    })
                    Line(x1 = 50, y1 = 50, x2 = 50, y2 = 350, attrs = {
                        attr("stroke", "#000")
                        attr("stroke-width", "2")
                    })

                    // Data Path
                    Path(d = data.lineChart.pathData, attrs = {
                        attr("fill", "none")
                        attr("stroke", "#3b82f6")
                        attr("stroke-width", "3")
                    })

                    // Points
                    data.lineChart.points.forEach { point ->
                        Circle(cx = point.x, cy = point.y, r = 4, attrs = {
                            attr("fill", "#1d4ed8")
                        })
                    }

                    // Labels
                    data.lineChart.xLabels.forEach { label ->
                        SvgText(text = label.text, x = label.x, y = 370, attrs = {
                            attr("text-anchor", "middle")
                            attr("font-size", "12")
                        })
                    }
                }
            }

            // Bar Chart
            Div(attrs = {
                classes("chart-wrapper")
                attr("aria-label", "Bar chart showing ${data.barChart.title}")
                attr("role", "img")
            }) {
                H2 { Text(data.barChart.title) }
                Svg(viewBox = "0 0 600 400") {
                    Defs {
                        LinearGradient(id = "bar-grad", attrs = {
                            attr("x1", "0%")
                            attr("y1", "100%")
                            attr("x2", "0%")
                            attr("y2", "0%")
                        }) {
                            Stop(attrs = {
                                attr("offset", "0%")
                                attr("stop-color", "#34d399")
                            })
                            Stop(attrs = {
                                attr("offset", "100%")
                                attr("stop-color", "#059669")
                            })
                        }
                    }

                    data.barChart.bars.forEach { bar ->
                        Rect(x = bar.x, y = bar.y, width = bar.width, height = bar.height, attrs = {
                            attr("fill", "url(#bar-grad)")
                        })
                        SvgText(text = bar.value.toString().removeSuffix(".0"), x = bar.x + bar.width / 2, y = bar.y - 10, attrs = {
                            attr("text-anchor", "middle")
                            attr("font-size", "12")
                        })
                    }
                }
            }

            // Pie Chart
            Div(attrs = {
                classes("chart-wrapper")
                attr("aria-label", "Pie chart showing ${data.pieChart.title}")
                attr("role", "img")
            }) {
                H2 { Text(data.pieChart.title) }
                Svg(viewBox = "0 0 400 400") {
                    G(attrs = { attr("transform", "translate(200, 200)") }) {
                        data.pieChart.slices.forEach { slice ->
                            Path(d = slice.pathData, attrs = {
                                attr("fill", slice.color)
                                attr("stroke", "#fff")
                                attr("stroke-width", "2")
                            })
                        }
                    }
                }
            }

            // Gauge Meter
            Div(attrs = {
                classes("chart-wrapper")
                attr("aria-label", "Gauge showing ${data.gauge.title}")
                attr("role", "img")
            }) {
                H2 { Text(data.gauge.title) }
                Svg(viewBox = "0 0 200 150") {
                    Path(d = "M 20 130 A 80 80 0 0 1 180 130", attrs = {
                        attr("fill", "none")
                        attr("stroke", "#e5e7eb")
                        attr("stroke-width", "20")
                    })
                    Path(d = data.gauge.valuePathData, attrs = {
                        attr("fill", "none")
                        attr("stroke", "#ef4444")
                        attr("stroke-width", "20")
                    })
                    // Needle
                    Line(x1 = 100, y1 = 130, x2 = data.gauge.needleX, y2 = data.gauge.needleY, attrs = {
                        attr("stroke", "#374151")
                        attr("stroke-width", "4")
                    })
                    Circle(cx = 100, cy = 130, r = 8, attrs = {
                        attr("fill", "#374151")
                    })
                    SvgText(text = data.gauge.valueLabel, x = 100, y = 145, attrs = {
                        attr("text-anchor", "middle")
                        attr("font-size", "14")
                        attr("dominant-baseline", "hanging")
                    })
                }
            }
        }
    }
}
