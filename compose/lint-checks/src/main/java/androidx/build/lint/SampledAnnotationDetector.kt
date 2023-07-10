/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import androidx.build.lint.SampledAnnotationDetector.Companion.INVALID_SAMPLES_LOCATION
import androidx.build.lint.SampledAnnotationDetector.Companion.MULTIPLE_FUNCTIONS_FOUND
import androidx.build.lint.SampledAnnotationDetector.Companion.OBSOLETE_SAMPLED_ANNOTATION
import androidx.build.lint.SampledAnnotationDetector.Companion.SAMPLED_ANNOTATION
import androidx.build.lint.SampledAnnotationDetector.Companion.SAMPLED_ANNOTATION_FQN
import androidx.build.lint.SampledAnnotationDetector.Companion.SAMPLED_FUNCTION_MAP
import androidx.build.lint.SampledAnnotationDetector.Companion.SAMPLES_DIRECTORY
import androidx.build.lint.SampledAnnotationDetector.Companion.SAMPLE_KDOC_ANNOTATION
import androidx.build.lint.SampledAnnotationDetector.Companion.SAMPLE_LINK_MAP
import androidx.build.lint.SampledAnnotationDetector.Companion.UNRESOLVED_SAMPLE_LINK
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintMap
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.PartialResult
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.kotlin.KotlinUastResolveProviderService

/**
 * Detector responsible for enforcing @Sampled annotation usage
 *
 * This detector enforces that:
 *
 * - Functions referenced with @sample are annotated with @Sampled - [UNRESOLVED_SAMPLE_LINK]
 * - Functions annotated with @Sampled are referenced with @sample - [OBSOLETE_SAMPLED_ANNOTATION]
 * - Functions annotated with @Sampled are inside a valid samples directory, matching module /
 * directory structure guidelines - [INVALID_SAMPLES_LOCATION]
 * - There are never multiple functions with the same fully qualified name that could be resolved
 * by an @sample link - [MULTIPLE_FUNCTIONS_FOUND]
 */
class SampledAnnotationDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UDeclaration::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitDeclaration(node: UDeclaration) {
            KDocSampleLinkHandler(context).visitDeclaration(node)
            if (node is UMethod) {
                SampledAnnotationHandler(context).visitMethod(node)
            }
        }
    }

    override fun checkPartialResults(context: Context, partialResults: PartialResult) {
        val sampleLinks = mutableMapOf<String, MutableList<Location>>()
        val sampledFunctions = mutableMapOf<String, MutableList<Location>>()
        partialResults.maps().forEach { map ->
            map.getMap(SAMPLE_LINK_MAP)?.run {
                iterator().forEach { key ->
                    sampleLinks.getOrPut(key) { mutableListOf() }.add(getLocation(key)!!)
                }
            }

            map.getMap(SAMPLED_FUNCTION_MAP)?.run {
                iterator().forEach { key ->
                    sampledFunctions.getOrPut(key) { mutableListOf() }.add(getLocation(key)!!)
                }
            }
        }

        // Only report errors on the sample module
        if (context.project.name != "samples") return

        /**
         * Returns whether this [Location] represents a file that we want to report errors for. We
         * only want to report an error for files in the parent module of this samples module, to
         * avoid reporting the same errors multiple times if multiple sample modules depend
         * on a library that has @sample links.
         */
        fun Location.shouldReport(): Boolean {
            // Path of the parent module that the sample module has samples for
            val sampleParentPath = context.project.dir.parentFile.toPath().toRealPath()
            val locationPath = file.toPath().toRealPath()
            return locationPath.startsWith(sampleParentPath)
        }

        sampleLinks.forEach { (link, locations) ->
            val functionLocations = sampledFunctions[link]
            when {
                functionLocations == null -> {
                    locations.forEach { location ->
                        if (location.shouldReport()) {
                            val incident = Incident(context)
                                .issue(UNRESOLVED_SAMPLE_LINK)
                                .location(location)
                                .message("Couldn't find a valid @Sampled function matching $link")
                            context.report(incident)
                        }
                    }
                }
                // This probably should never happen, but theoretically there could be multiple
                // samples with the same FQN across separate sample projects, so check here as well.
                functionLocations.size > 1 -> {
                    locations.forEach { location ->
                        if (location.shouldReport()) {
                            val incident = Incident(context)
                                .issue(MULTIPLE_FUNCTIONS_FOUND)
                                .location(location)
                                .message("Found multiple functions matching $link")
                            context.report(incident)
                        }
                    }
                }
            }
        }

        sampledFunctions.forEach { (link, locations) ->
            if (sampleLinks[link] == null) {
                locations.forEach { location ->
                    if (location.shouldReport()) {
                        val incident = Incident(context)
                            .issue(OBSOLETE_SAMPLED_ANNOTATION)
                            .location(location)
                            .message("$link is annotated with @$SAMPLED_ANNOTATION, but is not " +
                                "linked to from a @$SAMPLE_KDOC_ANNOTATION tag.")
                        context.report(incident)
                    }
                }
            }
        }
    }

    companion object {
        // The name of the @sample tag in KDoc
        const val SAMPLE_KDOC_ANNOTATION = "sample"
        // The name of the @Sampled annotation that samples must be annotated with
        const val SAMPLED_ANNOTATION = "Sampled"
        const val SAMPLED_ANNOTATION_FQN = "androidx.annotation.$SAMPLED_ANNOTATION"
        // The name of the samples directory inside a project
        const val SAMPLES_DIRECTORY = "samples"

        const val SAMPLE_LINK_MAP = "SampleLinkMap"
        const val SAMPLED_FUNCTION_MAP = "SampledFunctionMap"

        val OBSOLETE_SAMPLED_ANNOTATION = Issue.create(
            id = "ObsoleteSampledAnnotation",
            briefDescription = "Obsolete @$SAMPLED_ANNOTATION annotation",
            explanation = "This function is annotated with @$SAMPLED_ANNOTATION, but is not " +
                "linked to from a @$SAMPLE_KDOC_ANNOTATION tag. Either remove this annotation, " +
                "or add a valid @$SAMPLE_KDOC_ANNOTATION tag linking to it.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                SampledAnnotationDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        val UNRESOLVED_SAMPLE_LINK = Issue.create(
            id = "UnresolvedSampleLink",
            briefDescription = "Unresolved @$SAMPLE_KDOC_ANNOTATION annotation",
            explanation = "Couldn't find a valid @Sampled function matching the function " +
                "specified in the $SAMPLE_KDOC_ANNOTATION link. If there is a function with the " +
                "same fully qualified name, make sure it is annotated with @Sampled.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                SampledAnnotationDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        val MULTIPLE_FUNCTIONS_FOUND = Issue.create(
            id = "MultipleSampledFunctions",
            briefDescription = "Multiple matching functions found",
            explanation = "Found multiple functions matching the $SAMPLE_KDOC_ANNOTATION link.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                SampledAnnotationDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        val INVALID_SAMPLES_LOCATION = Issue.create(
            id = "InvalidSamplesLocation",
            briefDescription = "Invalid samples location",
            explanation = "This function is annotated with @$SAMPLED_ANNOTATION, but is not " +
                "inside a project/directory named $SAMPLES_DIRECTORY.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                SampledAnnotationDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}

/**
 * Handles KDoc with @sample links
 *
 * Checks KDoc in all applicable UDeclarations - this includes classes, functions, fields...
 */
private class KDocSampleLinkHandler(private val context: JavaContext) {
    fun visitDeclaration(node: UDeclaration) {
        val source = node.sourcePsi
        // TODO: remove workaround when https://youtrack.jetbrains.com/issue/KTIJ-19043 is fixed
        if (source is KtPropertyAccessor) {
            source.property.docComment?.let { handleSampleLink(it) }
        } else {
            node.comments
                .mapNotNull {
                    it.sourcePsi as? KDoc
                }
                .forEach {
                    handleSampleLink(it)
                }
            // Expect declarations are not visible in UAST, but they may have sample links on them.
            // If we are looking at an actual declaration, also manually find the corresponding
            // expect declaration for analysis.
            if ((source as? KtModifierListOwner)?.hasActualModifier() == true) {
                val service = node.project.getService(KotlinUastResolveProviderService::class.java)
                val member = service.getBindingContext(source)
                    .get(BindingContext.DECLARATION_TO_DESCRIPTOR, source) as? MemberDescriptor
                    // Should never be null since `actual` is only applicable to members
                    ?: return
                val expected = ExpectedActualResolver.findExpectedForActual(member) ?: return
                // There may be multiple possible candidates, we want to check them all regardless.
                expected.values.toList().flatten().forEach { descriptor ->
                    val element = descriptor.psiElement
                    (element as? KtDeclaration)?.docComment?.let {
                        handleSampleLink(it)
                    }
                }
            }
        }
    }

    private fun handleSampleLink(kdoc: KDoc) {
        val sections: List<KDocSection> = kdoc.children.mapNotNull { it as? KDocSection }

        // map of a KDocTag (which contains the location used when reporting issues) to the
        // method link specified in @sample
        val sampleTags = sections.flatMap { section ->
            section.findTagsByName(SAMPLE_KDOC_ANNOTATION)
                .mapNotNull { sampleTag ->
                    val linkText = sampleTag.getSubjectLink()?.getLinkText()
                    if (linkText == null) {
                        null
                    } else {
                        sampleTag to linkText
                    }
                }
        }.distinct()

        sampleTags.forEach { (docTag, link) ->
            // TODO: handle suppressions (if needed) with LintDriver.isSuppressed
            val mainLintMap =
                context.getPartialResults(UNRESOLVED_SAMPLE_LINK).map()

            val sampleLinkLintMap =
                mainLintMap.getMap(SAMPLE_LINK_MAP) ?: LintMap().also {
                    mainLintMap.put(SAMPLE_LINK_MAP, it)
                }

            // This overrides any identical links in the same project - no need to report the
            // same error multiple times in different places, and it is tricky to do so in any case.
            sampleLinkLintMap.put(link, context.getNameLocation(docTag))
        }
    }
}

/**
 * Handles sample functions annotated with @Sampled
 */
private class SampledAnnotationHandler(private val context: JavaContext) {

    fun visitMethod(node: UMethod) {
        if (node.hasAnnotation(SAMPLED_ANNOTATION_FQN)) {
            handleSampleCode(node)
        }
    }

    private fun handleSampleCode(node: UMethod) {
        val currentPath = context.psiFile!!.virtualFile.path

        if (SAMPLES_DIRECTORY !in currentPath) {
            val incident = Incident(context)
                .issue(INVALID_SAMPLES_LOCATION)
                .location(context.getNameLocation(node))
                .message("${node.name} is annotated with @$SAMPLED_ANNOTATION" +
                    ", but is not inside a project/directory named $SAMPLES_DIRECTORY.")
                .scope(node)
            context.report(incident)
            return
        }

        // The package name of the file we are in
        val parentFqName = (node.containingFile as KtFile).packageFqName.asString()
        // The full name of the current function that will be referenced in a @sample tag
        val fullFqName = "$parentFqName.${node.name}"

        val mainLintMap =
            context.getPartialResults(UNRESOLVED_SAMPLE_LINK).map()

        val sampledFunctionLintMap =
            mainLintMap.getMap(SAMPLED_FUNCTION_MAP) ?: LintMap().also {
                mainLintMap.put(SAMPLED_FUNCTION_MAP, it)
            }

        val location = context.getNameLocation(node)

        if (sampledFunctionLintMap.getLocation(fullFqName) != null) {
            val incident = Incident(context)
                .issue(MULTIPLE_FUNCTIONS_FOUND)
                .location(location)
                .message("Found multiple functions matching $fullFqName")
            context.report(incident)
        }

        sampledFunctionLintMap.put(fullFqName, location)
    }
}
