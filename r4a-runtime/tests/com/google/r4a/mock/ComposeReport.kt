package com.google.r4a.mock

fun ViewComposition.reportsTo(report: Report) {
    text(report.from)
    text("reports to")
    text(report.to)
}
fun ViewValidator.reportsTo(report: Report) {
    text(report.from)
    text("reports to")
    text(report.to)
}


fun ViewComposition.reportsReport(reports: Iterable<Report>) {
    linear {
        repeat(of = reports) { report ->
            reportsTo(report)
        }
    }
}

fun ViewValidator.reportsReport(reports: Iterable<Report>) {
    linear {
        repeat(of = reports) { report ->
            reportsTo(report)
        }
    }
}

