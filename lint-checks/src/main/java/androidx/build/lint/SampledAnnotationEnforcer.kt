/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build.lint

import androidx.build.lint.SampledAnnotationEnforcer.Companion.SAMPLED_ANNOTATION
import androidx.build.lint.SampledAnnotationEnforcer.Companion.SAMPLES_DIRECTORY
import androidx.build.lint.SampledAnnotationEnforcer.Companion.SAMPLE_KDOC_ANNOTATION
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.findDocComment.findDocComment
import org.jetbrains.kotlin.psi.psiUtil.safeNameForLazyResolve
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import java.io.File

/**
 * Class containing two lint detectors responsible for enforcing @Sampled annotation usage when
 * AndroidXExtension.enforceSampledAnnotation == true
 *
 * 1. KDocSampleLinkDetector, which enforces that any samples referenced from a @sample tag in KDoc
 * are correctly annotated with @Sampled
 *
 * 2. SampledAnnotationDetector, which enforces that any sample functions annotated with @Sampled
 * are linked to from KDoc in the parent module
 *
 * These lint checks make some assumptions about directory / module structure, and supports two
 * such setups:
 *
 * 1. Module foo which has a 'samples' dir/module inside it
 * 2. Module foo which has a 'samples' dir/module alongside it
 *
 * There are also some other tangentially related lint issues that can be reported, to ensure sample
 * correctness:
 *
 * 1. Missing samples directory
 * 2. No functions found matching a @sample link
 * 3. Multiple functions found matching a @sample link
 * 4. Function annotated with @Sampled does not live in a valid samples directory
 */
class SampledAnnotationEnforcer {

