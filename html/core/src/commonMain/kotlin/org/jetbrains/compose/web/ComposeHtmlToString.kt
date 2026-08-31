package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.web.dom.LocalComposeHtmlContext
import org.jetbrains.compose.web.dom.StringComposeHtmlContext
import org.jetbrains.compose.web.dom.StringHtmlApplier
import org.jetbrains.compose.web.dom.StringHtmlElementNode
import org.jetbrains.compose.web.dom.StringHtmlNodeWrapper

/**
 * Composes [content] using [data] and returns both its HTML and an inert serialized-data element from
 * which the client can recover the same value before hydration.
 *
 * [data] must be treated as an immutable snapshot: it is serialized before [content] is composed.
 * [serializeData] may use any textual format as long as the matching client
 * `hydrateComposable` call can deserialize it. This function safely embeds the returned string in
 * HTML without interpreting or validating its format.
 *
 * Everything returned by [serializeData] is public, readable in the page source, and editable by
 * the client. Always map server models to a dedicated public DTO first. Never pass domain objects,
 * credentials, or sensitive fields.
 *
 * ```
 * val publicData = PublicPageData(title = serverPage.title)
 * composeHtmlToString(publicData, PublicPageData::serialize) { data ->
 *     Page(data)
 * }
 * ```
 *
 * The returned [HydratableHtml.hydrationDataElement] must be emitted outside the hydration root.
 * The data can occur twice in the response: represented in the rendered HTML and in its serialized
 * form for the initial client composition.
 *
 * @throws IllegalArgumentException if [hydrationDataId] is not a supported hydration data id.
 */
fun <T> composeHtmlToString(
    data: T,
    serializeData: (T) -> String,
    hydrationDataId: String = DEFAULT_HYDRATION_DATA_ID,
    content: @Composable (T) -> Unit,
): HydratableHtml {
    requireValidHydrationDataId(hydrationDataId)
    val serializedData = serializeData(data)
    val html = composeHtmlToString(hydratable = true) {
        content(data)
    }

    return HydratableHtml(
        content = html,
        hydrationDataElement = hydrationDataElement(hydrationDataId, serializedData),
    )
}

/**
 * Composes [content] once into an HTML string without creating browser DOM nodes.
 * The backing composition is disposed after the initial HTML has been serialized.
 * The result can contain internal comments that preserve ambiguous text-node boundaries for
 * hydration. They have no effect on the rendered content.
 *
 * Set [hydratable] to `false` for static output without hydration boundary comments.
 *
 * Known limitations:
 * - DOM property updates registered with `AttrsScope.prop(...)` are ignored because
 *   string rendering has no underlying DOM element.
 */
fun composeHtmlToString(
    hydratable: Boolean = true,
    content: @Composable () -> Unit,
): String {
    val root = StringHtmlElementNode.root()
    val recomposer = Recomposer(Dispatchers.Default)
    val composition = ControlledComposition(
        applier = StringHtmlApplier(StringHtmlNodeWrapper(root)),
        parent = recomposer,
    )

    return try {
        composition.setContent {
            CompositionLocalProvider(
                LocalComposeHtmlContext provides StringComposeHtmlContext
            ) {
                content()
            }
        }
        root.toHtmlString(hydratable)
    } finally {
        composition.dispose()
        recomposer.close()
    }
}
