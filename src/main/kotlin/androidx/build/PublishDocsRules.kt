/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build

import androidx.build.ArtifactsPredicate.All
import androidx.build.ArtifactsPredicate.Benchmark
import androidx.build.ArtifactsPredicate.Exact
import androidx.build.ArtifactsPredicate.Group
import androidx.build.Strategy.Ignore
import androidx.build.Strategy.Prebuilts
import androidx.build.Strategy.TipOfTree

/**
 * Rule set used to generate public documentation.
 */
val RELEASE_RULE = docsRules("public", false) {
    prebuilts(LibraryGroups.ACTIVITY, "1.0.0-beta01")
    prebuilts(LibraryGroups.ANNOTATION, "1.1.0")
    prebuilts(LibraryGroups.APPCOMPAT, "1.1.0-beta01")
    prebuilts(LibraryGroups.ARCH_CORE, "2.1.0-rc01")
    prebuilts(LibraryGroups.ASYNCLAYOUTINFLATER, "1.0.0")
    ignore(LibraryGroups.BENCHMARK.group, "benchmark-gradle-plugin")
    prebuilts(LibraryGroups.BENCHMARK, "1.0.0-alpha02")
    prebuilts(LibraryGroups.BIOMETRIC, "biometric", "1.0.0-alpha04")
    prebuilts(LibraryGroups.BROWSER, "1.0.0")
    ignore(LibraryGroups.CAMERA.group, "camera-view")
    ignore(LibraryGroups.CAMERA.group, "camera-testing")
    ignore(LibraryGroups.CAMERA.group, "camera-extensions")
    ignore(LibraryGroups.CAMERA.group, "camera-extensions-stub")
    ignore(LibraryGroups.CAMERA.group, "camera-testlib-extensions")
    prebuilts(LibraryGroups.CAMERA, "1.0.0-alpha02")
    ignore(LibraryGroups.CAR.group, "car-moderator")
    prebuilts(LibraryGroups.CAR, "car-cluster", "1.0.0-alpha5")
    prebuilts(LibraryGroups.CAR, "car", "1.0.0-alpha7")
            .addStubs("car/stubs/android.car.jar")
    prebuilts(LibraryGroups.CARDVIEW, "1.0.0")
    prebuilts(LibraryGroups.COLLECTION, "1.1.0")
    prebuilts(LibraryGroups.CONCURRENT, "1.0.0-beta01")
    prebuilts(LibraryGroups.CONTENTPAGER, "1.0.0")
    prebuilts(LibraryGroups.COORDINATORLAYOUT, "1.1.0-beta01")
    prebuilts(LibraryGroups.CORE, "core", "1.2.0-alpha02")
    prebuilts(LibraryGroups.CORE, "core-ktx", "1.2.0-alpha02")
    prebuilts(LibraryGroups.CURSORADAPTER, "1.0.0")
    prebuilts(LibraryGroups.CUSTOMVIEW, "1.1.0-alpha01")
    prebuilts(LibraryGroups.DOCUMENTFILE, "1.0.0")
    prebuilts(LibraryGroups.DRAWERLAYOUT, "1.1.0-alpha02")
    prebuilts(LibraryGroups.DYNAMICANIMATION, "dynamicanimation-ktx", "1.0.0-alpha02")
    prebuilts(LibraryGroups.DYNAMICANIMATION, "1.1.0-alpha01")
    prebuilts(LibraryGroups.EMOJI, "1.0.0")
    prebuilts(LibraryGroups.ENTERPRISE, "1.0.0-alpha02")
    prebuilts(LibraryGroups.EXIFINTERFACE, "1.1.0-alpha01")
    prebuilts(LibraryGroups.FRAGMENT, "1.1.0-beta01")
    prebuilts(LibraryGroups.GRIDLAYOUT, "1.0.0")
    prebuilts(LibraryGroups.HEIFWRITER, "1.0.0")
    prebuilts(LibraryGroups.INTERPOLATOR, "1.0.0")
    prebuilts(LibraryGroups.LEANBACK, "1.1.0-alpha02")
    prebuilts(LibraryGroups.LEGACY, "1.0.0")
    ignore(LibraryGroups.LIFECYCLE.group, "lifecycle-compiler")
    ignore(LibraryGroups.LIFECYCLE.group, "lifecycle-runtime-ktx-lint")
    prebuilts(LibraryGroups.LIFECYCLE, "lifecycle-viewmodel-savedstate", "1.0.0-alpha01")
    prebuilts(LibraryGroups.LIFECYCLE, "2.2.0-alpha01")
    ignore(LibraryGroups.LOADER.group, "loader-ktx")
    prebuilts(LibraryGroups.LOADER, "1.1.0-rc01")
    prebuilts(LibraryGroups.LOCALBROADCASTMANAGER, "1.1.0-alpha01")
    prebuilts(LibraryGroups.MEDIA, "media", "1.1.0-rc01")
    // TODO: Rename media-widget to media2-widget after 1.0.0-alpha06
    prebuilts(LibraryGroups.MEDIA, "media-widget", "1.0.0-alpha06")
    ignore(LibraryGroups.MEDIA2.group, "media2-widget")
    ignore(LibraryGroups.MEDIA2.group, "media2-exoplayer")
    prebuilts(LibraryGroups.MEDIA2, "1.0.0-rc01")
    prebuilts(LibraryGroups.MEDIAROUTER, "1.1.0-rc01")
    ignore(LibraryGroups.NAVIGATION.group, "navigation-testing")
    ignore(LibraryGroups.NAVIGATION.group, "navigation-safe-args-generator")
    ignore(LibraryGroups.NAVIGATION.group, "navigation-safe-args-gradle-plugin")
    prebuilts(LibraryGroups.NAVIGATION, "2.1.0-alpha05")
    prebuilts(LibraryGroups.PAGING, "2.1.0")
    prebuilts(LibraryGroups.PALETTE, "1.0.0")
    prebuilts(LibraryGroups.PERCENTLAYOUT, "1.0.0")
    prebuilts(LibraryGroups.PERSISTENCE, "2.0.0")
    prebuilts(LibraryGroups.PREFERENCE, "preference-ktx", "1.1.0-beta01")
    prebuilts(LibraryGroups.PREFERENCE, "1.1.0-beta01")
    prebuilts(LibraryGroups.PRINT, "1.0.0")
    prebuilts(LibraryGroups.RECOMMENDATION, "1.0.0")
    prebuilts(LibraryGroups.RECYCLERVIEW, "recyclerview", "1.1.0-alpha06")
    prebuilts(LibraryGroups.RECYCLERVIEW, "recyclerview-selection", "1.1.0-alpha06")
    prebuilts(LibraryGroups.REMOTECALLBACK, "1.0.0-alpha02")
    ignore(LibraryGroups.ROOM.group, "room-common-java8")
    prebuilts(LibraryGroups.ROOM, "2.1.0")
    prebuilts(LibraryGroups.SAVEDSTATE, "1.0.0-beta01")
    // TODO: Remove this ignore once androidx.security:security-identity-credential:1.0.0-alpha01 is released
    ignore(LibraryGroups.SECURITY.group, "security-identity-credential")
    prebuilts(LibraryGroups.SECURITY, "1.0.0-alpha02")
    prebuilts(LibraryGroups.SHARETARGET, "1.0.0-alpha02")
    prebuilts(LibraryGroups.SLICE, "slice-builders", "1.1.0-alpha01")
    prebuilts(LibraryGroups.SLICE, "slice-builders-ktx", "1.0.0-alpha07")
    prebuilts(LibraryGroups.SLICE, "slice-core", "1.1.0-alpha01")
    // TODO: land prebuilts
//    prebuilts(LibraryGroups.SLICE.group, "slice-test", "1.0.0")
    ignore(LibraryGroups.SLICE.group, "slice-test")
    prebuilts(LibraryGroups.SLICE, "slice-view", "1.1.0-alpha01")
    prebuilts(LibraryGroups.SLIDINGPANELAYOUT, "1.0.0")
    prebuilts(LibraryGroups.SWIPEREFRESHLAYOUT, "1.1.0-alpha01")
    prebuilts(LibraryGroups.TEXTCLASSIFIER, "1.0.0-alpha02")
    prebuilts(LibraryGroups.TRANSITION, "1.2.0-alpha02")
    prebuilts(LibraryGroups.TVPROVIDER, "1.0.0")
    prebuilts(LibraryGroups.VECTORDRAWABLE, "1.1.0-beta02")
    prebuilts(LibraryGroups.VECTORDRAWABLE, "vectordrawable-animated", "1.1.0-beta02")
    prebuilts(LibraryGroups.VERSIONEDPARCELABLE, "1.1.0-rc01")
    prebuilts(LibraryGroups.VIEWPAGER, "1.0.0")
    prebuilts(LibraryGroups.VIEWPAGER2, "1.0.0-alpha05")
    prebuilts(LibraryGroups.WEAR, "1.0.0")
            .addStubs("wear/wear_stubs/com.google.android.wearable-stubs.jar")
    prebuilts(LibraryGroups.WEBKIT, "1.1.0-alpha01")
    ignore(LibraryGroups.WORK.group, "work-gcm")
    prebuilts(LibraryGroups.WORK, "2.1.0-beta02")
    default(Ignore)
}

