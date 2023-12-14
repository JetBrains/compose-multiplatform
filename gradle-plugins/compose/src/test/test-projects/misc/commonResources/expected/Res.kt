package app.group.generated.resources

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.ThemeQualifier

internal object Res {
    public object drawable {
        public val _3_strange_name: DrawableResource = DrawableResource(
            "drawable:_3_strange_name",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeRes/drawable/3-strange-name.xml"
                ),
            )
        )

        public val vector: DrawableResource = DrawableResource(
            "drawable:vector",
            setOf(
                ResourceItem(
                    setOf(ThemeQualifier.DARK, LanguageQualifier("ge"), ),
                    "composeRes/drawable-dark-ge/vector.xml"
                ),
                ResourceItem(
                    setOf(LanguageQualifier("en"), ),
                    "composeRes/drawable-en/vector.xml"
                ),
                ResourceItem(
                    setOf(RegionQualifier("US"), ),
                    "composeRes/drawable-rUS/vector.xml"
                ),
                ResourceItem(
                    setOf(),
                    "composeRes/drawable/vector.xml"
                ),
            )
        )

        public val vector_2: DrawableResource = DrawableResource(
            "drawable:vector_2",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeRes/drawable/vector_2.xml"
                ),
            )
        )
    }

    public object font {
        public val emptyfont: FontResource = FontResource(
            "font:emptyfont",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeRes/font/emptyFont.otf"
                ),
            )
        )
    }

    public object string {
        public val app_name: StringResource = StringResource(
            "string:app_name",
            "app_name",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeRes/values/strings.xml"
                ),
            )
        )

        public val hello: StringResource = StringResource(
            "string:hello",
            "hello",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeRes/values/strings.xml"
                ),
            )
        )

        public val multi_line: StringResource = StringResource(
            "string:multi_line",
            "multi_line",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeRes/values/strings.xml"
                ),
            )
        )

        public val str_arr: StringResource = StringResource(
            "string:str_arr",
            "str_arr",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeRes/values/strings.xml"
                ),
            )
        )

        public val str_template: StringResource = StringResource(
            "string:str_template",
            "str_template",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeRes/values/strings.xml"
                ),
            )
        )
    }
}