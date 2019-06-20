/*
 * Copyright 2017 The Android Open Source Project
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

/**
 * The list of maven group names of all the libraries in this project.
 */
object LibraryGroups {
    val ACTIVITY = LibraryGroup("androidx.activity")
    val ADS = LibraryGroup("androidx.ads", false)
    val ANIMATION = LibraryGroup("androidx.animation", false)
    val ANNOTATION = LibraryGroup("androidx.annotation")
    val APPCOMPAT = LibraryGroup("androidx.appcompat", false)
    val ARCH_CORE = LibraryGroup("androidx.arch.core")
    val ASYNCLAYOUTINFLATER = LibraryGroup("androidx.asynclayoutinflater")
    val AUTOFILL = LibraryGroup("androidx.autofill")
    val BIOMETRIC = LibraryGroup("androidx.biometric")
    val BROWSER = LibraryGroup("androidx.browser")
    val BENCHMARK = LibraryGroup("androidx.benchmark")
    val CAMERA = LibraryGroup("androidx.camera", false)
    val CAR = LibraryGroup("androidx.car", false)
    val CARDVIEW = LibraryGroup("androidx.cardview")
    val COLLECTION = LibraryGroup("androidx.collection")
    val CONCURRENT = LibraryGroup("androidx.concurrent")
    val CONTENTPAGER = LibraryGroup("androidx.contentpager")
    val COORDINATORLAYOUT = LibraryGroup("androidx.coordinatorlayout")
    val COMPOSE = LibraryGroup("androidx.compose", false)
    val CORE = LibraryGroup("androidx.core", false)
    val CURSORADAPTER = LibraryGroup("androidx.cursoradapter")
    val CUSTOMVIEW = LibraryGroup("androidx.customview")
    val DOCUMENTFILE = LibraryGroup("androidx.documentfile")
    val DRAWERLAYOUT = LibraryGroup("androidx.drawerlayout")
    val DYNAMICANIMATION = LibraryGroup("androidx.dynamicanimation", false)
    val EMOJI = LibraryGroup("androidx.emoji", false)
    val ENTERPRISE = LibraryGroup("androidx.enterprise")
    val EXIFINTERFACE = LibraryGroup("androidx.exifinterface")
    val FRAGMENT = LibraryGroup("androidx.fragment")
    val GRIDLAYOUT = LibraryGroup("androidx.gridlayout")
    val HEIFWRITER = LibraryGroup("androidx.heifwriter")
    val INSPECTION = LibraryGroup("androidx.inspection")
    val INTERPOLATOR = LibraryGroup("androidx.interpolator")
    val JETIFIER = LibraryGroup("com.android.tools.build.jetifier", false)
    val LEANBACK = LibraryGroup("androidx.leanback", false)
    val LEGACY = LibraryGroup("androidx.legacy", false)
    val LIFECYCLE = LibraryGroup("androidx.lifecycle", false)
    val LOADER = LibraryGroup("androidx.loader")
    val LOCALBROADCASTMANAGER = LibraryGroup("androidx.localbroadcastmanager")
    val MEDIA = LibraryGroup("androidx.media", false)
    val MEDIA2 = LibraryGroup("androidx.media2", false)
    val MEDIAROUTER = LibraryGroup("androidx.mediarouter")
    val NAVIGATION = LibraryGroup("androidx.navigation")
    val PAGING = LibraryGroup("androidx.paging")
    val PALETTE = LibraryGroup("androidx.palette")
    val PERCENTLAYOUT = LibraryGroup("androidx.percentlayout")
    val PERSISTENCE = LibraryGroup("androidx.sqlite")
    val PREFERENCE = LibraryGroup("androidx.preference")
    val PRINT = LibraryGroup("androidx.print")
    val RECOMMENDATION = LibraryGroup("androidx.recommendation")
    val RECYCLERVIEW = LibraryGroup("androidx.recyclerview", false)
    val REMOTECALLBACK = LibraryGroup("androidx.remotecallback")
    val ROOM = LibraryGroup("androidx.room")
    val SAVEDSTATE = LibraryGroup("androidx.savedstate")
    val SECURITY = LibraryGroup("androidx.security", false)
    val SHARETARGET = LibraryGroup("androidx.sharetarget")
    val SLICE = LibraryGroup("androidx.slice", false)
    val SLIDINGPANELAYOUT = LibraryGroup("androidx.slidingpanelayout")
    val SWIPEREFRESHLAYOUT = LibraryGroup("androidx.swiperefreshlayout")
    val TESTSCREENSHOT = LibraryGroup("androidx.test.screenshot")
    val TEXTCLASSIFIER = LibraryGroup("androidx.textclassifier")
    val TRANSITION = LibraryGroup("androidx.transition")
    val TVPROVIDER = LibraryGroup("androidx.tvprovider")
    val UI = LibraryGroup("androidx.ui", false)
    val VECTORDRAWABLE = LibraryGroup("androidx.vectordrawable", false)
    val VERSIONEDPARCELABLE = LibraryGroup("androidx.versionedparcelable", false)
    val VIEWPAGER = LibraryGroup("androidx.viewpager")
    val VIEWPAGER2 = LibraryGroup("androidx.viewpager2")
    val WEAR = LibraryGroup("androidx.wear")
    val WEBKIT = LibraryGroup("androidx.webkit")
    val WORK = LibraryGroup("androidx.work")
}

/**
 * This object contains the library group, as well as whether libraries
 * in this group are all required to have the same development version.
 */
data class LibraryGroup(val group: String = "unspecified", val requireSameVersion: Boolean = true)
