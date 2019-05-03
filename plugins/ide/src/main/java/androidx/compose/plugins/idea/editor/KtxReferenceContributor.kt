package androidx.compose.plugins.idea.editor

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.references.AbstractKotlinReferenceContributor
import org.jetbrains.kotlin.idea.references.KtMultiReference
import org.jetbrains.kotlin.idea.references.KtSimpleReference
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.kotlin.ComposeUtils
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.plugins.kotlin.EmitCallNode
import androidx.compose.plugins.kotlin.EmitOrCallNode
import androidx.compose.plugins.kotlin.ErrorNode
import androidx.compose.plugins.kotlin.MemoizedCallNode
import androidx.compose.plugins.kotlin.NonMemoizedCallNode
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.util.OperatorNameConventions

class KtxReferenceContributor : AbstractKotlinReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // We want to search for references on the closest ancestor of the element itself that we are putting a
        // reference on. This ends up being KtSimpleNameExpression for most things, which this provider deals with.
        // The reason we need to do it this way is because the reference handling is pretty broken in Kotlin, and the
        // "element" that the IDE ends up using to do the highlighting and surfacing of the references is always the
        // *last* element that the algorithm finds as it traverses up the spine of the tree. The way we are
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(KtSimpleNameExpression::class.java),
            KtxReferenceProvider
        )
        // We are hanging references on the LT bracket of the KTX element itself. To do this, we need to target the
        // KTX element overall, though I think this would work even better if we could target the token directly, but I
        // can't figure out how to do that and it doesn't look like that's done anywhere else.
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(KtxElement::class.java),
            KtxSyntaxReferenceProvider
        )
    }
}

object KtxSyntaxReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference> {
        return when (element) {
            is KtxElement -> arrayOf(KtxSyntaxReference(element))
            else -> PsiReference.EMPTY_ARRAY
        }
    }
}

/**
 * Because we are targeting SimpleNameExpression instead of KtxElement (because things will conflict otherwise), we have to run a
 * bit of logic to ensure that the simple name expression we have is one that we want to target in the KTX tag.
 *
 * The reason we have to do this runaround logic is because the way references currently work is that it calls `getReferencesByElement()`
 * as it travels up the spine of the tree from the leaf element that the cursor is on top of. When it is done, the IDE will highlight
 * and surface the references on the element that is shown by the *last* reference in the list that it finds as it traverses. This means
 * that using the `KtxElement` as the root is too high because it will be the "last" element for everything that gets run below it,
 * including nested KTX tags where this ends up getting really bad.  As a result, our strategy is to have `getReferencesByElement` run
 * on as small of a node as practical, even if it needs to look at it's parents and has the `ref.element` of the  reference it returns
 * be on a bigger element.
 *
 * This combination of things seems to work, but overall the reference system in IntelliJ/Kotlin is *really* fragile, so be really
 * careful when modifying this code.
 */
object KtxReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference> {
        if (element !is KtSimpleNameExpression) return PsiReference.EMPTY_ARRAY
        val parent = element.parent
        when (parent) {
            is KtxElement -> {
                if (element == parent.simpleTagName) {
                    // it is a simple open tag. include the syntax reference
                    return arrayOf<PsiReference>(
                        KtxSyntaxReferenceForImports(parent),
                        KtxReference(parent, element)
                    )
                } else if (element == parent.simpleClosingTagName) {
                    // it is a simple closing tag
                    return arrayOf<PsiReference>(
                        KtxReference(
                            parent,
                            element
                        )
                    )
                }
            }
            is KtxAttribute -> {
                if (parent.key == element) {
                    // attribute key
                    return arrayOf<PsiReference>(
                        AttributeReference(
                            parent
                        )
                    )
                }
            }
            is KtDotQualifiedExpression -> {
                val superparent = parent.parent
                if (superparent is KtxElement) {
                    val openFirstPart =
                        firstNameForElement(superparent)
                    if (openFirstPart == element) {
                        // the first part of the open tag. we add a syntax reference here since it needs to have no receiver
                        // in order to get used in the "unused imports" algorithm
                        return arrayOf<PsiReference>(
                            KtxSyntaxReferenceForImports(
                                superparent
                            )
                        )
                    } else {
                        val openLastPart =
                            lastNameForElement(superparent)
                        val closingLastPart =
                            superparent.qualifiedClosingTagName?.lastSimpleNameExpression()
                        if (closingLastPart == element || openLastPart == element) {
                            // if it's the last part of a dot qualified expression, on either the closing or open tag,
                            // we go ahead and add our ktx reference
                            return arrayOf<PsiReference>(
                                KtxReference(
                                    superparent,
                                    element
                                )
                            )
                        }
                    }
                }
            }
        }
        return PsiReference.EMPTY_ARRAY
    }
}

open class KtxSyntaxReference(
    val ktxElement: KtxElement
) : KtMultiReference<KtElement>(ktxElement) {

    override fun getRangeInElement() = TextRange(0, 1)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val resolvedElementCall = context[ComposeWritableSlices.RESOLVED_KTX_CALL, ktxElement]
            ?: return emptyList()

        val results = mutableListOf<DeclarationDescriptor>()

        resolvedElementCall.getComposerCall?.resultingDescriptor?.let { results.add(it) }

        var node: EmitOrCallNode? = resolvedElementCall.emitOrCall

        while (node != null) {
            node = when (node) {
                is EmitCallNode -> {
                    node.memoize.composerCall?.resultingDescriptor?.let { results.add(it) }
                    null
                }
                is MemoizedCallNode -> {
                    node.memoize.composerCall?.resultingDescriptor?.let { results.add(it) }
                    node.call
                }
                is NonMemoizedCallNode -> node.nextCall
                is ErrorNode -> null
            }
        }

        return results
    }

    override val resolvesByNames: Collection<Name>
        get() = COMPOSER_NAMES
}