/**
 * Rule set used to generate tip-of-tree documentation, typically for local and pre-submit use.
 */
val TIP_OF_TREE = docsRules("tipOfTree", true) {
    ignore(LibraryGroups.COMPOSE.group)
    // TODO: remove once we'll figure out our strategy about it
    ignore(LibraryGroups.CONCURRENT.group)
    default(TipOfTree)
}

/**
 * Builds rules describing how to generate documentation for a set of libraries.
 *
 * Rules are resolved in the order in which they were added. So, if you have two rules that specify
 * how docs should be built for a module, the first matching rule will be used.
 *
 * @property name human-readable label for this documentation set
 * @property offline true if generating documentation for local use, false otherwise.
 * @property init lambda that initializes a rule builder.
 * @return rules describing how to generate documentation.
 */
fun docsRules(
    name: String,
    offline: Boolean,
    init: PublishDocsRulesBuilder.() -> Unit
): PublishDocsRules {
    val f = PublishDocsRulesBuilder(name, offline)
    f.init()
    return f.build()
}

/**
 * Builder for rules describing how to generate documentation for a set of libraries.
 *
 * @property name human-readable label for this documentation set
 * @property offline true if generating documentation for local use, false otherwise.
 * @constructor Creates a builder with no rules specified.
 */
class PublishDocsRulesBuilder(private val name: String, private val offline: Boolean) {
    private val rules: MutableList<DocsRule> = mutableListOf(DocsRule(Benchmark, Ignore))

