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

import androidx.build.PublishDocsRules.Strategy.TipOfTree
import androidx.build.PublishDocsRules.Strategy.Prebuilts
import androidx.build.PublishDocsRules.Strategy.Ignore

val RELEASE_RULE = docsRules("public") {
    prebuilts(LibraryGroups.ARCH_CORE, "2.0.0-alpha1")
    prebuilts(LibraryGroups.WEAR, "1.0.0-alpha1")
            .addStubs("wear/wear_stubs/com.google.android.wearable-stubs.jar")
    prebuilts(LibraryGroups.CAR, "1.0.0-alpha1")
            .addStubs("car/car-stubs/android.car.jar")
    ignore(LibraryGroups.SLICE, "slice-builders-ktx")
    // navigation & workmanager don't have prebuilts currently
    tipOfTree(LibraryGroups.NAVIGATION)
    tipOfTree(LibraryGroups.WORKMANAGER)
    // lifecycle-viewmodel-ktx / lifecycle-process / lifecycle-service miss their prebuilts
    tipOfTree(LibraryGroups.LIFECYCLE, "lifecycle-viewmodel-ktx")
    tipOfTree(LibraryGroups.LIFECYCLE, "lifecycle-process")
    tipOfTree(LibraryGroups.LIFECYCLE, "lifecycle-service")
    // misses prebuilts, because it was released under different name in alpha1
    tipOfTree(LibraryGroups.CONTENTPAGER)
    prebuilts(LibraryGroups.LIFECYCLE, "2.0.0-alpha1")
    prebuilts(LibraryGroups.ROOM, "2.0.0-alpha1")
    prebuilts(LibraryGroups.PERSISTENCE, "2.0.0-alpha1")
    prebuilts(LibraryGroups.PAGING, "paging-rxjava2", "1.0.0-alpha1")
    prebuilts(LibraryGroups.PAGING, "2.0.0-alpha1")
    ignore(LibraryGroups.JETIFIER)
    ignore(LibraryGroups.CAR)
    ignore(LibraryGroups.CONTENTPAGER)
    ignore(LibraryGroups.VERSIONEDPARCELABLE)
    default(Prebuilts("1.0.0-alpha1"))
}

typealias ArtifactsPredicate = (String, String) -> Boolean

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

    private val rules: MutableList<Pair<ArtifactsPredicate, PublishDocsRules.Strategy>>
            = mutableListOf()

    private fun groupPredicate(name: String) = { group: String, _: String -> name == group }

    private fun artifactPredicate(group: String, name: String) = {
        inGroup: String, inName: String -> group == inGroup && name == inName }

    private val allPredicate = { _: String, _: String -> true }

    /**
     * docs for projects within [groupName] will be built from sources.
     */
    fun tipOfTree(groupName: String) {
        rules.add(groupPredicate(groupName) to TipOfTree)
    }

    /**
     * docs for a project with the given [groupName] and [name] will be built from sources.
     */
    fun tipOfTree(groupName: String, name: String) {
        rules.add(artifactPredicate(groupName, name) to TipOfTree)
    }

    /**
     * docs for a project with the given [groupName] and [name] will be built from a prebuilt with
     * the given [version].
     */
    fun prebuilts(groupName: String, moduleName: String , version: String) {
        rules.add(artifactPredicate(groupName, moduleName) to Prebuilts(Version(version)))
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
        rules.add(groupPredicate(groupName) to strategy)
        return strategy
    }

    /**
     * defines a default strategy for building docs
     */
    fun default(strategy: PublishDocsRules.Strategy) {
        rules.add(allPredicate to strategy)
    }

    /**
     * docs for projects within [groupName] won't be built
     */
    fun ignore(groupName: String) {
        rules.add(groupPredicate(groupName) to Ignore)
    }

    /**
     * docs for a specified project won't be built
     */
    fun ignore(groupName: String, name: String) {
        rules.add(artifactPredicate(groupName, name) to Ignore)
    }

    fun build() = PublishDocsRules(name, rules)
}

class PublishDocsRules(
        val name: String,
        private val rules: List<Pair<ArtifactsPredicate, Strategy>>
) {
    sealed class Strategy {
        object TipOfTree : Strategy()
        object Ignore : Strategy()
        class Prebuilts(val version: Version) : Strategy() {
            var stubs: MutableList<String>? = null
            constructor(version: String) : this(Version(version))
            fun addStubs(path: String) {
                if (stubs == null) {
                    stubs = mutableListOf()
                }
                stubs!!.add(path)
            }
        }
    }

    fun resolve(groupName: String, moduleName: String): Strategy {
        return rules.find { it.first(groupName, moduleName) }?.second ?: throw Error()
    }
}