class KtxSyntaxReferenceForImports(ktxElement: KtxElement) : KtxSyntaxReference(ktxElement) {
    // NOTE(lmr): It's important that this return a KtNameReferenceElement or else this reference will not get used in the
    // Unused Import detection! Moreover, we want it to return a name expression that does *not* have a receiver to the left of
    // it, or else it will also get excluded. In this case we just return the left-most SimpleNameExpression of the KtxElement.
    override fun getElement() = firstNameForElement(ktxElement)

    override fun getRangeInElement() = TextRange.EMPTY_RANGE
}

/**
 * This is needed because sometimes attributes of KTX tags target a setter function with a different name than
 * the "attribute" name, which means that the extension setter won't show up as properly "used". This fixes that.
 *
 * In the long-term, I think we should not need this, since we should probably just target properties in kotlin and not setters
 */
class AttributeReference(
    private var attribute: KtxAttribute
) : KtSimpleReference<KtSimpleNameExpression>(attribute.key) {

    override fun getRangeInElement() = TextRange(0, attribute.key.textLength)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        return context[ComposeWritableSlices.ATTRIBUTE_KEY_REFERENCE_TARGET, attribute.key]
            ?: listOfNotNull(context[BindingContext.REFERENCE_TARGET, attribute.key])
    }

    override val resolvesByNames: Collection<Name>
        get() = attribute.key.getReferencedNameAsName().let {
            listOf(
                it,
                Name.identifier(ComposeUtils.setterMethodFromPropertyName(it.identifier))
            )
        }
}

class KtxReference(
    val ktxElement: KtxElement,
    val nameElement: KtSimpleNameExpression
) : KtMultiReference<KtElement>(nameElement) {

    override fun getRangeInElement() = TextRange(0, nameElement.textLength)

    private fun primaryTargetOf(resolvedCall: ResolvedCall<*>?): List<DeclarationDescriptor> {
        return when (resolvedCall) {
            is VariableAsFunctionResolvedCall ->
                listOf(resolvedCall.variableCall.resultingDescriptor)
            null -> emptyList()
            else -> listOf(resolvedCall.resultingDescriptor)
        }
    }

    private fun primaryTargetsOf(node: EmitOrCallNode?): List<DeclarationDescriptor> {
        return when (node) {
            is EmitCallNode -> primaryTargetOf(node.memoize.ctorCall)
            is MemoizedCallNode ->
                primaryTargetOf(node.memoize.ctorCall) + primaryTargetsOf(node.call)
            is NonMemoizedCallNode ->
                primaryTargetOf(node.resolvedCall) + primaryTargetsOf(node.nextCall)
            is ErrorNode -> emptyList()
            null -> emptyList()
        }
    }

    private fun filterResultsIfInSameContainer(
        targets: List<DeclarationDescriptor>
    ): List<DeclarationDescriptor> {
        if (targets.size <= 1) return targets
        var node = targets.first()
        val results = mutableListOf(node)
        for (target in targets.drop(1)) {
            val prev = node
            node = target

            val container = target.containingDeclaration
            if (container == prev) continue
            if (prev is ClassConstructorDescriptor && prev.containingDeclaration == container)
                continue

            results.add(target)
        }
        return results
    }

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val resolvedElementCall = context[ComposeWritableSlices.RESOLVED_KTX_CALL, ktxElement]
            ?: return emptyList()
        val results = primaryTargetsOf(resolvedElementCall.emitOrCall)

        return filterResultsIfInSameContainer(results)
    }

    override val resolvesByNames: Collection<Name>
        get() = listOf(
            nameElement.getReferencedNameAsName(),
            OperatorNameConventions.INVOKE
        )
}

private val COMPOSER_NAMES = listOf(
    KtxNameConventions.COMPOSER,
    KtxNameConventions.EMIT,
    KtxNameConventions.CALL
)

private fun firstNameForElement(element: KtxElement): KtSimpleNameExpression {
    return element.simpleTagName
        ?: element.qualifiedTagName?.firstSimpleNameExpression()
        ?: error("Expected a simple name expression")
}

private fun KtExpression.firstSimpleNameExpression(): KtSimpleNameExpression? = when (this) {
    is KtSimpleNameExpression -> this
    is KtDotQualifiedExpression ->
        receiverExpression.firstSimpleNameExpression()
            ?: selectorExpression?.firstSimpleNameExpression()
    else -> null
}

private fun lastNameForElement(element: KtxElement): KtSimpleNameExpression {
    return element.simpleTagName
        ?: element.qualifiedTagName?.lastSimpleNameExpression()
        ?: error("Expected a simple name expression")
}

private fun KtExpression.lastSimpleNameExpression(): KtSimpleNameExpression? = when (this) {
    is KtSimpleNameExpression -> this
    is KtDotQualifiedExpression ->
        selectorExpression?.lastSimpleNameExpression()
            ?: receiverExpression.lastSimpleNameExpression()
    else -> null
}