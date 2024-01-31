@file:OptIn(
    org.jetbrains.compose.resources.InternalResourceApi::class,
    org.jetbrains.compose.resources.ExperimentalResourceApi::class,
)

package app.group.resources_test.generated.resources

import kotlin.ByteArray
import kotlin.OptIn
import kotlin.String
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.readResourceBytes

@ExperimentalResourceApi
internal object Res {
    /**
     * Reads the content of the resource file at the specified path and returns it as a byte array.
     *
     * Example: `val bytes = Res.readBytes("files/key.bin")`
     *
     * @param path The path of the file to read in the compose resource's directory.
     * @return The content of the file as a byte array.
     */
    public suspend fun readBytes(path: String): ByteArray = readResourceBytes(path)

    public object drawable {
        public val _3_strange_name: DrawableResource = get__3_strange_name()

        public val vector: DrawableResource = get_vector()

        public val vector_2: DrawableResource = get_vector_2()
    }

    public object string {
        public val app_name: StringResource = get_app_name()

        public val hello: StringResource = get_hello()

        public val multi_line: StringResource = get_multi_line()

        public val str_arr: StringResource = get_str_arr()

        public val str_template: StringResource = get_str_template()
    }

    public object font {
        public val emptyfont: FontResource = get_emptyfont()
    }
}

private fun get__3_strange_name(): DrawableResource =
    org.jetbrains.compose.resources.DrawableResource(
        "drawable:_3_strange_name",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/3-strange-name.xml"),
        )
    )

private fun get_vector(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
    "drawable:vector",
    setOf(

        org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.LanguageQualifier("au"),
            org.jetbrains.compose.resources.RegionQualifier("US"), ), "drawable-au-rUS/vector.xml"),

        org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.ThemeQualifier.DARK,
            org.jetbrains.compose.resources.LanguageQualifier("ge"), ), "drawable-dark-ge/vector.xml"),

        org.jetbrains.compose.resources.ResourceItem(setOf(org.jetbrains.compose.resources.LanguageQualifier("en"),
        ), "drawable-en/vector.xml"),
        org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/vector.xml"),
    )
)

private fun get_vector_2(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
    "drawable:vector_2",
    setOf(
        org.jetbrains.compose.resources.ResourceItem(setOf(), "drawable/vector_2.xml"),
    )
)

private fun get_app_name(): StringResource = org.jetbrains.compose.resources.StringResource(
    "string:app_name", "app_name",
    setOf(
        org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
    )
)

private fun get_hello(): StringResource = org.jetbrains.compose.resources.StringResource(
    "string:hello", "hello",
    setOf(
        org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
    )
)

private fun get_multi_line(): StringResource = org.jetbrains.compose.resources.StringResource(
    "string:multi_line", "multi_line",
    setOf(
        org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
    )
)

private fun get_str_arr(): StringResource = org.jetbrains.compose.resources.StringResource(
    "string:str_arr", "str_arr",
    setOf(
        org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
    )
)

private fun get_str_template(): StringResource = org.jetbrains.compose.resources.StringResource(
    "string:str_template", "str_template",
    setOf(
        org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.xml"),
    )
)

private fun get_emptyfont(): FontResource = org.jetbrains.compose.resources.FontResource(
    "font:emptyfont",
    setOf(
        org.jetbrains.compose.resources.ResourceItem(setOf(), "font/emptyFont.otf"),
    )
)