    companion object {
        // The name of the @sample tag in KDoc
        const val SAMPLE_KDOC_ANNOTATION = "sample"
        // The name of the @Sampled annotation that samples must be annotated with
        const val SAMPLED_ANNOTATION = "Sampled"
        // The name of the samples directory inside a project
        const val SAMPLES_DIRECTORY = "samples"

        val MISSING_SAMPLED_ANNOTATION = Issue.create(
            "EnforceSampledAnnotation",
            "Missing @$SAMPLED_ANNOTATION annotation",
            "Functions referred to from KDoc with a @$SAMPLE_KDOC_ANNOTATION tag must " +
                    "be annotated with @$SAMPLED_ANNOTATION, to provide visibility at the sample " +
                    "site and ensure that it doesn't get changed accidentally.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(KDocSampleLinkDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        val OBSOLETE_SAMPLED_ANNOTATION = Issue.create(
            "EnforceSampledAnnotation",
            "Obsolete @$SAMPLED_ANNOTATION annotation",
            "This function is annotated with @$SAMPLED_ANNOTATION, but is not linked to " +
                    "from a @$SAMPLE_KDOC_ANNOTATION tag. Either remove this annotation, or add " +
                    "a valid @$SAMPLE_KDOC_ANNOTATION tag linking to it.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(SampledAnnotationDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        val MISSING_SAMPLES_DIRECTORY = Issue.create(
            "EnforceSampledAnnotation",
            "Missing $SAMPLES_DIRECTORY directory",
            "Couldn't find a valid $SAMPLES_DIRECTORY directory in this project.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(SampledAnnotationDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        val UNRESOLVED_SAMPLE_LINK = Issue.create(
            "EnforceSampledAnnotation",
            "Unresolved @$SAMPLE_KDOC_ANNOTATION annotation",
            "Couldn't find a valid function matching the function specified in the " +
                    "$SAMPLE_KDOC_ANNOTATION link.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(SampledAnnotationDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        val MULTIPLE_FUNCTIONS_FOUND = Issue.create(
            "EnforceSampledAnnotation",
            "Multiple matching functions found",
            "Found multiple functions matching the $SAMPLE_KDOC_ANNOTATION link.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(SampledAnnotationDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        val INVALID_SAMPLES_LOCATION = Issue.create(
            "EnforceSampledAnnotation",
            "Invalid samples location",
            "This function is annotated with @$SAMPLED_ANNOTATION, but is not inside a" +
                    "project/directory named $SAMPLES_DIRECTORY.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(SampledAnnotationDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    /**
     * Enforces that any @sample links in KDoc link to a function that is annotated with @Sampled
     *
     * Checks KDoc in all applicable UDeclarations - this includes classes, functions, fields...
     *
     * Also reports issues if there is no samples directory found, if there is no function matching
     * a given @sample link, and if there are multiple functions matching a given @sample link.
     */
    class KDocSampleLinkDetector : Detector(), SourceCodeScanner {
        // Cache containing every function inside a project's corresponding samples dir
        private var sampleFunctionCache: List<KtNamedFunction>? = null

        /**
         * @return a list of all the functions found in the samples directory for this project, or
         * `null` if there is no valid samples directory for this project
         */
        internal fun getSampleFunctionCache(context: JavaContext): List<KtNamedFunction>? {
            if (sampleFunctionCache == null) {
                val sampleDirectory = findSampleDirectory(context)
                sampleFunctionCache = if (sampleDirectory == null) {
                    null
                } else {
                    val allKtFiles = sampleDirectory.getAllKtFiles()
                    // Remove any functions without a valid fully qualified name, this includes
                    // things such as overridden functions in anonymous classes like a Runnable
                    allKtFiles.flatMap { it.getAllFunctions() }.filter {
                        it.fqName != null
                    }
                }
            }
            return sampleFunctionCache
        }

        override fun getApplicableUastTypes(): List<Class<out UElement>>? =
            listOf(UDeclaration::class.java)

        override fun createUastHandler(context: JavaContext): UElementHandler? =
            KDocSampleLinkHandler(context)

        /**
         * Clear caches before and after a project run, as they are only relevant per project
         *
         * Note: this isn't strictly needed, as normally a new detector will be instantiated per
         * project, but if lint is set to run on dependencies, the same detector will end up
         * being reused, and we can run into some caching issues. Safer just to always clear here as
         * we really want to avoid false positives.
         */
        override fun beforeCheckEachProject(context: Context) {
            sampleFunctionCache = null
        }

        override fun afterCheckEachProject(context: Context) {
            sampleFunctionCache = null
        }

        private inner class KDocSampleLinkHandler(
            private val context: JavaContext
        ) : UElementHandler() {

            // Cache containing all the KDoc elements we have found inside this Handler (created
            // once per JavaContext - Java/Kotlin source file)
            private val kdocCache = mutableListOf<KDoc>()

            override fun visitDeclaration(node: UDeclaration) {
                node.findKdoc()?.let { kdoc ->
                    // It's possible for different declarations to point to the same KDoc - for
                    // example if a class has some KDoc, we will visit it once for the constructor
                    // function and once for the class itself. If we have seen this KDoc before
                    // just skip it so we don't report the issue multiple times
                    if (kdoc !in kdocCache) {
                        kdocCache.add(kdoc)
                        handleSampleLink(kdoc, node.namedUnwrappedElement!!.name!!)
                    }
                }
            }

            private fun handleSampleLink(kdoc: KDoc, sourceNodeName: String) {
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

                    val allFunctions = getSampleFunctionCache(context)

                    if (allFunctions == null) {
                        context.report(
                            MISSING_SAMPLES_DIRECTORY,
                            docTag,
                            context.getNameLocation(docTag),
                            "Couldn't find a valid $SAMPLES_DIRECTORY directory in this project"
                        )
                        return@forEach
                    }

                    // We filtered out not-null fqNames when building the cache, so safe to !!
                    val matchingFunctions = allFunctions.filter {
                        link == it.fqName!!.asString()
                    }

                    when (matchingFunctions.size) {
                        0 -> {
                            context.report(
                                UNRESOLVED_SAMPLE_LINK,
                                docTag,
                                context.getNameLocation(docTag),
                                "Couldn't find a valid function matching $link"
                            )
                        }
                        1 -> {
                            val function = matchingFunctions.first()
                            if (!function.hasSampledAnnotation()) {
                                context.report(
                                    MISSING_SAMPLED_ANNOTATION,
                                    docTag,
                                    context.getNameLocation(docTag),
                            "${function.name} is not annotated with @$SAMPLED_ANNOTATION" +
                                            ", but is linked to from the KDoc of $sourceNodeName"
                                )
                            }
                        }
                        else -> {
                            context.report(
                                MULTIPLE_FUNCTIONS_FOUND,
                                docTag,
                                context.getNameLocation(docTag),
                                "Found multiple functions matching $link"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks all functions annotated with @Sampled to ensure that they are linked from KDoc
     *
     * Also reports an issue if a function is annotated with @Sampled, and does not live in a
     * valid samples directory/module.
     */
    class SampledAnnotationDetector : Detector(), SourceCodeScanner {
        // Cache containing every link referenced from a @sample tag inside the parent project
        private var sampleLinkCache: List<String>? = null

        internal fun getSampleLinkCache(context: JavaContext): List<String> {
            if (sampleLinkCache == null) {
                sampleLinkCache = buildSampleLinkCache(context)
            }
            return sampleLinkCache!!
        }

        override fun getApplicableUastTypes(): List<Class<out UElement>>? =
            listOf(UMethod::class.java)

        override fun createUastHandler(context: JavaContext): UElementHandler? =
            SampledAnnotationHandler(context)

        /**
         * Clear caches before and after a project run, as they are only relevant per project
         *
         * Note: this isn't strictly needed, as normally a new detector will be instantiated per
         * project, but if lint is set to run on dependencies, the same detector will end up
         * being reused, and we can run into some caching issues. Safer just to always clear here as
         * we really want to avoid false positives.
         */
        override fun beforeCheckEachProject(context: Context) {
            sampleLinkCache = null
        }

        override fun afterCheckEachProject(context: Context) {
            sampleLinkCache = null
        }

        private inner class SampledAnnotationHandler(
            private val context: JavaContext
        ) : UElementHandler() {

            override fun visitMethod(node: UMethod) {
                val element = (node.sourceElement as? KtDeclaration) ?: return

                if (element.annotationEntries.any {
                        it.shortName.safeNameForLazyResolve().identifier == SAMPLED_ANNOTATION
                    }) {
                    handleSampleCode(element, node)
                }
            }

            private fun handleSampleCode(function: KtDeclaration, node: UMethod) {
                val currentPath = context.psiFile!!.virtualFile.path

                if (SAMPLES_DIRECTORY !in currentPath) {
                    context.report(
                        INVALID_SAMPLES_LOCATION,
                        node,
                        context.getNameLocation(node),
                        "${function.name} in $currentPath is annotated with @$SAMPLED_ANNOTATION" +
                                ", but is not inside a project/directory named $SAMPLES_DIRECTORY."
                    )
                    return
                }

                // The package name of the file we are in
                val parentFqName = function.containingKtFile.packageFqName.asString()
                // The full name of the current function that will be referenced in a @sample tag
                val fullFqName = "$parentFqName.${function.name}"

                if (getSampleLinkCache(context).none { it == fullFqName }) {
                    context.report(
                        OBSOLETE_SAMPLED_ANNOTATION,
                        node,
                        context.getNameLocation(node),
                        "${function.name} is annotated with @$SAMPLED_ANNOTATION, but is not " +
                                "linked to from a @$SAMPLE_KDOC_ANNOTATION tag."
                    )
                }
            }
        }

        /**
         * At this point we are inside some sample module, which is depending on a module that
         * would end up referencing the sample
         *
         * For example, we could be in :foo:integration-tests:sample, and we want to find the
         * path for module :foo
         */
        private fun buildSampleLinkCache(context: JavaContext): List<String> {
            val currentProjectPath = context.project.dir.absolutePath

            // The paths of every module the current module depends on
            val dependenciesPathList = context.project.directLibraries.map {
                it.dir.absolutePath
            }

            // Try and find a common path, i.e if we are in a/b/foo/integration-tests/sample, we
            // will match a/b/foo for the parent
            var parentProjectPath = dependenciesPathList.find {
                currentProjectPath.startsWith(it)
            }

            // If we haven't found a path, it might be that we are on the same top level, i.e
            // we are in a/b/foo/integration-tests/sample, and the module is in a/b/foo/foo-xyz
            // Try matching with the parent directory of each module.
            parentProjectPath = parentProjectPath ?: dependenciesPathList.find {
                currentProjectPath.startsWith(File(it).parent)
            }

            // There is no dependent module that exists above us, or alongside us, so throw
            if (parentProjectPath == null) {
                throw IllegalStateException("Couldn't find a parent project for " +
                        currentProjectPath
                )
            }

            val parentProjectDirectory = navigateToDirectory(context, parentProjectPath)

            return parentProjectDirectory.getAllKtFiles().flatMap { file ->
                file.findAllSampleLinks()
            }
        }
    }
}

/**
 * @return the KDoc for the given section, or `null` if there is no corresponding KDoc
 *
 * This also filters out non-Kotlin declarations, so we don't bother looking for KDoc inside
 * Java nodes for example.
 */
internal fun UDeclaration.findKdoc(): KDoc? {
    // Unfortunate workaround as the KDoc cannot be returned from the node directly
    // https://youtrack.jetbrains.com/issue/KT-22135
    val ktDeclaration = sourceElement as? KtDeclaration ?: return null
    return findDocComment(ktDeclaration)
}

/**
 * @return whether this function is annotated with @Sampled
 */
internal fun KtNamedFunction.hasSampledAnnotation(): Boolean {
    return modifierList?.annotationEntries?.any { annotation ->
        annotation.shortName.safeNameForLazyResolve().identifier == SAMPLED_ANNOTATION
    } ?: false
}

/**
 * @return a list of all sample links found recursively inside the element
 */
internal fun PsiElement.findAllSampleLinks(): List<String> {
    val sampleLinks = mutableListOf<String>()
    if (this is KDoc) {
        val sections: List<KDocSection> = this.children.mapNotNull { it as? KDocSection }

        sections.forEach { section ->
            section.findTagsByName(SAMPLE_KDOC_ANNOTATION).forEach { sampleTag ->
                sampleTag.getSubjectLink()?.getLinkText()?.let { sampleLinks.add(it) }
            }
        }
    }
    children.forEach { sampleLinks.addAll(it.findAllSampleLinks()) }
    return sampleLinks
}

/**
 * @return a list of all files found recursively inside the directory
 */
internal fun PsiDirectory.getAllKtFiles(): List<PsiFile> {
    val psiFiles = mutableListOf<PsiFile>()
    accept(object : KtTreeVisitorVoid() {
        override fun visitFile(file: PsiFile?) {
            psiFiles.addIfNotNull(file as? KtFile)
        }
    })
    return psiFiles
}

/**
 * @return a list of all functions found recursively inside the element
 */
internal fun PsiElement.getAllFunctions(): List<KtNamedFunction> {
    val functions = mutableListOf<KtNamedFunction>()
    accept(object : KtTreeVisitorVoid() {
        override fun visitDeclaration(dcl: KtDeclaration) {
            functions.addIfNotNull(dcl as? KtNamedFunction)
        }
    })
    return functions
}

/**
 * @return the samples directory if it exists, otherwise returns null
 *
 * The samples directory could either be a direct child of the current module, or a
 * sibling directory
 *
 * For example, if we are in a/b/foo, the samples directory could either be:
 *     a/b/foo/.../samples
 *     a/b/.../samples
 *
 * For efficiency, first we look inside a/b/foo, and then if that fails we look
 * inside a/b
 */
internal fun findSampleDirectory(context: JavaContext): PsiDirectory? {
    val currentProjectPath = context.project.dir.absolutePath
    val currentProjectDir = navigateToDirectory(context, currentProjectPath)
    fun PsiDirectory.searchForSampleDirectory(): PsiDirectory? {
        if (name == SAMPLES_DIRECTORY) {
            return this
        }
        subdirectories.forEach {
            val dir = it.searchForSampleDirectory()
            if (dir != null) {
                return dir
            }
        }
        return null
    }

    // Look inside a/b/foo
    var sampleDir = currentProjectDir.searchForSampleDirectory()

    // Try looking inside /a/b
    if (sampleDir == null) {
        sampleDir = currentProjectDir.parent!!.searchForSampleDirectory()
    }

    return sampleDir
}

/**
 * @return the directory with the given [path], using [context] to get the current filesystem
 */
internal fun navigateToDirectory(context: JavaContext, path: String): PsiDirectory {
    val file = context.psiFile
        ?: throw IllegalStateException("Not linting a source file")
    val filesystem = file.virtualFile.fileSystem
    val virtualFile = filesystem.findFileByPath(path)
    return file.manager.findDirectory(virtualFile!!)
        ?: throw IllegalStateException("Couldn't find directory for $path")
}
