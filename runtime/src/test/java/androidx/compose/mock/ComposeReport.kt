package androidx.compose.mock

fun MockViewComposition.reportsTo(report: Report) {
    text(report.from)
    text("reports to")
    text(report.to)
}
fun MockViewValidator.reportsTo(report: Report) {
    text(report.from)
    text("reports to")
    text(report.to)
}

fun MockViewComposition.reportsReport(reports: Iterable<Report>) {
    linear {
        repeat(of = reports) { report ->
            reportsTo(report)
        }
    }
}

fun MockViewValidator.reportsReport(reports: Iterable<Report>) {
    linear {
        repeat(of = reports) { report ->
            reportsTo(report)
        }
    }
}
