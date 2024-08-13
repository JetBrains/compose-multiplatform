package org.jetbrains.compose.resources

interface Qualifier

@InternalResourceApi
class LanguageQualifier(
    val language: String
) : Qualifier {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LanguageQualifier

        return language == other.language
    }

    override fun hashCode(): Int {
        return language.hashCode()
    }

    override fun toString(): String {
        return "LanguageQualifier(language='$language')"
    }
}

@InternalResourceApi
class RegionQualifier(
    val region: String
) : Qualifier {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RegionQualifier

        return region == other.region
    }

    override fun hashCode(): Int {
        return region.hashCode()
    }

    override fun toString(): String {
        return "RegionQualifier(region='$region')"
    }
}

@InternalResourceApi
enum class ThemeQualifier : Qualifier {
    LIGHT,
    DARK;

    companion object {
        fun selectByValue(isDark: Boolean) =
            if (isDark) DARK else LIGHT
    }
}

//https://developer.android.com/guide/topics/resources/providing-resources
@InternalResourceApi
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
