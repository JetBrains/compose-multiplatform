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
import androidx.build.ArtifactsPredicate.Exact
import androidx.build.ArtifactsPredicate.Group
import androidx.build.Strategy.TipOfTree
import androidx.build.Strategy.Prebuilts
import androidx.build.Strategy.Ignore

val RELEASE_RULE = docsRules("public") {
    val defaultVersion = "1.0.0-alpha3"
    prebuilts(LibraryGroups.ANNOTATION, defaultVersion)
    prebuilts(LibraryGroups.APPCOMPAT, defaultVersion)
    prebuilts(LibraryGroups.ASYNCLAYOUTINFLATER, defaultVersion)
    prebuilts(LibraryGroups.BROWSER, defaultVersion)
    prebuilts(LibraryGroups.CAR, defaultVersion)
            .addStubs("car/car-stubs/android.car.jar")
    prebuilts(LibraryGroups.CARDVIEW, defaultVersion)
    prebuilts(LibraryGroups.COLLECTION, defaultVersion)
    prebuilts(LibraryGroups.CONTENTPAGER, defaultVersion)
    prebuilts(LibraryGroups.COORDINATORLAYOUT, defaultVersion)
    prebuilts(LibraryGroups.CORE, defaultVersion)
    prebuilts(LibraryGroups.CURSORADAPTER, defaultVersion)
    prebuilts(LibraryGroups.CUSTOMVIEW, defaultVersion)
    prebuilts(LibraryGroups.DOCUMENTFILE, defaultVersion)
    prebuilts(LibraryGroups.DRAWERLAYOUT, defaultVersion)
    prebuilts(LibraryGroups.DYNAMICANIMATION, defaultVersion)
    prebuilts(LibraryGroups.EMOJI, defaultVersion)
    prebuilts(LibraryGroups.EXIFINTERFACE, defaultVersion)
    prebuilts(LibraryGroups.FRAGMENT, defaultVersion)
    prebuilts(LibraryGroups.GRIDLAYOUT, defaultVersion)
    prebuilts(LibraryGroups.HEIFWRITER, defaultVersion)
    prebuilts(LibraryGroups.INTERPOLATOR, defaultVersion)
    prebuilts(LibraryGroups.LEANBACK, defaultVersion)
    prebuilts(LibraryGroups.LEGACY, defaultVersion)
    prebuilts(LibraryGroups.LOADER, defaultVersion)
    prebuilts(LibraryGroups.LOCALBROADCASTMANAGER, defaultVersion)
    prebuilts(LibraryGroups.MEDIA, defaultVersion)
    prebuilts(LibraryGroups.MEDIAROUTER, defaultVersion)
    prebuilts(LibraryGroups.PALETTE, defaultVersion)
    prebuilts(LibraryGroups.PERCENTLAYOUT, defaultVersion)
    prebuilts(LibraryGroups.PREFERENCE, defaultVersion)
    prebuilts(LibraryGroups.PRINT, defaultVersion)
    prebuilts(LibraryGroups.RECOMMENDATION, defaultVersion)
    prebuilts(LibraryGroups.RECYCLERVIEW, defaultVersion)
    prebuilts(LibraryGroups.SLICE, defaultVersion)
    prebuilts(LibraryGroups.SLIDINGPANELAYOUT, defaultVersion)
    prebuilts(LibraryGroups.SWIPEREFRESHLAYOUT, defaultVersion)
    prebuilts(LibraryGroups.TEXTCLASSIFIER, defaultVersion)
    prebuilts(LibraryGroups.TRANSITION, defaultVersion)
    prebuilts(LibraryGroups.TVPROVIDER, defaultVersion)
    prebuilts(LibraryGroups.VECTORDRAWABLE, defaultVersion)
    prebuilts(LibraryGroups.VIEWPAGER, defaultVersion)
    prebuilts(LibraryGroups.WEAR, defaultVersion)
            .addStubs("wear/wear_stubs/com.google.android.wearable-stubs.jar")
    prebuilts(LibraryGroups.WEBKIT, defaultVersion)
    val flatfootVersion = "2.0.0-alpha1"
    prebuilts(LibraryGroups.ROOM, flatfootVersion)
    prebuilts(LibraryGroups.PERSISTENCE, flatfootVersion)
    prebuilts(LibraryGroups.LIFECYCLE, flatfootVersion)
    prebuilts(LibraryGroups.ARCH_CORE, flatfootVersion)
    prebuilts(LibraryGroups.PAGING, flatfootVersion)
    prebuilts(LibraryGroups.NAVIGATION, "1.0.0-alpha01")
    prebuilts(LibraryGroups.WORKMANAGER, "1.0.0-alpha02")
    default(Ignore)
}

