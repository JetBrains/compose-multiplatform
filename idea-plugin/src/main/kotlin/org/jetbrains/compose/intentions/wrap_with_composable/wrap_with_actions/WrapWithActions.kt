package org.jetbrains.compose.intentions.wrap_with_composable.wrap_with_actions

import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TemplateSettings

class WrapWithBoxIntention : BaseWrapWithComposableAction() {

    override fun getText(): String {
        return "Wrap with Box"
    }

    override fun getTemplate(): TemplateImpl? {
        return TemplateSettings.getInstance().getTemplate("WwB", "ComposeMultiplatformTemplates")
    }
}

class WrapWithCardIntention : BaseWrapWithComposableAction() {

    override fun getText(): String {
        return "Wrap with Card"
    }

    override fun getTemplate(): TemplateImpl? {
        return TemplateSettings.getInstance().getTemplate("WwC", "ComposeMultiplatformTemplates")
    }
}

class WrapWithColumnIntention : BaseWrapWithComposableAction() {

    override fun getText(): String {
        return "Wrap with Column"
    }

    override fun getTemplate(): TemplateImpl? {
        return TemplateSettings.getInstance().getTemplate("WwCol", "ComposeMultiplatformTemplates")
    }
}

class WrapWithRowIntention : BaseWrapWithComposableAction() {

    override fun getText(): String {
        return "Wrap with Row"
    }

    override fun getTemplate(): TemplateImpl? {
        return TemplateSettings.getInstance().getTemplate("WwRow", "ComposeMultiplatformTemplates")
    }
}

class WrapWithLzyColumnIntention : BaseWrapWithComposableAction() {

    override fun getText(): String {
        return "Wrap with LazyColumn"
    }

    override fun getTemplate(): TemplateImpl? {
        return TemplateSettings.getInstance().getTemplate("WwLazyCol", "ComposeMultiplatformTemplates")
    }
}

class WrapWithLzyRowIntention : BaseWrapWithComposableAction() {

    override fun getText(): String {
        return "Wrap with LazyRow"
    }

    override fun getTemplate(): TemplateImpl? {
        return TemplateSettings.getInstance().getTemplate("WwLazyRow", "ComposeMultiplatformTemplates")
    }
}
