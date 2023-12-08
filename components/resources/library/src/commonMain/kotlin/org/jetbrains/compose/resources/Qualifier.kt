package org.jetbrains.compose.resources

interface Qualifier

data class LanguageQualifier(
    val language: String
) : Qualifier {
    companion object {
        val regex = Regex("[a-z][a-z]")
    }
}

data class RegionQualifier(
    val region: String
) : Qualifier {
    companion object {
        val regex = Regex("r[A-Z][A-Z]")
    }
}

enum class ThemeQualifier(val code: String) : Qualifier {
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun selectByValue(isDark: Boolean) =
            if (isDark) DARK else LIGHT
    }
}

//https://developer.android.com/guide/topics/resources/providing-resources
enum class DensityQualifier(val code: String, val dpi: Int) : Qualifier {
    LDPI("ldpi", 120),
    MDPI("mdpi", 160),
    HDPI("hdpi", 240),
    XHDPI("xhdpi", 320),
    XXHDPI("xxhdpi", 480),
    XXXHDPI("xxxhdpi", 640);

    companion object {
        fun selectByValue(dpi: Int) = when {
            dpi <= LDPI.dpi -> LDPI
            dpi <= MDPI.dpi -> MDPI
            dpi <= HDPI.dpi -> HDPI
            dpi <= XHDPI.dpi -> XHDPI
            dpi <= XXHDPI.dpi -> XXHDPI
            else -> XXXHDPI
        }
        fun selectByDensity(density: Float) = when {
            density <= 0.75 -> LDPI
            density <= 1.0 -> MDPI
            density <= 1.33 -> HDPI
            density <= 2.0 -> XHDPI
            density <= 3.0 -> XXHDPI
            else -> XXXHDPI
        }
    }
}

//TODO: move it to the gradle plugin
internal fun List<String>.parseQualifiers(): List<Qualifier> {
    var language: LanguageQualifier? = null
    var region: RegionQualifier? = null
    var theme: ThemeQualifier? = null
    var density: DensityQualifier? = null

    this.forEach { q ->
        if (density == null) {
            DensityQualifier.entries.firstOrNull { it.code == q }?.let {
                density = it
                return@forEach
            }
        }
        if (theme == null) {
            ThemeQualifier.entries.firstOrNull { it.code == q }?.let {
                theme = it
                return@forEach
            }
        }
        if (language == null && q.matches(LanguageQualifier.regex)) {
            language = LanguageQualifier(q)
            return@forEach
        }
        if (region == null && q.matches(RegionQualifier.regex)) {
            region = RegionQualifier(q.takeLast(2))
            return@forEach
        }
    }

    return buildList {
        language?.let { add(it) }
        region?.let { add(it) }
        theme?.let { add(it) }
        density?.let { add(it) }
    }
}