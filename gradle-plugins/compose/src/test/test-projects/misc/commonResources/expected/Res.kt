package app.group.generated.resources

import kotlin.ByteArray
import kotlin.OptIn
import kotlin.String
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.ThemeQualifier
import org.jetbrains.compose.resources.readResourceBytes

internal object Res {
    @OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)
    public suspend fun readFileBytes(path: String): ByteArray =
        readResourceBytes("composeResources/" + path)

    public object drawable {
        public val _3_strange_name: DrawableResource = DrawableResource(
            "drawable:_3_strange_name",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeResources/drawable/3-strange-name.xml"
                ),
            )
        )

        public val vector: DrawableResource = DrawableResource(
            "drawable:vector",
            setOf(
                ResourceItem(
                    setOf(LanguageQualifier("au"), RegionQualifier("US"), ),
                    "composeResources/drawable-au-rUS/vector.xml"
                ),
                ResourceItem(
                    setOf(ThemeQualifier.DARK, LanguageQualifier("ge"), ),
                    "composeResources/drawable-dark-ge/vector.xml"
                ),
                ResourceItem(
                    setOf(LanguageQualifier("en"), ),
                    "composeResources/drawable-en/vector.xml"
                ),
                ResourceItem(
                    setOf(),
                    "composeResources/drawable/vector.xml"
                ),
            )
        )

        public val vector_2: DrawableResource = DrawableResource(
            "drawable:vector_2",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeResources/drawable/vector_2.xml"
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
                    "composeResources/font/emptyFont.otf"
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
                    "composeResources/values/strings.xml"
                ),
            )
        )

        public val hello: StringResource = StringResource(
            "string:hello",
            "hello",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeResources/values/strings.xml"
                ),
            )
        )

        public val multi_line: StringResource = StringResource(
            "string:multi_line",
            "multi_line",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeResources/values/strings.xml"
                ),
            )
        )

        public val str_arr: StringResource = StringResource(
            "string:str_arr",
            "str_arr",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeResources/values/strings.xml"
                ),
            )
        )

        public val str_template: StringResource = StringResource(
            "string:str_template",
            "str_template",
            setOf(
                ResourceItem(
                    setOf(),
                    "composeResources/values/strings.xml"
                ),
            )
        )
    }
}