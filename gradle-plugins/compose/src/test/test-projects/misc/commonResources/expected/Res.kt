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
    /**
     * Reads the content of the resource file at the specified path and returns it as a byte array.
     *
     * Example: `val bytes = Res.readBytes("files/key.bin")`
     *
     * @param path The path of the file to read in the compose resource's directory.
     * @return The content of the file as a byte array.
     */
    @OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)
    public suspend fun readBytes(path: String): ByteArray = readResourceBytes(path)

    public object drawable {
        public val _3_strange_name: DrawableResource = DrawableResource(
            "drawable:_3_strange_name",
            setOf(
                ResourceItem(
                    setOf(),
                    "drawable/3-strange-name.xml"
                ),
            )
        )

        public val vector: DrawableResource = DrawableResource(
            "drawable:vector",
            setOf(
                ResourceItem(
                    setOf(LanguageQualifier("au"), RegionQualifier("US"), ),
                    "drawable-au-rUS/vector.xml"
                ),
                ResourceItem(
                    setOf(ThemeQualifier.DARK, LanguageQualifier("ge"), ),
                    "drawable-dark-ge/vector.xml"
                ),
                ResourceItem(
                    setOf(LanguageQualifier("en"), ),
                    "drawable-en/vector.xml"
                ),
                ResourceItem(
                    setOf(),
                    "drawable/vector.xml"
                ),
            )
        )

        public val vector_2: DrawableResource = DrawableResource(
            "drawable:vector_2",
            setOf(
                ResourceItem(
                    setOf(),
                    "drawable/vector_2.xml"
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
                    "font/emptyFont.otf"
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
                    "values/strings.xml"
                ),
            )
        )

        public val hello: StringResource = StringResource(
            "string:hello",
            "hello",
            setOf(
                ResourceItem(
                    setOf(),
                    "values/strings.xml"
                ),
            )
        )

        public val multi_line: StringResource = StringResource(
            "string:multi_line",
            "multi_line",
            setOf(
                ResourceItem(
                    setOf(),
                    "values/strings.xml"
                ),
            )
        )

        public val str_arr: StringResource = StringResource(
            "string:str_arr",
            "str_arr",
            setOf(
                ResourceItem(
                    setOf(),
                    "values/strings.xml"
                ),
            )
        )

        public val str_template: StringResource = StringResource(
            "string:str_template",
            "str_template",
            setOf(
                ResourceItem(
                    setOf(),
                    "values/strings.xml"
                ),
            )
        )
    }
}