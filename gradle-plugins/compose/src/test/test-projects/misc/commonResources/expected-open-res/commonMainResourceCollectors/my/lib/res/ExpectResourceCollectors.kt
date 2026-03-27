package my.lib.res

import kotlin.String
import kotlin.collections.Map
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource

public expect val MyRes.allDrawableResources: Map<String, DrawableResource>

public expect val MyRes.allStringResources: Map<String, StringResource>

public expect val MyRes.allStringArrayResources: Map<String, StringArrayResource>

public expect val MyRes.allPluralStringResources: Map<String, PluralStringResource>

public expect val MyRes.allFontResources: Map<String, FontResource>