val TIP_OF_TREE = docsRules("tipOfTree") {
    default(TipOfTree)
}

/**
 * Rules are resolved in addition order. So if you have two rules that specify how docs should be
 * built for a module, first defined rule wins.
 */
fun docsRules(name: String, init: PublishDocsRulesBuilder.() -> Unit): PublishDocsRules {
    val f = PublishDocsRulesBuilder(name)
    f.init()
    return f.build()
}

class PublishDocsRulesBuilder(private val name: String) {

    private val rules: MutableList<DocsRule> = mutableListOf()
    /**
     * docs for projects within [groupName] will be built from sources.
     */
    fun tipOfTree(groupName: String) {
        rules.add(DocsRule(Group(groupName), TipOfTree))
    }

    /**
     * docs for a project with the given [groupName] and [name] will be built from sources.
     */
    fun tipOfTree(groupName: String, name: String) {
        rules.add(DocsRule(Exact(groupName, name), TipOfTree))
    }

    /**
     * docs for a project with the given [groupName] and [name] will be built from a prebuilt with
     * the given [version].
     */
    fun prebuilts(groupName: String, moduleName: String, version: String) {
        rules.add(DocsRule(Exact(groupName, moduleName), Prebuilts(Version(version))))
    }

    /**
     * docs for projects within [groupName] will be built from prebuilts with the given [version]
     */
    fun prebuilts(groupName: String, version: String) = prebuilts(groupName, Version(version))

    /**
     * docs for projects within [groupName] will be built from prebuilts with the given [version]
     */
    fun prebuilts(groupName: String, version: Version): Prebuilts {
        val strategy = Prebuilts(version)
        rules.add(DocsRule(Group(groupName), strategy))
        return strategy
    }

    /**
     * defines a default strategy for building docs
     */
    fun default(strategy: Strategy) {
        rules.add(DocsRule(All, strategy))
    }

    /**
     * docs for projects within [groupName] won't be built
     */
    fun ignore(groupName: String) {
        rules.add(DocsRule(Group(groupName), Ignore))
    }

    /**
     * docs for a specified project won't be built
     */
    fun ignore(groupName: String, name: String) {
        rules.add(DocsRule(Exact(groupName, name), Ignore))
    }

    fun build() = PublishDocsRules(name, rules)
}

sealed class ArtifactsPredicate {
    abstract fun apply(inGroup: String, inName: String): Boolean
    object All : ArtifactsPredicate() {
        override fun apply(inGroup: String, inName: String) = true
    }
    class Group(val group: String) : ArtifactsPredicate() {
        override fun apply(inGroup: String, inName: String) = inGroup == group
        override fun toString() = "\"$group\""
    }
    class Exact(val group: String, val name: String) : ArtifactsPredicate() {
        override fun apply(inGroup: String, inName: String) = group == inGroup && name == inName
        override fun toString() = "\"$group\", \"$name\""
    }
}

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

sealed class Strategy {
    object TipOfTree : Strategy()
    object Ignore : Strategy()
    class Prebuilts(val version: Version) : Strategy() {
        var stubs: MutableList<String>? = null
        fun addStubs(path: String) {
            if (stubs == null) {
                stubs = mutableListOf()
            }
            stubs!!.add(path)
        }

        override fun toString() = "Prebuilts(\"$version\")"
    }
}

class PublishDocsRules(val name: String, private val rules: List<DocsRule>) {
    fun resolve(groupName: String, moduleName: String): DocsRule {
        return rules.find { it.predicate.apply(groupName, moduleName) } ?: throw Error()
    }
}