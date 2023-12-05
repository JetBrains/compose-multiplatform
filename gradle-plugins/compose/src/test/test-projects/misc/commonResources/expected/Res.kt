package app.group.generated.resources

import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.ImageResource
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.StringResource

internal object Res {
    public object fonts {
        public val emptyfont: FontResource = FontResource(
            "FONT:emptyfont",
            setOf(
                ResourceItem(setOf(), "composeRes/fonts/emptyFont.otf"),
            )
        )
    }

    public object images {
        public val _3_strange_name: ImageResource = ImageResource(
            "IMAGE:_3_strange_name",
            setOf(
                ResourceItem(setOf(), "composeRes/images/3-strange-name.xml"),
            )
        )

        public val vector: ImageResource = ImageResource(
            "IMAGE:vector",
            setOf(
                ResourceItem(setOf("q1", "q2"), "composeRes/images-q1-q2/vector.xml"),
                ResourceItem(setOf("q1"), "composeRes/images-q1/vector.xml"),
                ResourceItem(setOf("q2"), "composeRes/images-q2/vector.xml"),
                ResourceItem(setOf(), "composeRes/images/vector.xml"),
            )
        )

        public val vector_2: ImageResource = ImageResource(
            "IMAGE:vector_2",
            setOf(
                ResourceItem(setOf(), "composeRes/images/vector_2.xml"),
            )
        )
    }

    public object strings {
        public val app_name: StringResource = StringResource(
            "STRING:app_name",
            "app_name",
            setOf(
                ResourceItem(setOf(), "composeRes/values/strings.xml"),
            )
        )

        public val hello: StringResource = StringResource(
            "STRING:hello",
            "hello",
            setOf(
                ResourceItem(setOf(), "composeRes/values/strings.xml"),
            )
        )

        public val multi_line: StringResource = StringResource(
            "STRING:multi_line",
            "multi_line",
            setOf(
                ResourceItem(setOf(), "composeRes/values/strings.xml"),
            )
        )

        public val str_arr: StringResource = StringResource(
            "STRING:str_arr",
            "str_arr",
            setOf(
                ResourceItem(setOf(), "composeRes/values/strings.xml"),
            )
        )

        public val str_template: StringResource = StringResource(
            "STRING:str_template",
            "str_template",
            setOf(
                ResourceItem(setOf(), "composeRes/values/strings.xml"),
            )
        )
    }
}