    /**
     * Specifies that docs for projects within [groupName] will be built from sources.
     */
    fun tipOfTree(groupName: String) {
        rules.add(DocsRule(Group(groupName), TipOfTree))
    }

    /**
     * Specifies that docs for a project with the given [groupName] and artifact [name] will be
     * built from sources.
     */
    fun tipOfTree(groupName: String, name: String) {
        rules.add(DocsRule(Exact(groupName, name), TipOfTree))
    }

    /**
     * Specifies that docs for a project with the given [groupName] and artifact [name] will be
     * built from a prebuilt with the given [version].
     */
    fun prebuilts(libraryGroup: LibraryGroup, moduleName: String, version: String): Prebuilts {
        val strategy = Prebuilts(Version(version))
        rules.add(DocsRule(Exact(libraryGroup.group, moduleName), strategy))
        return strategy
    }

    /**
     * Specifies that docs for projects within [groupName] will be built from prebuilts with the
     * given [version].
     */
    fun prebuilts(libraryGroup: LibraryGroup, version: String) =
            prebuilts(libraryGroup, Version(version))

    /**
     * Specifies that docs for projects within [groupName] will be built from prebuilts with the
     * given [version].
     */
    fun prebuilts(libraryGroup: LibraryGroup, version: Version): Prebuilts {
        val strategy = Prebuilts(version)
        rules.add(DocsRule(Group(libraryGroup.group), strategy))
        return strategy
    }

    /**
     * Specifies the default strategy for building docs.
     *
     * This method should be called last, as it matches all candidates. No rules specified after
     * calling this method will have any effect.
     */
    fun default(strategy: Strategy) {
        rules.add(DocsRule(All, strategy))
    }

    /**
     * Specifies that docs for projects with the given [groupName] won't be built.
     */
    fun ignore(groupName: String) {
        rules.add(DocsRule(Group(groupName), Ignore))
    }

    /**
     * Specifies that docs for a project with the given [groupName] and artifact [name] won't be
     * built.
     */
    fun ignore(groupName: String, name: String) {
        rules.add(DocsRule(Exact(groupName, name), Ignore))
    }

    /**
     * Builds a fully-initialized set of documentation rules.
     */
    fun build() = PublishDocsRules(name, offline, rules)
}

/**
 * ArtifactsPredicates are used to match libraries.
 */
sealed class ArtifactsPredicate {
    /**
     * Returns true if the predicate matches the specified library project.
     *
     * @param inGroup the library Maven groupId to be matched.
     * @param inName the library Maven artifact name to be matched.
     * @return true if the predicate matches the library.
     */
    abstract fun apply(inGroup: String, inName: String): Boolean

