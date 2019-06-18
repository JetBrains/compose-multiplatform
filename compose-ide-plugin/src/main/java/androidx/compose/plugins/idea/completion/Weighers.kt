package androidx.compose.plugins.idea.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.codeInsight.lookup.WeighingContext
import com.intellij.openapi.util.Key
import androidx.compose.plugins.idea.AttributeInfo

object Weighers {
    val IS_IMPORTED_KEY = Key<Boolean>("compose.is_imported")
    val ATTRIBUTE_INFO_KEY = Key<AttributeInfo>("compose.attribute_info")

    val IsImported = object : LookupElementWeigher("compose.importedWeigher") {
        override fun weigh(element: LookupElement, context: WeighingContext) =
            if (element.getUserData(IS_IMPORTED_KEY) == true) -1 else 0
    }

    abstract class AttributeInfoWeigher(key: String) : LookupElementWeigher(key) {
        override fun weigh(element: LookupElement, context: WeighingContext) =
            element.getUserData(ATTRIBUTE_INFO_KEY)?.let { weigh(it) } ?: 0
        abstract fun weigh(info: AttributeInfo): Int
    }

    val IsImmediateAttribute = object : AttributeInfoWeigher("compose.isImmediateAttributeWeigher") {
        override fun weigh(info: AttributeInfo): Int = if (info.isImmediate) -1 else 0
    }
    val IsRequiredAttribute = object : AttributeInfoWeigher("compose.isRequiredAttributeWeigher") {
        override fun weigh(info: AttributeInfo): Int = if (info.isRequired) -1 else 0
    }

    val IsChildAttribute = object : AttributeInfoWeigher("compose.isChildAttributeWeigher") {
        override fun weigh(info: AttributeInfo): Int = if (info.isChildren) 1 else 0
    }
}