package org.jetbrains.compose.resources

interface Qualifier

data class LanguageQualifier(
    val language: String
) : Qualifier

data class RegionQualifier(
    val region: String
) : Qualifier

enum class ThemeQualifier : Qualifier {
    LIGHT,
    DARK;

    companion object {
        fun selectByValue(isDark: Boolean) =
            if (isDark) DARK else LIGHT
    }
}

//https://developer.android.com/guide/topics/resources/providing-resources
enum class DensityQualifier(val dpi: Int) : Qualifier {
    LDPI(120),
    MDPI(160),
    HDPI(240),
    XHDPI(320),
    XXHDPI(480),
    XXXHDPI(640);

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
            density <= 1.5 -> HDPI
            density <= 2.0 -> XHDPI
            density <= 3.0 -> XXHDPI
            else -> XXXHDPI
        }
    }
}