    /**
     * Predicate that matches all library projects.
     */
    object All : ArtifactsPredicate() {
        override fun apply(inGroup: String, inName: String) = true
    }

    /**
     * Predicate that matches library projects with the specified Maven groupId.
     *
     * @property group the Maven groupId to be matched.
     * @constructor Creates a predicate to match the specified Maven groupId.
     */
    class Group(val group: String) : ArtifactsPredicate() {
        override fun apply(inGroup: String, inName: String) = inGroup == group
        override fun toString() = "\"$group\""
    }

    /**
     * Predicate that matches library projects with the specified Maven groupId and artifact name.
     *
     * @property group the Maven groupId to be matched.
     * @peoperty name the Maven artifact name to be matched.
     * @constructor Creates a predicate to match the specified Maven groupId and artifact name.
     */
    class Exact(val group: String, val name: String) : ArtifactsPredicate() {
        override fun apply(inGroup: String, inName: String) = group == inGroup && name == inName
        override fun toString() = "\"$group\", \"$name\""
    }

    /**
     * Predicate that matches all benchmark projects, e.g. all library projects where the project
     * name is suffixed with "-benchmark".
     */
    object Benchmark : ArtifactsPredicate() {
        override fun apply(inGroup: String, inName: String) = inName.endsWith("-benchmark")
    }
}

/**
 * Rule associating a [predicate] -- used to match libraries -- with a documentation strategy.
 *
 * @property predicate the predicate used to match libraries.
 * @property strategy the strategy used to generate documentation.
 */
data class DocsRule(val predicate: ArtifactsPredicate, val strategy: Strategy) {
    override fun toString(): String {
        if (predicate is All) {
            return "default($strategy)"
        }
        return when (strategy) {
            is Prebuilts -> "prebuilts($predicate, \"${strategy.version}\")"
            is Ignore -> "ignore($predicate)"
            is TipOfTree -> "tipOfTree($predicate)"
        }
    }
}

/**
 * Strategies are used to inform the build of which source set should be used when generating
 * documentation.
 */
sealed class Strategy {
    /**
     * Strategy that uses tip-of-tree source code, equivalent to a project() dependency.
     */
    object TipOfTree : Strategy()

    /**
     * Strategy that does not generate documentation.
     */
    object Ignore : Strategy()

    /**
     * Strategy that uses a versioned prebuilt, equivalent to a Maven coordinate dependency.
     */
    class Prebuilts(val version: Version) : Strategy() {
        /**
         * List of stub JARs that should be made available on the documentation generator's
         * classpath.
         */
        var stubs: MutableList<String>? = null

        /**
         * Adds a stub JAR to the documentation generation tool's classpath.
         *
         * Useful for generating documentation for libraries that depend on sidecar JARs or other
         * run-time dependencies that would not otherwise be available on the classpath for the
         * documentation generation tool.
         *
         * @param path the path to the stub JAR relative to the top-level AndroidX project root.
         */
        fun addStubs(path: String) {
            if (stubs == null) {
                stubs = mutableListOf()
            }
            stubs!!.add(path)
        }

        override fun toString() = "Prebuilts(\"$version\")"

        /**
         * Returns a Maven dependency spec for the specified library [extension].
         */
        fun dependency(extension: AndroidXExtension): String {
            return "${extension.mavenGroup?.group}:${extension.project.name}:$version"
        }
    }
}

/**
 * Rules describing how to generate documentation for a set of libraries.
 *
 * @property name human-readable label for this documentation set
 * @property offline true if generating documentation for local use, false otherwise.
 * @constructor Creates a documentation rule set.
 */
class PublishDocsRules(val name: String, val offline: Boolean, private val rules: List<DocsRule>) {
    /**
     * Resolves a rule describing how to generate documentation for the given library.
     *
     * If multiple rules match, the matching first rule is returned.
     *
     * @return the documentation rule
     */
    fun resolve(extension: AndroidXExtension): DocsRule? {
        val mavenGroup = extension.mavenGroup
        return if (mavenGroup == null) null else resolve(mavenGroup.group, extension.project.name)
    }

    /**
     * Resolves a rule describing how to generate documentation for a given Maven group and module
     * name.
     *
     * If multiple rules match, the matching first rule is returned.
     *
     * @return the documentation rule
     */
    fun resolve(groupName: String, moduleName: String): DocsRule {
        return rules.find { it.predicate.apply(groupName, moduleName) } ?: throw Error()
    }
}
