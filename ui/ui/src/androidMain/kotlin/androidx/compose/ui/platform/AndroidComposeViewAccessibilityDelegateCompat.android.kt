/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.content.Context
import android.graphics.RectF
import android.graphics.Region
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
import android.view.accessibility.AccessibilityNodeProvider
import androidx.annotation.DoNotInline
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.collection.ArraySet
import androidx.collection.SparseArrayCompat
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.R
import androidx.compose.ui.fastJoinToString
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.HitTestResult
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.OwnerScope
import androidx.compose.ui.platform.accessibility.hasCollectionInfo
import androidx.compose.ui.platform.accessibility.setCollectionInfo
import androidx.compose.ui.platform.accessibility.setCollectionItemInfo
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsActions.CustomActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertiesAndroid
import androidx.compose.ui.semantics.SemanticsEntity
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.outerSemantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.toAccessibilitySpannableString
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
import androidx.core.view.ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE
import androidx.core.view.accessibility.AccessibilityEventCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign

private fun LayoutNode.findClosestParentNode(selector: (LayoutNode) -> Boolean): LayoutNode? {
    var currentParent = this.parent
    while (currentParent != null) {
        if (selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parent
        }
    }

    return null
}

internal class AndroidComposeViewAccessibilityDelegateCompat(val view: AndroidComposeView) :
    AccessibilityDelegateCompat() {
    companion object {
        /** Virtual node identifier value for invalid nodes. */
        const val InvalidId = Integer.MIN_VALUE
        const val ClassName = "android.view.View"
        const val LogTag = "AccessibilityDelegate"
        const val ExtraDataTestTagKey = "androidx.compose.ui.semantics.testTag"

        /**
         * Intent size limitations prevent sending over a megabyte of data. Limit
         * text length to 100K characters - 200KB.
         */
        const val ParcelSafeTextLength = 100000
        /**
         * The undefined cursor position.
         */
        const val AccessibilityCursorPositionUndefined = -1
        // 20 is taken from AbsSeekbar.java.
        const val AccessibilitySliderStepsCount = 20
        /**
         * Delay before dispatching a recurring accessibility event in milliseconds.
         * This delay guarantees that a recurring event will be send at most once
         * during the [SendRecurringAccessibilityEventsIntervalMillis] time
         * frame.
         */
        const val SendRecurringAccessibilityEventsIntervalMillis: Long = 100

        /**
         * Timeout to determine whether a text selection changed event and the pending text
         * traversed event could be resulted from the same traverse action.
         */
        const val TextTraversedEventTimeoutMillis: Long = 1000
        private val AccessibilityActionsResourceIds = intArrayOf(
            R.id.accessibility_custom_action_0,
            R.id.accessibility_custom_action_1,
            R.id.accessibility_custom_action_2,
            R.id.accessibility_custom_action_3,
            R.id.accessibility_custom_action_4,
            R.id.accessibility_custom_action_5,
            R.id.accessibility_custom_action_6,
            R.id.accessibility_custom_action_7,
            R.id.accessibility_custom_action_8,
            R.id.accessibility_custom_action_9,
            R.id.accessibility_custom_action_10,
            R.id.accessibility_custom_action_11,
            R.id.accessibility_custom_action_12,
            R.id.accessibility_custom_action_13,
            R.id.accessibility_custom_action_14,
            R.id.accessibility_custom_action_15,
            R.id.accessibility_custom_action_16,
            R.id.accessibility_custom_action_17,
            R.id.accessibility_custom_action_18,
            R.id.accessibility_custom_action_19,
            R.id.accessibility_custom_action_20,
            R.id.accessibility_custom_action_21,
            R.id.accessibility_custom_action_22,
            R.id.accessibility_custom_action_23,
            R.id.accessibility_custom_action_24,
            R.id.accessibility_custom_action_25,
            R.id.accessibility_custom_action_26,
            R.id.accessibility_custom_action_27,
            R.id.accessibility_custom_action_28,
            R.id.accessibility_custom_action_29,
            R.id.accessibility_custom_action_30,
            R.id.accessibility_custom_action_31
        )
    }

    /** Virtual view id for the currently hovered logical item. */
    internal var hoveredVirtualViewId = InvalidId
    private val accessibilityManager: AccessibilityManager =
        view.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    internal var accessibilityForceEnabledForTesting = false
    private val isAccessibilityEnabled
        get() = accessibilityForceEnabledForTesting ||
            accessibilityManager.isEnabled &&
            accessibilityManager.isTouchExplorationEnabled
    private val handler = Handler(Looper.getMainLooper())
    private var nodeProvider: AccessibilityNodeProviderCompat =
        AccessibilityNodeProviderCompat(MyNodeProvider())
    private var focusedVirtualViewId = InvalidId

    // For actionIdToId and labelToActionId, the keys are the virtualViewIds. The value of
    // actionIdToLabel holds assigned custom action id to custom action label mapping. The
    // value of labelToActionId holds custom action label to assigned custom action id mapping.
    private var actionIdToLabel = SparseArrayCompat<SparseArrayCompat<CharSequence>>()
    private var labelToActionId = SparseArrayCompat<Map<CharSequence, Int>>()
    private var accessibilityCursorPosition = AccessibilityCursorPositionUndefined
    // We hold this node id to reset the [accessibilityCursorPosition] to undefined when
    // traversal with granularity switches to the next node
    private var previousTraversedNode: Int? = null
    private val subtreeChangedLayoutNodes = ArraySet<LayoutNode>()
    private val boundsUpdateChannel = Channel<Unit>(Channel.CONFLATED)
    private var currentSemanticsNodesInvalidated = true

    private class PendingTextTraversedEvent(
        val node: SemanticsNode,
        val action: Int,
        val granularity: Int,
        val fromIndex: Int,
        val toIndex: Int,
        val traverseTime: Long
    )

    private var pendingTextTraversedEvent: PendingTextTraversedEvent? = null

    /**
     * Up to date semantics nodes in pruned semantics tree. It always reflects the current
     * semantics tree. They key is the virtual view id(the root node has a key of
     * AccessibilityNodeProviderCompat.HOST_VIEW_ID and other node has a key of its id).
     */
    private var currentSemanticsNodes: Map<Int, SemanticsNodeWithAdjustedBounds> = mapOf()
        get() {
            if (currentSemanticsNodesInvalidated) {
                field = view.semanticsOwner.getAllUncoveredSemanticsNodesToMap()
                currentSemanticsNodesInvalidated = false
            }
            return field
        }
    private var paneDisplayed = ArraySet<Int>()

    /**
     * A snapshot of the semantics node. The children here is fixed and are taken from the time
     * this node is constructed. While a SemanticsNode always contains the up-to-date children.
     */
    @VisibleForTesting
    internal class SemanticsNodeCopy(
        semanticsNode: SemanticsNode,
        currentSemanticsNodes: Map<Int, SemanticsNodeWithAdjustedBounds>
    ) {
        val unmergedConfig = semanticsNode.unmergedConfig
        val children: MutableSet<Int> = mutableSetOf()

        init {
            semanticsNode.replacedChildren.fastForEach { child ->
                if (currentSemanticsNodes.contains(child.id)) {
                    children.add(child.id)
                }
            }
        }

        fun hasPaneTitle() = unmergedConfig.contains(SemanticsProperties.PaneTitle)
    }

    // previousSemanticsNodes holds the previous pruned semantics tree so that we can compare the
    // current and previous trees in onSemanticsChange(). We use SemanticsNodeCopy here because
    // SemanticsNode's children are dynamically generated and always reflect the current children.
    // We need to keep a copy of its old structure for comparison.
    @VisibleForTesting
    internal var previousSemanticsNodes: MutableMap<Int, SemanticsNodeCopy> = mutableMapOf()
    private var previousSemanticsRoot =
        SemanticsNodeCopy(view.semanticsOwner.unmergedRootSemanticsNode, mapOf())
    private var checkingForSemanticsChanges = false

    init {
        // Remove callbacks that rely on view being attached to a window when we become detached.
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {}
            override fun onViewDetachedFromWindow(view: View) {
                handler.removeCallbacks(semanticsChangeChecker)
            }
        })
    }

    /**
     * Returns true if there is any semantics node in the tree that can scroll in the given
     * [orientation][vertical] and [direction] at the given [position] in the view associated with
     * this delegate.
     *
     * @param direction The direction to check for scrolling: <0 means scrolling left or up, >0
     * means scrolling right or down.
     * @param position The position in the view to check in view-local coordinates.
     */
    internal fun canScroll(
        vertical: Boolean,
        direction: Int,
        position: Offset
    ): Boolean = canScroll(currentSemanticsNodes.values, vertical, direction, position)

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun canScroll(
        currentSemanticsNodes: Collection<SemanticsNodeWithAdjustedBounds>,
        vertical: Boolean,
        direction: Int,
        position: Offset
    ): Boolean {
        // No down event has occurred yet which gives us a location to hit test.
        if (position == Offset.Unspecified || !position.isValid()) return false

        val scrollRangeProperty = when (vertical) {
            true -> SemanticsProperties.VerticalScrollAxisRange
            false -> SemanticsProperties.HorizontalScrollAxisRange
        }

        return currentSemanticsNodes.any { node ->
            // Only consider nodes that are under the touch event. Checks the adjusted bounds to
            // avoid overlapping siblings. Because position is a float (touch event can happen in-
            // between pixels), convert the int-based Android Rect to a float-based Compose Rect
            // before doing the comparison.
            if (!node.adjustedBounds.toComposeRect().contains(position)) {
                return@any false
            }

            val scrollRange = node.semanticsNode.config.getOrNull(scrollRangeProperty)
                ?: return@any false

            // A node simply having scrollable semantics doesn't mean it's necessarily scrollable
            // in the given direction – it must also not be scrolled to its limit in that direction.
            var actualDirection = if (scrollRange.reverseScrolling) -direction else direction
            if (direction == 0 && scrollRange.reverseScrolling) {
                // The View implementation of canScroll* treat zero as a positive direction, so
                // this code should do the same. That means if scrolling is reversed, zero should be
                // a negative direction. The actual number doesn't matter, just its sign.
                actualDirection = -1
            }

            if (actualDirection < 0) scrollRange.value() > 0
            else scrollRange.value() < scrollRange.maxValue()
        }
    }

    private fun createNodeInfo(virtualViewId: Int): AccessibilityNodeInfo? {
        if (view.viewTreeOwners?.lifecycleOwner?.lifecycle?.currentState ==
            Lifecycle.State.DESTROYED
        ) {
            return null
        }
        val info: AccessibilityNodeInfoCompat = AccessibilityNodeInfoCompat.obtain()
        val semanticsNodeWithAdjustedBounds = currentSemanticsNodes[virtualViewId]
        if (semanticsNodeWithAdjustedBounds == null) {
            info.recycle()
            return null
        }
        val semanticsNode: SemanticsNode = semanticsNodeWithAdjustedBounds.semanticsNode
        if (virtualViewId == AccessibilityNodeProviderCompat.HOST_VIEW_ID) {
            info.setParent(ViewCompat.getParentForAccessibility(view) as? View)
        } else {
            if (semanticsNode.parent != null) {
                var parentId = semanticsNode.parent!!.id
                if (parentId == view.semanticsOwner.unmergedRootSemanticsNode.id) {
                    parentId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
                info.setParent(view, parentId)
            } else {
                throw IllegalStateException("semanticsNode $virtualViewId has null parent")
            }
        }
        info.setSource(view, virtualViewId)
        val boundsInRoot = semanticsNodeWithAdjustedBounds.adjustedBounds
        val topLeftInScreen =
            view.localToScreen(Offset(boundsInRoot.left.toFloat(), boundsInRoot.top.toFloat()))
        val bottomRightInScreen =
            view.localToScreen(Offset(boundsInRoot.right.toFloat(), boundsInRoot.bottom.toFloat()))
        info.setBoundsInScreen(
            android.graphics.Rect(
                floor(topLeftInScreen.x).toInt(),
                floor(topLeftInScreen.y).toInt(),
                ceil(bottomRightInScreen.x).toInt(),
                ceil(bottomRightInScreen.y).toInt()
            )
        )

        populateAccessibilityNodeInfoProperties(virtualViewId, info, semanticsNode)

        return info.unwrap()
    }

    @VisibleForTesting
    @OptIn(ExperimentalComposeUiApi::class)
    fun populateAccessibilityNodeInfoProperties(
        virtualViewId: Int,
        info: AccessibilityNodeInfoCompat,
        semanticsNode: SemanticsNode
    ) {
        // set classname
        info.className = ClassName
        val role = semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.Role)
        role?.let {
            if (semanticsNode.isFake || semanticsNode.replacedChildren.isEmpty()) {
                if (role == Role.Tab) {
                    info.roleDescription = view.context.resources.getString(R.string.tab)
                } else {
                    val className = when (it) {
                        Role.Button -> "android.widget.Button"
                        Role.Checkbox -> "android.widget.CheckBox"
                        Role.Switch -> "android.widget.Switch"
                        Role.RadioButton -> "android.widget.RadioButton"
                        Role.Image -> "android.widget.ImageView"
                        else -> null
                    }
                    if (role != Role.Image) {
                        info.className = className
                    } else {
                        // If ancestor of the image is a merging container (e.g. IconButton), we
                        // don't want to announce Image role to avoid "Favorite, Image, Button"
                        val ancestor = semanticsNode.layoutNode.findClosestParentNode { parent ->
                            parent.outerSemantics
                                ?.collapsedSemanticsConfiguration()
                                ?.isMergingSemanticsOfDescendants == true
                        }
                        if (ancestor == null ||
                            semanticsNode.unmergedConfig.isMergingSemanticsOfDescendants
                        ) {
                            info.className = className
                        }
                    }
                }
            }
        }
        if (semanticsNode.isTextField) {
            info.className = "android.widget.EditText"
        }
        if (semanticsNode.config.contains(SemanticsProperties.Text)) {
            info.className = "android.widget.TextView"
        }

        info.packageName = view.context.packageName

        // This property exists to distinguish semantically meaningful nodes from purely structural
        // or decorative UI elements. In Compose, LayoutNodes without semantics are simply omitted
        // from the AccessibilityNodeInfo tree. Therefore, every AccessibilityNodeInfo qualifies as
        // "important".
        info.isImportantForAccessibility = true

        semanticsNode.replacedChildrenSortedByBounds.fastForEach { child ->
            if (currentSemanticsNodes.contains(child.id)) {
                val holder = view.androidViewsHandler.layoutNodeToHolder[child.layoutNode]
                if (holder != null) {
                    info.addChild(holder)
                } else {
                    info.addChild(view, child.id)
                }
            }
        }

        // Manage internal accessibility focus state.
        if (focusedVirtualViewId == virtualViewId) {
            info.isAccessibilityFocused = true
            info.addAction(AccessibilityActionCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
        } else {
            info.isAccessibilityFocused = false
            info.addAction(AccessibilityActionCompat.ACTION_ACCESSIBILITY_FOCUS)
        }

        setText(semanticsNode, info)
        setContentInvalid(semanticsNode, info)

        info.stateDescription =
            semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.StateDescription)
        val toggleState = semanticsNode.unmergedConfig.getOrNull(
            SemanticsProperties.ToggleableState
        )
        toggleState?.let {
            info.isCheckable = true
            when (it) {
                ToggleableState.On -> {
                    info.isChecked = true
                    // Unfortunately, talback has a bug of using "checked", so we set state
                    // description here
                    if (role == Role.Switch && info.stateDescription == null) {
                        info.stateDescription = view.context.resources.getString(R.string.on)
                    }
                }
                ToggleableState.Off -> {
                    info.isChecked = false
                    // Unfortunately, talkback has a bug of using "not checked", so we set state
                    // description here
                    if (role == Role.Switch && info.stateDescription == null) {
                        info.stateDescription = view.context.resources.getString(R.string.off)
                    }
                }
                ToggleableState.Indeterminate -> {
                    if (info.stateDescription == null) {
                        info.stateDescription =
                            view.context.resources.getString(R.string.indeterminate)
                    }
                }
            }
        }
        semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.Selected)?.let {
            if (role == Role.Tab) {
                // Tab in native android uses selected property
                info.isSelected = it
            } else {
                info.isCheckable = true
                info.isChecked = it
                if (info.stateDescription == null) {
                    // If a radio entry (radio button + text) is selectable, it won't have the role
                    // RadioButton, so if we use info.isCheckable/info.isChecked, talkback will say
                    // "checked/not checked" instead "selected/note selected".
                    info.stateDescription = if (it) {
                        view.context.resources.getString(R.string.selected)
                    } else {
                        view.context.resources.getString(R.string.not_selected)
                    }
                }
            }
        }

        if (!semanticsNode.unmergedConfig.isMergingSemanticsOfDescendants ||
            // we don't emit fake nodes for nodes without children, therefore we should assign
            // content description for such nodes
            semanticsNode.replacedChildren.isEmpty()
        ) {
            info.contentDescription = semanticsNode.unmergedConfig.getOrNull(
                SemanticsProperties.ContentDescription
            )?.firstOrNull()
        }

        if (semanticsNode.unmergedConfig.isMergingSemanticsOfDescendants) {
            info.isScreenReaderFocusable = true
        }

        // Map testTag to resourceName if testTagsAsResourceId == true (which can be set by an ancestor)
        val testTag = semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.TestTag)
        if (testTag != null) {
            var testTagsAsResourceId = false
            var current: SemanticsNode? = semanticsNode
            while (current != null) {
                if (current.unmergedConfig.contains(
                    SemanticsPropertiesAndroid.TestTagsAsResourceId
                )) {
                    testTagsAsResourceId = current.unmergedConfig.get(
                        SemanticsPropertiesAndroid.TestTagsAsResourceId
                    )
                    break
                } else {
                    current = current.parent
                }
            }

            if (testTagsAsResourceId) {
                info.viewIdResourceName = testTag
            }
        }

        semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.Heading)?.let {
            info.isHeading = true
        }
        info.isPassword = semanticsNode.isPassword
        // Note editable is not added to semantics properties api.
        info.isEditable = semanticsNode.isTextField
        info.isEnabled = semanticsNode.enabled()
        info.isFocusable = semanticsNode.unmergedConfig.contains(SemanticsProperties.Focused)
        if (info.isFocusable) {
            info.isFocused = semanticsNode.unmergedConfig[SemanticsProperties.Focused]
            if (info.isFocused) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS)
            } else {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_FOCUS)
            }
        }

        // Mark invisible nodes
        val wrapperToCheckTransparency = if (semanticsNode.isFake) {
            // when node is fake, its parent that is the original semantics node should define the
            // alpha value
            semanticsNode.parent?.findWrapperToGetBounds()
        } else {
            semanticsNode.findWrapperToGetBounds()
        }
        val isTransparent = wrapperToCheckTransparency?.isTransparent() ?: false
        info.isVisibleToUser = !isTransparent &&
            semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.InvisibleToUser) == null

        semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.LiveRegion)?.let {
            info.liveRegion = when (it) {
                LiveRegionMode.Polite -> ACCESSIBILITY_LIVE_REGION_POLITE
                LiveRegionMode.Assertive -> ACCESSIBILITY_LIVE_REGION_ASSERTIVE
                else -> ACCESSIBILITY_LIVE_REGION_POLITE
            }
        }
        info.isClickable = false
        semanticsNode.unmergedConfig.getOrNull(SemanticsActions.OnClick)?.let {
            // Selectable items that are already selected should not announce it again
            val isSelected =
                semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.Selected) == true
            info.isClickable = !isSelected
            if (semanticsNode.enabled() && !isSelected) {
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        it.label
                    )
                )
            }
        }
        info.isLongClickable = false
        semanticsNode.unmergedConfig.getOrNull(SemanticsActions.OnLongClick)?.let {
            info.isLongClickable = true
            if (semanticsNode.enabled()) {
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_LONG_CLICK,
                        it.label
                    )
                )
            }
        }

        // The config will contain this action only if there is a text selection at the moment.
        semanticsNode.unmergedConfig.getOrNull(SemanticsActions.CopyText)?.let {
            info.addAction(
                AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_COPY,
                    it.label
                )
            )
        }
        if (semanticsNode.enabled()) {
            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.SetText)?.let {
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SET_TEXT,
                        it.label
                    )
                )
            }

            // The config will contain this action only if there is a text selection at the moment.
            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.CutText)?.let {
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CUT,
                        it.label
                    )
                )
            }

            // The config will contain the action anyway, therefore we check the clipboard text to
            // decide whether to add the action to the node or not.
            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.PasteText)?.let {
                if (info.isFocused && view.clipboardManager.hasText()) {
                    info.addAction(
                        AccessibilityActionCompat(
                            AccessibilityNodeInfoCompat.ACTION_PASTE,
                            it.label
                        )
                    )
                }
            }
        }

        val text = getIterableTextForAccessibility(semanticsNode)
        if (!text.isNullOrEmpty()) {
            info.setTextSelection(
                getAccessibilitySelectionStart(semanticsNode),
                getAccessibilitySelectionEnd(semanticsNode)
            )
            val setSelectionAction =
                semanticsNode.unmergedConfig.getOrNull(SemanticsActions.SetSelection)
            // ACTION_SET_SELECTION should be provided even when SemanticsActions.SetSelection
            // semantics action is not provided by the component
            info.addAction(
                AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_SET_SELECTION,
                    setSelectionAction?.label
                )
            )
            info.addAction(AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY)
            info.addAction(AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY)
            info.movementGranularities =
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER or
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD or
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH
            // We only traverse the text when contentDescription is not set.
            val contentDescription = semanticsNode.unmergedConfig.getOrNull(
                SemanticsProperties.ContentDescription
            )
            if (contentDescription.isNullOrEmpty() &&
                semanticsNode.unmergedConfig.contains(SemanticsActions.GetTextLayoutResult) &&
                // Talkback does not handle below granularities for text field (which includes
                // label/hint) when text field is not in focus
                !semanticsNode.excludeLineAndPageGranularities()
            ) {
                info.movementGranularities = info.movementGranularities or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val extraDataKeys: MutableList<String> = mutableListOf()
            if (!info.text.isNullOrEmpty() &&
                semanticsNode.unmergedConfig.contains(SemanticsActions.GetTextLayoutResult)
            ) {
                extraDataKeys.add(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY)
            }
            if (semanticsNode.unmergedConfig.contains(SemanticsProperties.TestTag)) {
                extraDataKeys.add(ExtraDataTestTagKey)
            }

            if (!extraDataKeys.isEmpty()) {
                AccessibilityNodeInfoVerificationHelperMethods.setAvailableExtraData(
                    info.unwrap(),
                    extraDataKeys
                )
            }
        }

        val rangeInfo =
            semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
        if (rangeInfo != null) {
            if (semanticsNode.unmergedConfig.contains(SemanticsActions.SetProgress)) {
                info.className = "android.widget.SeekBar"
            } else {
                info.className = "android.widget.ProgressBar"
            }
            if (rangeInfo !== ProgressBarRangeInfo.Indeterminate) {
                info.rangeInfo = AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
                    AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT,
                    rangeInfo.range.start,
                    rangeInfo.range.endInclusive,
                    rangeInfo.current
                )
                // let's set state description here and use state description change events.
                // otherwise, we need to send out type_view_selected event, as the old android
                // versions do. But the support for type_view_selected event for progress bars
                // maybe deprecated in talkback in the future.
                if (info.stateDescription == null) {
                    val valueRange = rangeInfo.range
                    val progress = (
                        if (valueRange.endInclusive - valueRange.start == 0f) 0f
                        else (rangeInfo.current - valueRange.start) /
                            (valueRange.endInclusive - valueRange.start)
                        ).coerceIn(0f, 1f)

                    // We only display 0% or 100% when it is exactly 0% or 100%.
                    val percent = when (progress) {
                        0f -> 0
                        1f -> 100
                        else -> (progress * 100).roundToInt().coerceIn(1, 99)
                    }
                    info.stateDescription =
                        view.context.resources.getString(R.string.template_percent, percent)
                }
            } else if (info.stateDescription == null) {
                info.stateDescription = view.context.resources.getString(R.string.in_progress)
            }
            if (semanticsNode.unmergedConfig.contains(SemanticsActions.SetProgress) &&
                semanticsNode.enabled()
            ) {
                if (rangeInfo.current <
                    rangeInfo.range.endInclusive.coerceAtLeast(rangeInfo.range.start)
                ) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD)
                }
                if (rangeInfo.current >
                    rangeInfo.range.start.coerceAtMost(rangeInfo.range.endInclusive)
                ) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Api24Impl.addSetProgressAction(info, semanticsNode)
        }

        setCollectionInfo(semanticsNode, info)
        setCollectionItemInfo(semanticsNode, info)

        // Will the scrollable scroll when ACTION_SCROLL_FORWARD is performed?
        fun ScrollAxisRange.canScrollForward(): Boolean {
            return value() < maxValue() && !reverseScrolling || value() > 0f && reverseScrolling
        }
        // Will the scrollable scroll when ACTION_SCROLL_BACKWARD is performed?
        fun ScrollAxisRange.canScrollBackward(): Boolean {
            return value() > 0f && !reverseScrolling || value() < maxValue() && reverseScrolling
        }

        val xScrollState =
            semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)
        val scrollAction = semanticsNode.unmergedConfig.getOrNull(SemanticsActions.ScrollBy)
        if (xScrollState != null && scrollAction != null) {
            // Talkback defines SCROLLABLE_ROLE_FILTER_FOR_DIRECTION_NAVIGATION, so we need to
            // assign a role for auto scroll to work. Node with collectionInfo resolved by
            // Talkback to ROLE_LIST and supports autoscroll too
            if (!semanticsNode.hasCollectionInfo()) {
                info.className = "android.widget.HorizontalScrollView"
            }
            if (xScrollState.maxValue() > 0f) {
                info.isScrollable = true
            }
            if (semanticsNode.enabled()) {
                if (xScrollState.canScrollForward()) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD)
                    info.addAction(
                        if (!semanticsNode.isRtl) {
                            AccessibilityActionCompat.ACTION_SCROLL_RIGHT
                        } else {
                            AccessibilityActionCompat.ACTION_SCROLL_LEFT
                        }
                    )
                }
                if (xScrollState.canScrollBackward()) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD)
                    info.addAction(
                        if (!semanticsNode.isRtl) {
                            AccessibilityActionCompat.ACTION_SCROLL_LEFT
                        } else {
                            AccessibilityActionCompat.ACTION_SCROLL_RIGHT
                        }
                    )
                }
            }
        }
        val yScrollState =
            semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        if (yScrollState != null && scrollAction != null) {
            // Talkback defines SCROLLABLE_ROLE_FILTER_FOR_DIRECTION_NAVIGATION, so we need to
            // assign a role for auto scroll to work. Node with collectionInfo resolved by
            // Talkback to ROLE_LIST and supports autoscroll too
            if (!semanticsNode.hasCollectionInfo()) {
                info.className = "android.widget.ScrollView"
            }
            if (yScrollState.maxValue() > 0f) {
                info.isScrollable = true
            }
            if (semanticsNode.enabled()) {
                if (yScrollState.canScrollForward()) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD)
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_DOWN)
                }
                if (yScrollState.canScrollBackward()) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD)
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_UP)
                }
            }
        }

        info.paneTitle = semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.PaneTitle)

        if (semanticsNode.enabled()) {
            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.Expand)?.let {
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_EXPAND,
                        it.label
                    )
                )
            }

            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.Collapse)?.let {
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_COLLAPSE,
                        it.label
                    )
                )
            }

            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.Dismiss)?.let {
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_DISMISS,
                        it.label
                    )
                )
            }

            if (semanticsNode.unmergedConfig.contains(CustomActions)) {
                val customActions = semanticsNode.unmergedConfig[CustomActions]
                if (customActions.size >= AccessibilityActionsResourceIds.size) {
                    throw IllegalStateException(
                        "Can't have more than " +
                            "${AccessibilityActionsResourceIds.size} custom actions for one widget"
                    )
                }
                val currentActionIdToLabel = SparseArrayCompat<CharSequence>()
                val currentLabelToActionId = mutableMapOf<CharSequence, Int>()
                // If this virtual node had custom action id assignment before, we try to keep the id
                // unchanged for the same action (identified by action label). This way, we can
                // minimize the influence of custom action change between custom actions are
                // presented to the user and actually performed.
                if (labelToActionId.containsKey(virtualViewId)) {
                    val oldLabelToActionId = labelToActionId[virtualViewId]
                    val availableIds = AccessibilityActionsResourceIds.toMutableList()
                    val unassignedActions = mutableListOf<CustomAccessibilityAction>()
                    customActions.fastForEach { action ->
                        if (oldLabelToActionId!!.contains(action.label)) {
                            val actionId = oldLabelToActionId[action.label]
                            currentActionIdToLabel.put(actionId!!, action.label)
                            currentLabelToActionId[action.label] = actionId
                            availableIds.remove(actionId)
                            info.addAction(AccessibilityActionCompat(actionId, action.label))
                        } else {
                            unassignedActions.add(action)
                        }
                    }
                    unassignedActions.fastForEachIndexed { index, action ->
                        val actionId = availableIds[index]
                        currentActionIdToLabel.put(actionId, action.label)
                        currentLabelToActionId[action.label] = actionId
                        info.addAction(AccessibilityActionCompat(actionId, action.label))
                    }
                } else {
                    customActions.fastForEachIndexed { index, action ->
                        val actionId = AccessibilityActionsResourceIds[index]
                        currentActionIdToLabel.put(actionId, action.label)
                        currentLabelToActionId[action.label] = actionId
                        info.addAction(AccessibilityActionCompat(actionId, action.label))
                    }
                }
                actionIdToLabel.put(virtualViewId, currentActionIdToLabel)
                labelToActionId.put(virtualViewId, currentLabelToActionId)
            }
        }
    }

    /** Set the error text for this node */
    private fun setContentInvalid(node: SemanticsNode, info: AccessibilityNodeInfoCompat) {
        if (node.unmergedConfig.contains(SemanticsProperties.Error)) {
            info.isContentInvalid = true
            info.error = node.unmergedConfig.getOrNull(SemanticsProperties.Error)
        }
    }

    @OptIn(InternalTextApi::class)
    private fun setText(
        node: SemanticsNode,
        info: AccessibilityNodeInfoCompat,
    ) {
        val fontFamilyResolver: FontFamily.Resolver = view.fontFamilyResolver
        val editableTextToAssign = trimToSize(
            node.unmergedConfig.getTextForTextField()
                ?.toAccessibilitySpannableString(density = view.density, fontFamilyResolver),
            ParcelSafeTextLength
        )

        val textToAssign = trimToSize(
            node.unmergedConfig.getOrNull(SemanticsProperties.Text)?.firstOrNull()
                ?.toAccessibilitySpannableString(density = view.density, fontFamilyResolver),
            ParcelSafeTextLength
        )

        info.text = editableTextToAssign ?: textToAssign
    }

    /**
     * Returns whether this virtual view is accessibility focused.
     *
     * @return True if the view is accessibility focused.
     */
    private fun isAccessibilityFocused(virtualViewId: Int): Boolean {
        return (focusedVirtualViewId == virtualViewId)
    }

    /**
     * Attempts to give accessibility focus to a virtual view.
     * <p>
     * A virtual view will not actually take focus if
     * {@link AccessibilityManager#isEnabled()} returns false,
     * {@link AccessibilityManager#isTouchExplorationEnabled()} returns false,
     * or the view already has accessibility focus.
     *
     * @param virtualViewId The id of the virtual view on which to place
     *            accessibility focus.
     * @return Whether this virtual view actually took accessibility focus.
     */
    private fun requestAccessibilityFocus(virtualViewId: Int): Boolean {
        if (!isAccessibilityEnabled) {
            return false
        }
        // TODO: Check virtual view visibility.
        if (!isAccessibilityFocused(virtualViewId)) {
            // Clear focus from the previously focused view, if applicable.
            if (focusedVirtualViewId != InvalidId) {
                sendEventForVirtualView(
                    focusedVirtualViewId,
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED
                )
            }

            // Set focus on the new view.
            focusedVirtualViewId = virtualViewId

            view.invalidate()
            sendEventForVirtualView(
                virtualViewId,
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
            )
            return true
        }
        return false
    }

    /**
     * Populates an event of the specified type with information about an item
     * and attempts to send it up through the view hierarchy.
     * <p>
     * You should call this method after performing a user action that normally
     * fires an accessibility event, such as clicking on an item.
     *
     * <pre>public performItemClick(T item) {
     *   ...
     *   sendEventForVirtualView(item.id, AccessibilityEvent.TYPE_VIEW_CLICKED)
     * }
     * </pre>
     *
     * @param virtualViewId The virtual view id for which to send an event.
     * @param eventType The type of event to send.
     * @param contentChangeType The contentChangeType of this event.
     * @param contentDescription Content description of this event.
     * @return true if the event was sent successfully.
     */
    private fun sendEventForVirtualView(
        virtualViewId: Int,
        eventType: Int,
        contentChangeType: Int? = null,
        contentDescription: List<String>? = null
    ): Boolean {
        if (virtualViewId == InvalidId || !isAccessibilityEnabled) {
            return false
        }

        val event: AccessibilityEvent = createEvent(virtualViewId, eventType)
        if (contentChangeType != null) {
            event.contentChangeTypes = contentChangeType
        }
        if (contentDescription != null) {
            event.contentDescription = contentDescription.fastJoinToString(",")
        }

        return sendEvent(event)
    }

    /**
     * Send an accessibility event.
     *
     * @param event The accessibility event to send.
     * @return true if the event was sent successfully.
     */
    private fun sendEvent(event: AccessibilityEvent): Boolean {
        if (!isAccessibilityEnabled) {
            return false
        }

        return view.parent.requestSendAccessibilityEvent(view, event)
    }

    /**
     * Constructs and returns an {@link AccessibilityEvent} populated with
     * information about the specified item.
     *
     * @param virtualViewId The virtual view id for the item for which to
     *            construct an event.
     * @param eventType The type of event to construct.
     * @return An {@link AccessibilityEvent} populated with information about
     *         the specified item.
     */
    @Suppress("DEPRECATION")
    @VisibleForTesting
    internal fun createEvent(virtualViewId: Int, eventType: Int): AccessibilityEvent {
        val event: AccessibilityEvent = AccessibilityEvent.obtain(eventType)
        event.isEnabled = true
        event.className = ClassName

        // Don't allow the client to override these properties.
        event.packageName = view.context.packageName
        event.setSource(view, virtualViewId)

        // populate additional information from the node
        currentSemanticsNodes[virtualViewId]?.let {
            event.isPassword = it.semanticsNode.isPassword
        }

        return event
    }

    private fun createTextSelectionChangedEvent(
        virtualViewId: Int,
        fromIndex: Int?,
        toIndex: Int?,
        itemCount: Int?,
        text: String?
    ): AccessibilityEvent {
        return createEvent(
            virtualViewId,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        ).apply {
            fromIndex?.let { this.fromIndex = it }
            toIndex?.let { this.toIndex = it }
            itemCount?.let { this.itemCount = it }
            text?.let { this.text.add(it) }
        }
    }

    /**
     * Attempts to clear accessibility focus from a virtual view.
     *
     * @param virtualViewId The id of the virtual view from which to clear
     *            accessibility focus.
     * @return Whether this virtual view actually cleared accessibility focus.
     */
    private fun clearAccessibilityFocus(virtualViewId: Int): Boolean {
        if (isAccessibilityFocused(virtualViewId)) {
            focusedVirtualViewId = InvalidId
            view.invalidate()
            sendEventForVirtualView(
                virtualViewId,
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED
            )
            return true
        }
        return false
    }

    private fun performActionHelper(
        virtualViewId: Int,
        action: Int,
        arguments: Bundle?
    ): Boolean {
        val node = currentSemanticsNodes[virtualViewId]?.semanticsNode ?: return false

        // Actions can be performed when disabled.
        when (action) {
            AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS ->
                return requestAccessibilityFocus(virtualViewId)
            AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS ->
                return clearAccessibilityFocus(virtualViewId)
            AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
            AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY -> {
                if (arguments != null) {
                    val granularity = arguments.getInt(
                        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT
                    )
                    val extendSelection = arguments.getBoolean(
                        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
                    )
                    return traverseAtGranularity(
                        node, granularity,
                        action == AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                        extendSelection
                    )
                }
                return false
            }
            AccessibilityNodeInfoCompat.ACTION_SET_SELECTION -> {
                val start = arguments?.getInt(
                    AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT, -1
                ) ?: -1
                val end = arguments?.getInt(
                    AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT, -1
                ) ?: -1
                // Note: This is a little different from current android framework implementation.
                val success = setAccessibilitySelection(node, start, end, false)
                // Text selection changed event already updates the cache. so this may not be
                // necessary.
                if (success) {
                    sendEventForVirtualView(
                        semanticsNodeIdToAccessibilityVirtualNodeId(node.id),
                        AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                    )
                }
                return success
            }
            AccessibilityNodeInfoCompat.ACTION_COPY -> {
                return node.unmergedConfig.getOrNull(
                    SemanticsActions.CopyText
                )?.action?.invoke() ?: false
            }
        }

        if (!node.enabled()) {
            return false
        }

        // Actions can't be performed when disabled.
        when (action) {
            AccessibilityNodeInfoCompat.ACTION_CLICK -> {
                val result =
                    node.unmergedConfig.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
                sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
                return result ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_LONG_CLICK -> {
                return node.unmergedConfig.getOrNull(SemanticsActions.OnLongClick)?.action?.invoke()
                    ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD,
            AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD,
            android.R.id.accessibilityActionScrollDown,
            android.R.id.accessibilityActionScrollUp,
            android.R.id.accessibilityActionScrollRight,
            android.R.id.accessibilityActionScrollLeft -> {
                // Introduce a few shorthands:
                val scrollForward = action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
                val scrollBackward = action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
                val scrollLeft = action == android.R.id.accessibilityActionScrollLeft
                val scrollRight = action == android.R.id.accessibilityActionScrollRight
                val scrollUp = action == android.R.id.accessibilityActionScrollUp
                val scrollDown = action == android.R.id.accessibilityActionScrollDown

                val scrollHorizontal = scrollLeft || scrollRight || scrollForward || scrollBackward
                val scrollVertical = scrollUp || scrollDown || scrollForward || scrollBackward

                if (scrollForward || scrollBackward) {
                    val rangeInfo =
                        node.unmergedConfig.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
                    val setProgressAction =
                        node.unmergedConfig.getOrNull(SemanticsActions.SetProgress)
                    if (rangeInfo != null && setProgressAction != null) {
                        val max = rangeInfo.range.endInclusive.coerceAtLeast(rangeInfo.range.start)
                        val min = rangeInfo.range.start.coerceAtMost(rangeInfo.range.endInclusive)
                        var increment = if (rangeInfo.steps > 0) {
                            (max - min) / (rangeInfo.steps + 1)
                        } else {
                            (max - min) / AccessibilitySliderStepsCount
                        }
                        if (scrollBackward) {
                            increment = -increment
                        }
                        return setProgressAction.action?.invoke(rangeInfo.current + increment)
                            ?: false
                    }
                }

                // Will the scrollable scroll when ScrollBy is invoked with the given [amount]?
                fun ScrollAxisRange.canScroll(amount: Float): Boolean {
                    return amount < 0 && value() > 0 || amount > 0 && value() < maxValue()
                }

                val viewport = node.layoutInfo.coordinates.boundsInParent().size
                val scrollAction =
                    node.unmergedConfig.getOrNull(SemanticsActions.ScrollBy) ?: return false

                val xScrollState =
                    node.unmergedConfig.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)
                if (xScrollState != null && scrollHorizontal) {
                    var amountToScroll = viewport.width
                    if (scrollLeft || scrollBackward) {
                        amountToScroll = -amountToScroll
                    }
                    if (xScrollState.reverseScrolling) {
                        amountToScroll = -amountToScroll
                    }
                    if (node.isRtl && (scrollLeft || scrollRight)) {
                        amountToScroll = -amountToScroll
                    }
                    if (xScrollState.canScroll(amountToScroll)) {
                        return scrollAction.action?.invoke(amountToScroll, 0f) ?: false
                    }
                }

                val yScrollState =
                    node.unmergedConfig.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
                if (yScrollState != null && scrollVertical) {
                    var amountToScroll = viewport.height
                    if (scrollUp || scrollBackward) {
                        amountToScroll = -amountToScroll
                    }
                    if (yScrollState.reverseScrolling) {
                        amountToScroll = -amountToScroll
                    }
                    if (yScrollState.canScroll(amountToScroll)) {
                        return scrollAction.action?.invoke(0f, amountToScroll) ?: false
                    }
                }

                return false
            }
            android.R.id.accessibilityActionSetProgress -> {
                if (arguments == null || !arguments.containsKey(
                        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE
                    )
                ) {
                    return false
                }
                return node.unmergedConfig.getOrNull(SemanticsActions.SetProgress)?.action?.invoke(
                    arguments.getFloat(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE)
                ) ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_FOCUS -> {
                return node.unmergedConfig.getOrNull(SemanticsActions.RequestFocus)
                    ?.action?.invoke() ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS -> {
                return if (node.unmergedConfig.getOrNull(SemanticsProperties.Focused) == true) {
                    view.focusManager.clearFocus()
                    true
                } else {
                    false
                }
            }
            AccessibilityNodeInfoCompat.ACTION_SET_TEXT -> {
                val text = arguments?.getString(
                    AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE
                )
                return node.unmergedConfig.getOrNull(SemanticsActions.SetText)
                    ?.action?.invoke(AnnotatedString(text ?: "")) ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_PASTE -> {
                return node.unmergedConfig.getOrNull(
                    SemanticsActions.PasteText
                )?.action?.invoke() ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_CUT -> {
                return node.unmergedConfig.getOrNull(
                    SemanticsActions.CutText
                )?.action?.invoke() ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_EXPAND -> {
                return node.unmergedConfig.getOrNull(
                    SemanticsActions.Expand
                )?.action?.invoke() ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_COLLAPSE -> {
                return node.unmergedConfig.getOrNull(
                    SemanticsActions.Collapse
                )?.action?.invoke() ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_DISMISS -> {
                return node.unmergedConfig.getOrNull(
                    SemanticsActions.Dismiss
                )?.action?.invoke() ?: false
            }
            android.R.id.accessibilityActionShowOnScreen -> {
                // TODO(b/190865803): Consider scrolling nested containers instead of only the first one.
                var scrollableAncestor: SemanticsNode? = node.parent
                var scrollAction = scrollableAncestor?.config?.getOrNull(SemanticsActions.ScrollBy)
                while (scrollableAncestor != null) {
                    if (scrollAction != null) {
                        break
                    }
                    scrollableAncestor = scrollableAncestor.parent
                    scrollAction = scrollableAncestor?.config?.getOrNull(SemanticsActions.ScrollBy)
                }
                if (scrollableAncestor == null) {
                    return false
                }

                // TalkBack expects the minimum amount of movement to fully reveal the node.
                // First, get the viewport and the target bounds in root coordinates
                val viewportInParent = scrollableAncestor.layoutInfo.coordinates.boundsInParent()
                val parentInRoot = scrollableAncestor.layoutInfo.coordinates
                    .parentLayoutCoordinates?.positionInRoot() ?: Offset.Zero
                val viewport = viewportInParent.translate(parentInRoot)
                val target = Rect(node.positionInRoot, node.size.toSize())

                val xScrollState = scrollableAncestor.unmergedConfig
                    .getOrNull(SemanticsProperties.HorizontalScrollAxisRange)
                val yScrollState = scrollableAncestor.unmergedConfig
                    .getOrNull(SemanticsProperties.VerticalScrollAxisRange)

                // Given the desired scroll value to align either side of the target with the
                // viewport, what delta should we go with?
                // If we need to scroll in opposite directions for both sides, don't scroll at all.
                // Otherwise, take the delta that scrolls the least amount.
                fun scrollDelta(a: Float, b: Float): Float =
                    if (sign(a) == sign(b)) if (abs(a) < abs(b)) a else b else 0f

                // Get the desired delta X
                var dx = scrollDelta(target.left - viewport.left, target.right - viewport.right)
                // And adjust for reversing properties
                if (xScrollState?.reverseScrolling == true) dx = -dx
                if (node.isRtl) dx = -dx

                // Get the desired delta Y
                var dy = scrollDelta(target.top - viewport.top, target.bottom - viewport.bottom)
                // And adjust for reversing properties
                if (yScrollState?.reverseScrolling == true) dy = -dy

                return scrollAction?.action?.invoke(dx, dy) ?: false
            }
            // TODO: handling for other system actions
            else -> {
                val label = actionIdToLabel[virtualViewId]?.get(action) ?: return false
                val customActions = node.unmergedConfig.getOrNull(CustomActions) ?: return false
                customActions.fastForEach { customAction ->
                    if (customAction.label == label) {
                        return customAction.action()
                    }
                }
                return false
            }
        }
    }

    private fun addExtraDataToAccessibilityNodeInfoHelper(
        virtualViewId: Int,
        info: AccessibilityNodeInfo,
        extraDataKey: String,
        arguments: Bundle?
    ) {
        val node = currentSemanticsNodes[virtualViewId]?.semanticsNode ?: return
        val text = getIterableTextForAccessibility(node)
        if (node.unmergedConfig.contains(SemanticsActions.GetTextLayoutResult) &&
            arguments != null && extraDataKey == EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
        ) {
            val positionInfoStartIndex = arguments.getInt(
                EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, -1
            )
            val positionInfoLength = arguments.getInt(
                EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, -1
            )
            if ((positionInfoLength <= 0) || (positionInfoStartIndex < 0) ||
                (positionInfoStartIndex >= text?.length ?: Int.MAX_VALUE)
            ) {
                Log.e(LogTag, "Invalid arguments for accessibility character locations")
                return
            }
            val textLayoutResults = mutableListOf<TextLayoutResult>()
            // Note now it only works for single Text/TextField until we fix b/157474582.
            val getLayoutResult = node.unmergedConfig[SemanticsActions.GetTextLayoutResult]
                .action?.invoke(textLayoutResults)
            val textLayoutResult: TextLayoutResult
            if (getLayoutResult == true) {
                textLayoutResult = textLayoutResults[0]
            } else {
                return
            }
            val boundingRects = mutableListOf<RectF?>()
            for (i in 0 until positionInfoLength) {
                // This is a workaround until we fix the merging issue in b/157474582.
                if (positionInfoStartIndex + i >= textLayoutResult.layoutInput.text.length) {
                    boundingRects.add(null)
                    continue
                }
                val bounds = textLayoutResult.getBoundingBox(positionInfoStartIndex + i)
                val boundsOnScreen = toScreenCoords(node, bounds)
                boundingRects.add(boundsOnScreen)
            }
            info.extras.putParcelableArray(extraDataKey, boundingRects.toTypedArray())
        } else if (node.unmergedConfig.contains(SemanticsProperties.TestTag) &&
            arguments != null && extraDataKey == ExtraDataTestTagKey
        ) {
            val testTag = node.unmergedConfig.getOrNull(SemanticsProperties.TestTag)
            if (testTag != null) {
                info.extras.putCharSequence(extraDataKey, testTag)
            }
        }
    }

    private fun toScreenCoords(textNode: SemanticsNode?, bounds: Rect): RectF? {
        if (textNode == null) return null
        val boundsInRoot = bounds.translate(textNode.positionInRoot)
        val textNodeBoundsInRoot = textNode.boundsInRoot

        // Only visible or partially visible locations are used.
        val visibleBounds = if (boundsInRoot.overlaps(textNodeBoundsInRoot)) {
            boundsInRoot.intersect(textNodeBoundsInRoot)
        } else {
            null
        }

        return if (visibleBounds != null) {
            val topLeftInScreen =
                view.localToScreen(Offset(visibleBounds.left, visibleBounds.top))
            val bottomRightInScreen =
                view.localToScreen(Offset(visibleBounds.right, visibleBounds.bottom))
            RectF(
                topLeftInScreen.x,
                topLeftInScreen.y,
                bottomRightInScreen.x,
                bottomRightInScreen.y
            )
        } else {
            null
        }
    }

    /**
     * Dispatches hover {@link android.view.MotionEvent}s to the virtual view hierarchy when
     * the Explore by Touch feature is enabled.
     * <p>
     * This method should be called by overriding
     * {@link View#dispatchHoverEvent}:
     *
     * <pre>&#64;Override
     * public boolean dispatchHoverEvent(MotionEvent event) {
     *   if (mHelper.dispatchHoverEvent(this, event) {
     *     return true;
     *   }
     *   return super.dispatchHoverEvent(event);
     * }
     * </pre>
     *
     * @param event The hover event to dispatch to the virtual view hierarchy.
     * @return Whether the hover event was handled.
     */
    fun dispatchHoverEvent(event: MotionEvent): Boolean {
        if (!isAccessibilityEnabled) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_HOVER_ENTER -> {
                val virtualViewId = hitTestSemanticsAt(event.x, event.y)
                // The android views could be view groups, so the event must be dispatched to the
                // views. Android ViewGroup.java will take care of synthesizing hover enter/exit
                // actions from hover moves.
                // Note that this should be before calling "updateHoveredVirtualView" so that in
                // the corner case of overlapped nodes, the final hover enter event is sent from
                // the node/view that we want to focus.
                val handled = view.androidViewsHandler.dispatchGenericMotionEvent(event)
                updateHoveredVirtualView(virtualViewId)
                return if (virtualViewId == InvalidId) handled else true
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                return when {
                    hoveredVirtualViewId != InvalidId -> {
                        updateHoveredVirtualView(InvalidId)
                        true
                    }
                    else -> {
                        view.androidViewsHandler.dispatchGenericMotionEvent(event)
                    }
                }
            }
            else -> {
                return false
            }
        }
    }

    /**
     * Hit test the layout tree for semantics wrappers.
     * The return value is a virtual view id, or InvalidId if an embedded Android View was hit.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @VisibleForTesting
    internal fun hitTestSemanticsAt(x: Float, y: Float): Int {
        view.measureAndLayout()

        val hitSemanticsEntities = HitTestResult<SemanticsEntity>()
        view.root.hitTestSemantics(
            pointerPosition = Offset(x, y),
            hitSemanticsEntities = hitSemanticsEntities
        )

        val wrapper = hitSemanticsEntities.lastOrNull()?.layoutNode?.outerSemantics
        var virtualViewId = InvalidId
        if (wrapper != null) {

            // The node below is not added to the tree; it's a wrapper around outer semantics to
            // use the methods available to the SemanticsNode
            val semanticsNode = SemanticsNode(wrapper, false)
            val wrapperToCheckAlpha = semanticsNode.findWrapperToGetBounds()

            // Do not 'find' invisible nodes when exploring by touch. This will prevent us from
            // sending events for invisible nodes
            if (!semanticsNode.unmergedConfig.contains(SemanticsProperties.InvisibleToUser) &&
                !wrapperToCheckAlpha.isTransparent()
            ) {
                val androidView = view.androidViewsHandler.layoutNodeToHolder[wrapper.layoutNode]
                if (androidView == null) {
                    virtualViewId = semanticsNodeIdToAccessibilityVirtualNodeId(wrapper.modifier.id)
                }
            }
        }
        return virtualViewId
    }

    /**
     * Sets the currently hovered item, sending hover accessibility events as
     * necessary to maintain the correct state.
     *
     * @param virtualViewId The virtual view id for the item currently being
     *            hovered, or {@link #InvalidId} if no item is hovered within
     *            the parent view.
     */
    private fun updateHoveredVirtualView(virtualViewId: Int) {
        if (hoveredVirtualViewId == virtualViewId) {
            return
        }

        val previousVirtualViewId: Int = hoveredVirtualViewId
        hoveredVirtualViewId = virtualViewId

        /*
        Stay consistent with framework behavior by sending ENTER/EXIT pairs
        in reverse order. This is accurate as of API 18.
        */
        sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
        sendEventForVirtualView(previousVirtualViewId, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT)
    }

    override fun getAccessibilityNodeProvider(host: View): AccessibilityNodeProviderCompat {
        return nodeProvider
    }

    /**
     * Trims the text to [size] length. Returns the string as it is if the length is
     * smaller than [size]. If chars at [size] - 1 and [size] is a surrogate
     * pair, returns a CharSequence of length [size] - 1.
     *
     * @param size length of the result, should be greater than 0
     */
    private fun <T : CharSequence> trimToSize(text: T?, @IntRange(from = 1) size: Int): T? {
        require(size > 0)
        var len = size
        if (text.isNullOrEmpty() || text.length <= size) return text
        if (Character.isHighSurrogate(text[size - 1]) && Character.isLowSurrogate(text[size])) {
            len = size - 1
        }
        @Suppress("UNCHECKED_CAST")
        return text.subSequence(0, len) as T
    }

    // TODO (in a separate cl): Called when the SemanticsNode with id semanticsNodeId disappears.
    // fun clearNode(semanticsNodeId: Int) { // clear the actionIdToId and labelToActionId nodes }

    private val semanticsChangeChecker = Runnable {
        view.measureAndLayout()
        checkForSemanticsChanges()
        checkingForSemanticsChanges = false
    }

    internal fun onSemanticsChange() {
        // When accessibility is turned off, we still want to keep
        // currentSemanticsNodesInvalidated up to date so that when accessibility is turned on
        // later, we can refresh currentSemanticsNodes if currentSemanticsNodes is stale.
        currentSemanticsNodesInvalidated = true

        if (isAccessibilityEnabled && !checkingForSemanticsChanges) {
            checkingForSemanticsChanges = true
            handler.post(semanticsChangeChecker)
        }
    }

    /**
     * This suspend function loops for the entire lifetime of the Compose instance: it consumes
     * recent layout changes and sends events to the accessibility framework in batches separated
     * by a 100ms delay.
     */
    suspend fun boundsUpdatesEventLoop() {
        try {
            val subtreeChangedSemanticsNodesIds = ArraySet<Int>()
            for (notification in boundsUpdateChannel) {
                if (isAccessibilityEnabled) {
                    for (i in subtreeChangedLayoutNodes.indices) {
                        sendSubtreeChangeAccessibilityEvents(
                            subtreeChangedLayoutNodes.valueAt(i)!!,
                            subtreeChangedSemanticsNodesIds
                        )
                    }
                    subtreeChangedSemanticsNodesIds.clear()
                    // When the bounds of layout nodes change, we will not always get semantics
                    // change notifications because bounds is not part of semantics. And bounds
                    // change from a layout node without semantics will affect the global bounds
                    // of it children which has semantics. Bounds change will affect which nodes
                    // are covered and which nodes are not, so the currentSemanticsNodes is not
                    // up to date anymore.
                    // After the subtree events are sent, accessibility services will get the
                    // current visible/invisible state. We also try to do semantics tree diffing
                    // to send out the proper accessibility events and update our copy here so that
                    // our incremental changes (represented by accessibility events) are consistent
                    // with accessibility services. That is: change - notify - new change -
                    // notify, if we don't do the tree diffing and update our copy here, we will
                    // combine old change and new change, which is missing finer-grained
                    // notification.
                    if (!checkingForSemanticsChanges) {
                        checkingForSemanticsChanges = true
                        handler.post(semanticsChangeChecker)
                    }
                }
                subtreeChangedLayoutNodes.clear()
                delay(SendRecurringAccessibilityEventsIntervalMillis)
            }
        } finally {
            subtreeChangedLayoutNodes.clear()
        }
    }

    internal fun onLayoutChange(layoutNode: LayoutNode) {
        // When accessibility is turned off, we still want to keep
        // currentSemanticsNodesInvalidated up to date so that when accessibility is turned on
        // later, we can refresh currentSemanticsNodes if currentSemanticsNodes is stale.
        currentSemanticsNodesInvalidated = true
        if (!isAccessibilityEnabled) {
            return
        }
        // The layout change of a LayoutNode will also affect its children, so even if it doesn't
        // have semantics attached, we should process it.
        notifySubtreeAccessibilityStateChangedIfNeeded(layoutNode)
    }

    private fun notifySubtreeAccessibilityStateChangedIfNeeded(layoutNode: LayoutNode) {
        if (subtreeChangedLayoutNodes.add(layoutNode)) {
            boundsUpdateChannel.trySend(Unit)
        }
    }

    private fun sendSubtreeChangeAccessibilityEvents(
        layoutNode: LayoutNode,
        subtreeChangedSemanticsNodesIds: ArraySet<Int>
    ) {
        // The node may be no longer available while we were waiting so check
        // again.
        if (!layoutNode.isAttached) {
            return
        }
        // Android Views will send proper events themselves.
        if (view.androidViewsHandler.layoutNodeToHolder.contains(layoutNode)) {
            return
        }
        // When we finally send the event, make sure it is an accessibility-focusable node.
        var semanticsWrapper = layoutNode.outerSemantics
            ?: layoutNode.findClosestParentNode { it.outerSemantics != null }
                ?.outerSemantics ?: return
        if (!semanticsWrapper.collapsedSemanticsConfiguration().isMergingSemanticsOfDescendants) {
            layoutNode.findClosestParentNode {
                it.outerSemantics
                    ?.collapsedSemanticsConfiguration()
                    ?.isMergingSemanticsOfDescendants == true
            }?.outerSemantics?.let { semanticsWrapper = it }
        }
        val id = semanticsWrapper.modifier.id
        if (!subtreeChangedSemanticsNodesIds.add(id)) {
            return
        }

        sendEventForVirtualView(
            semanticsNodeIdToAccessibilityVirtualNodeId(id),
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
        )
    }

    private fun checkForSemanticsChanges() {
        // Structural change
        sendSemanticsStructureChangeEvents(
            view.semanticsOwner.unmergedRootSemanticsNode,
            previousSemanticsRoot
        )
        // Property change
        sendSemanticsPropertyChangeEvents(currentSemanticsNodes)
        updateSemanticsNodesCopyAndPanes()
    }

    private fun updateSemanticsNodesCopyAndPanes() {
        // TODO(b/172606324): removed this compose specific fix when talkback has a proper solution.
        for (id in paneDisplayed) {
            val currentNode = currentSemanticsNodes[id]?.semanticsNode
            if (currentNode == null || !currentNode.hasPaneTitle()) {
                paneDisplayed.remove(id)
                sendPaneChangeEvents(
                    id,
                    AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED,
                    previousSemanticsNodes[id]?.unmergedConfig?.getOrNull(
                        SemanticsProperties.PaneTitle
                    )
                )
            }
        }
        previousSemanticsNodes.clear()
        for (entry in currentSemanticsNodes.entries) {
            if (entry.value.semanticsNode.hasPaneTitle() && paneDisplayed.add(entry.key)) {
                sendPaneChangeEvents(
                    entry.key,
                    AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_APPEARED,
                    entry.value.semanticsNode.unmergedConfig[SemanticsProperties.PaneTitle]
                )
            }
            previousSemanticsNodes[entry.key] =
                SemanticsNodeCopy(entry.value.semanticsNode, currentSemanticsNodes)
        }
        previousSemanticsRoot =
            SemanticsNodeCopy(view.semanticsOwner.unmergedRootSemanticsNode, currentSemanticsNodes)
    }

    @VisibleForTesting
    internal fun sendSemanticsPropertyChangeEvents(
        newSemanticsNodes: Map<Int, SemanticsNodeWithAdjustedBounds>
    ) {
        val oldScrollObservationScopes = ArrayList(scrollObservationScopes)
        scrollObservationScopes.clear()
        for (id in newSemanticsNodes.keys) {
            // We do doing this search because the new configuration is set as a whole, so we
            // can't indicate which property is changed when setting the new configuration.
            val oldNode = previousSemanticsNodes[id] ?: continue
            val newNode = newSemanticsNodes[id]?.semanticsNode
            var propertyChanged = false

            for (entry in newNode!!.unmergedConfig) {
                var newlyObservingScroll = false
                if (entry.key == SemanticsProperties.HorizontalScrollAxisRange ||
                    entry.key == SemanticsProperties.VerticalScrollAxisRange
                ) {
                    newlyObservingScroll = registerScrollingId(id, oldScrollObservationScopes)
                }
                if (!newlyObservingScroll &&
                    entry.value == oldNode.unmergedConfig.getOrNull(entry.key)
                ) {
                    continue
                }
                @Suppress("UNCHECKED_CAST")
                when (entry.key) {
                    SemanticsProperties.PaneTitle -> {
                        val paneTitle = entry.value as String
                        // If oldNode doesn't have pane title, it will be handled in
                        // updateSemanticsNodesCopyAndPanes().
                        if (oldNode.hasPaneTitle()) {
                            sendPaneChangeEvents(
                                id,
                                AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_TITLE,
                                paneTitle
                            )
                        }
                    }
                    SemanticsProperties.StateDescription, SemanticsProperties.ToggleableState -> {
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEventCompat.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                        )
                        // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
                        // force ViewRootImpl to update its accessibility-focused virtual-node.
                        // If we have an androidx fix, we can remove this event.
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED
                        )
                    }
                    SemanticsProperties.ProgressBarRangeInfo -> {
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEventCompat.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                        )
                        // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
                        // force ViewRootImpl to update its accessibility-focused virtual-node.
                        // If we have an androidx fix, we can remove this event.
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED
                        )
                    }
                    SemanticsProperties.Selected -> {
                        // The assumption is among widgets using SemanticsProperties.Selected, only
                        // Tab is using AccessibilityNodeInfo#isSelected, and all others are using
                        // AccessibilityNodeInfo#isChekable and setting
                        // AccessibilityNodeInfo#stateDescription in this delegate.
                        if (newNode.config.getOrNull(SemanticsProperties.Role) == Role.Tab) {
                            if (newNode.config.getOrNull(SemanticsProperties.Selected) == true) {
                                val event = createEvent(
                                    semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                    AccessibilityEvent.TYPE_VIEW_SELECTED
                                )
                                // Here we use the merged node
                                val mergedNode =
                                    SemanticsNode(newNode.outerSemanticsEntity, true)
                                val contentDescription = mergedNode.config.getOrNull(
                                    SemanticsProperties.ContentDescription
                                )?.fastJoinToString(",")
                                val text = mergedNode.config.getOrNull(SemanticsProperties.Text)
                                    ?.fastJoinToString(",")
                                contentDescription?.let { event.contentDescription = it }
                                text?.let { event.text.add(it) }
                                sendEvent(event)
                            } else {
                                // Send this event to match View.java.
                                sendEventForVirtualView(
                                    semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                                    AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED
                                )
                            }
                        } else {
                            sendEventForVirtualView(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                                AccessibilityEventCompat.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                            )
                            // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
                            // force ViewRootImpl to update its accessibility-focused virtual-node.
                            // If we have an androidx fix, we can remove this event.
                            sendEventForVirtualView(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                                AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED
                            )
                        }
                    }
                    SemanticsProperties.ContentDescription -> {
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION,
                            entry.value as List<String>
                        )
                    }
                    SemanticsProperties.EditableText -> {
                        // TODO(b/160184953) Add test for SemanticsProperty Text change event
                        if (newNode.isTextField) {
                            val oldText = oldNode.unmergedConfig.getTextForTextField() ?: ""
                            val newText = newNode.unmergedConfig.getTextForTextField() ?: ""
                            var startCount = 0
                            // endCount records how many characters are the same from the end.
                            var endCount = 0
                            val oldTextLen = oldText.length
                            val newTextLen = newText.length
                            val minLength = oldTextLen.coerceAtMost(newTextLen)
                            while (startCount < minLength) {
                                if (oldText[startCount] != newText[startCount]) {
                                    break
                                }
                                startCount++
                            }
                            // abcdabcd vs
                            //     abcd
                            while (endCount < minLength - startCount) {
                                if (oldText[oldTextLen - 1 - endCount] !=
                                    newText[newTextLen - 1 - endCount]
                                ) {
                                    break
                                }
                                endCount++
                            }
                            val removedCount = oldTextLen - endCount - startCount
                            val addedCount = newTextLen - endCount - startCount
                            val textChangeEvent = createEvent(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                            )
                            textChangeEvent.fromIndex = startCount
                            textChangeEvent.removedCount = removedCount
                            textChangeEvent.addedCount = addedCount
                            textChangeEvent.beforeText = oldText
                            textChangeEvent.text.add(trimToSize(newText, ParcelSafeTextLength))
                            sendEvent(textChangeEvent)
                        } else {
                            sendEventForVirtualView(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                                AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT
                            )
                        }
                    }
                    // do we need to overwrite TextRange equals?
                    SemanticsProperties.TextSelectionRange -> {
                        val newText = newNode.unmergedConfig.getTextForTextField()?.text ?: ""
                        val textRange =
                            newNode.unmergedConfig[SemanticsProperties.TextSelectionRange]
                        val event = createTextSelectionChangedEvent(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            textRange.start,
                            textRange.end,
                            newText.length,
                            trimToSize(newText, ParcelSafeTextLength)
                        )
                        sendEvent(event)
                        sendPendingTextTraversedAtGranularityEvent(newNode.id)
                    }
                    SemanticsProperties.HorizontalScrollAxisRange,
                    SemanticsProperties.VerticalScrollAxisRange -> {
                        // TODO(yingleiw): Add throttling for scroll/state events.
                        notifySubtreeAccessibilityStateChangedIfNeeded(newNode.layoutNode)

                        val scope = scrollObservationScopes.findById(id)!!
                        scope.horizontalScrollAxisRange = newNode.unmergedConfig.getOrNull(
                            SemanticsProperties.HorizontalScrollAxisRange
                        )
                        scope.verticalScrollAxisRange = newNode.unmergedConfig.getOrNull(
                            SemanticsProperties.VerticalScrollAxisRange
                        )
                        sendScrollEventIfNeeded(scope)
                    }
                    SemanticsProperties.Focused -> {
                        if (entry.value as Boolean) {
                            sendEvent(
                                createEvent(
                                    semanticsNodeIdToAccessibilityVirtualNodeId(newNode.id),
                                    AccessibilityEvent.TYPE_VIEW_FOCUSED
                                )
                            )
                        }
                        // In View.java this window event is sent for unfocused view. But we send
                        // it for focused too so that TalkBack invalidates its cache. Otherwise
                        // PasteText edit option is not displayed properly on some OS versions.
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(newNode.id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                        )
                    }
                    CustomActions -> {
                        val actions = newNode.unmergedConfig[CustomActions]
                        val oldActions = oldNode.unmergedConfig.getOrNull(CustomActions)
                        if (oldActions != null) {
                            // Suppose actions with the same label should be deduped.
                            val labels = mutableSetOf<String>()
                            actions.fastForEach { action ->
                                labels.add(action.label)
                            }
                            val oldLabels = mutableSetOf<String>()
                            oldActions.fastForEach { action ->
                                oldLabels.add(action.label)
                            }
                            propertyChanged =
                                !(labels.containsAll(oldLabels) && oldLabels.containsAll(labels))
                        } else if (actions.isNotEmpty()) {
                            propertyChanged = true
                        }
                    }
                    // TODO(b/151840490) send the correct events for certain properties, like view
                    //  selected.
                    else -> {
                        if (entry.value is AccessibilityAction<*>) {
                            propertyChanged = !(entry.value as AccessibilityAction<*>)
                                .accessibilityEquals(oldNode.unmergedConfig.getOrNull(entry.key))
                        } else {
                            propertyChanged = true
                        }
                    }
                }
            }

            if (!propertyChanged) {
                propertyChanged = newNode.propertiesDeleted(oldNode)
            }
            if (propertyChanged) {
                // TODO(b/176105563): throttle the window content change events and merge different
                //  sub types. We can use the subtreeChangedLayoutNodes with sub types.
                sendEventForVirtualView(
                    semanticsNodeIdToAccessibilityVirtualNodeId(id),
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                )
            }
        }
    }

    // List of visible scrollable nodes (which are observing scroll state snapshot writes).
    private val scrollObservationScopes = mutableListOf<ScrollObservationScope>()

    /*
     * Lambda to store in scrolling snapshot observer, which must never be recreated because
     * the snapshot system makes use of lambda reference comparisons.
     * (Note that recent versions of the Kotlin compiler do maintain a persistent
     * object for most lambda expressions, so this is just for the purpose of explicitness.)
     */
    private val sendScrollEventIfNeededLambda: (ScrollObservationScope) -> Unit = {
        this.sendScrollEventIfNeeded(it)
    }

    private fun registerScrollingId(
        id: Int,
        oldScrollObservationScopes: List<ScrollObservationScope>
    ): Boolean {
        var newlyObservingScroll = false
        val oldScope = oldScrollObservationScopes.findById(id)
        val newScope = if (oldScope != null) {
            oldScope
        } else {
            newlyObservingScroll = true
            ScrollObservationScope(
                semanticsNodeId = id,
                allScopes = scrollObservationScopes,
                oldXValue = null,
                oldYValue = null,
                horizontalScrollAxisRange = null,
                verticalScrollAxisRange = null
            )
        }
        scrollObservationScopes.add(newScope)
        return newlyObservingScroll
    }

    private fun sendScrollEventIfNeeded(scrollObservationScope: ScrollObservationScope) {
        if (!scrollObservationScope.isValid) {
            return
        }
        view.snapshotObserver.observeReads(scrollObservationScope, sendScrollEventIfNeededLambda) {
            val newXState = scrollObservationScope.horizontalScrollAxisRange
            val newYState = scrollObservationScope.verticalScrollAxisRange
            val oldXValue = scrollObservationScope.oldXValue
            val oldYValue = scrollObservationScope.oldYValue

            val deltaX = if (newXState != null && oldXValue != null) {
                newXState.value() - oldXValue
            } else {
                0f
            }
            val deltaY = if (newYState != null && oldYValue != null) {
                newYState.value() - oldYValue
            } else {
                0f
            }

            if (deltaX != 0f || deltaY != 0f) {
                val virtualNodeId = semanticsNodeIdToAccessibilityVirtualNodeId(
                    scrollObservationScope.semanticsNodeId
                )

                // View System sends a content changed event before each scroll event. TalkBack
                // uses the content changed event to synchronize the focus rect along with touch
                // scroll, and uses the scroll event to hear feedback that the app reacted to scroll
                // actions that it initiated.
                sendEventForVirtualView(
                    virtualNodeId,
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
                )

                val event = createEvent(
                    virtualNodeId,
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
                )
                if (newXState != null) {
                    event.scrollX = newXState.value().toInt()
                    event.maxScrollX = newXState.maxValue().toInt()
                }
                if (newYState != null) {
                    event.scrollY = newYState.value().toInt()
                    event.maxScrollY = newYState.maxValue().toInt()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Api28Impl.setScrollEventDelta(event, deltaX.toInt(), deltaY.toInt())
                }
                sendEvent(event)
            }

            if (newXState != null) {
                scrollObservationScope.oldXValue = newXState.value()
            }
            if (newYState != null) {
                scrollObservationScope.oldYValue = newYState.value()
            }
        }
    }

    private fun sendPaneChangeEvents(
        semanticsNodeId: Int,
        contentChangeType: Int,
        title: String?
    ) {
        val event = createEvent(
            semanticsNodeIdToAccessibilityVirtualNodeId(semanticsNodeId),
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        event.contentChangeTypes = contentChangeType
        if (title != null) {
            event.text.add(title)
        }
        sendEvent(event)
    }

    private fun sendSemanticsStructureChangeEvents(
        newNode: SemanticsNode,
        oldNode: SemanticsNodeCopy
    ) {
        val newChildren: MutableSet<Int> = mutableSetOf()

        // If any child is added, clear the subtree rooted at this node and return.
        newNode.replacedChildren.fastForEach { child ->
            if (currentSemanticsNodes.contains(child.id)) {
                if (!oldNode.children.contains(child.id)) {
                    notifySubtreeAccessibilityStateChangedIfNeeded(newNode.layoutNode)
                    return
                }
                newChildren.add(child.id)
            }
        }

        // If any child is deleted, clear the subtree rooted at this node and return.
        for (child in oldNode.children) {
            if (!newChildren.contains(child)) {
                notifySubtreeAccessibilityStateChangedIfNeeded(newNode.layoutNode)
                return
            }
        }

        newNode.replacedChildren.fastForEach { child ->
            if (currentSemanticsNodes.contains(child.id)) {
                sendSemanticsStructureChangeEvents(child, previousSemanticsNodes[child.id]!!)
            }
        }
    }

    private fun semanticsNodeIdToAccessibilityVirtualNodeId(id: Int): Int {
        if (id == view.semanticsOwner.unmergedRootSemanticsNode.id) {
            return AccessibilityNodeProviderCompat.HOST_VIEW_ID
        }
        return id
    }

    private fun traverseAtGranularity(
        node: SemanticsNode,
        granularity: Int,
        forward: Boolean,
        extendSelection: Boolean
    ): Boolean {
        if (node.id != previousTraversedNode) {
            accessibilityCursorPosition = AccessibilityCursorPositionUndefined
            previousTraversedNode = node.id
        }

        val text = getIterableTextForAccessibility(node)
        if (text.isNullOrEmpty()) {
            return false
        }
        val iterator = getIteratorForGranularity(node, granularity) ?: return false
        var current = getAccessibilitySelectionEnd(node)
        if (current == AccessibilityCursorPositionUndefined) {
            current = if (forward) 0 else text.length
        }
        val range = (if (forward) iterator.following(current) else iterator.preceding(current))
            ?: return false
        val segmentStart = range[0]
        val segmentEnd = range[1]
        var selectionStart: Int
        val selectionEnd: Int
        if (extendSelection && isAccessibilitySelectionExtendable(node)) {
            selectionStart = getAccessibilitySelectionStart(node)
            if (selectionStart == AccessibilityCursorPositionUndefined) {
                selectionStart = if (forward) segmentStart else segmentEnd
            }
            selectionEnd = if (forward) segmentEnd else segmentStart
        } else {
            selectionStart = if (forward) segmentEnd else segmentStart
            selectionEnd = selectionStart
        }
        val action =
            if (forward)
                AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
            else AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
        pendingTextTraversedEvent = PendingTextTraversedEvent(
            node,
            action,
            granularity,
            segmentStart,
            segmentEnd,
            SystemClock.uptimeMillis()
        )
        setAccessibilitySelection(node, selectionStart, selectionEnd, true)
        return true
    }

    private fun sendPendingTextTraversedAtGranularityEvent(semanticsNodeId: Int) {
        pendingTextTraversedEvent?.let {
            // not the same node, do nothing. Don't set pendingTextTraversedEvent to null either.
            if (semanticsNodeId != it.node.id) {
                return
            }
            if (SystemClock.uptimeMillis() - it.traverseTime <= TextTraversedEventTimeoutMillis) {
                val event = createEvent(
                    semanticsNodeIdToAccessibilityVirtualNodeId(it.node.id),
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                )
                event.fromIndex = it.fromIndex
                event.toIndex = it.toIndex
                event.action = it.action
                event.movementGranularity = it.granularity
                event.text.add(getIterableTextForAccessibility(it.node))
                sendEvent(event)
            }
        }
        pendingTextTraversedEvent = null
    }

    private fun setAccessibilitySelection(
        node: SemanticsNode,
        start: Int,
        end: Int,
        traversalMode: Boolean
    ): Boolean {
        // Any widget which has custom action_set_selection needs to provide cursor
        // positions, so events will be sent when cursor position change.
        // When the node is disabled, only the default/virtual set selection can performed.
        if (node.unmergedConfig.contains(SemanticsActions.SetSelection) && node.enabled()) {
            // Hide all selection controllers used for adjusting selection
            // since we are doing so explicitly by other means and these
            // controllers interact with how selection behaves. From TextView.java.
            return node.unmergedConfig[SemanticsActions.SetSelection].action?.invoke(
                start,
                end,
                traversalMode
            ) ?: false
        }
        if (start == end && end == accessibilityCursorPosition) {
            return false
        }
        val text = getIterableTextForAccessibility(node) ?: return false
        accessibilityCursorPosition = if (start >= 0 && start == end && end <= text.length) {
            start
        } else {
            AccessibilityCursorPositionUndefined
        }
        val nonEmptyText = text.isNotEmpty()
        val event = createTextSelectionChangedEvent(
            semanticsNodeIdToAccessibilityVirtualNodeId(node.id),
            if (nonEmptyText) accessibilityCursorPosition else null,
            if (nonEmptyText) accessibilityCursorPosition else null,
            if (nonEmptyText) text.length else null,
            text
        )
        sendEvent(event)
        sendPendingTextTraversedAtGranularityEvent(node.id)
        return true
    }

    /** Returns selection start and end indices in original text */
    private fun getAccessibilitySelectionStart(node: SemanticsNode): Int {
        // If there is ContentDescription, it will be used instead of text during traversal.
        if (!node.unmergedConfig.contains(SemanticsProperties.ContentDescription) &&
            node.unmergedConfig.contains(SemanticsProperties.TextSelectionRange)
        ) {
            return node.unmergedConfig[SemanticsProperties.TextSelectionRange].start
        }
        return accessibilityCursorPosition
    }

    private fun getAccessibilitySelectionEnd(node: SemanticsNode): Int {
        // If there is ContentDescription, it will be used instead of text during traversal.
        if (!node.unmergedConfig.contains(SemanticsProperties.ContentDescription) &&
            node.unmergedConfig.contains(SemanticsProperties.TextSelectionRange)
        ) {
            return node.unmergedConfig[SemanticsProperties.TextSelectionRange].end
        }
        return accessibilityCursorPosition
    }

    private fun isAccessibilitySelectionExtendable(node: SemanticsNode): Boolean {
        // Currently only TextField is extendable. Static text may become extendable later.
        return !node.unmergedConfig.contains(SemanticsProperties.ContentDescription) &&
            node.unmergedConfig.contains(SemanticsProperties.EditableText)
    }

    private fun getIteratorForGranularity(
        node: SemanticsNode?,
        granularity: Int
    ): AccessibilityIterators.TextSegmentIterator? {
        if (node == null) return null

        val text = getIterableTextForAccessibility(node)
        if (text.isNullOrEmpty()) {
            return null
        }
        // TODO(b/160190186) Make sure locale is right in AccessibilityIterators.
        val iterator: AccessibilityIterators.AbstractTextSegmentIterator
        @Suppress("DEPRECATION")
        when (granularity) {
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER -> {
                iterator = AccessibilityIterators.CharacterTextSegmentIterator.getInstance(
                    view.context.resources.configuration.locale
                )
                iterator.initialize(text)
            }
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD -> {
                iterator = AccessibilityIterators.WordTextSegmentIterator.getInstance(
                    view.context.resources.configuration.locale
                )
                iterator.initialize(text)
            }
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH -> {
                iterator = AccessibilityIterators.ParagraphTextSegmentIterator.getInstance()
                iterator.initialize(text)
            }
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE,
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE -> {
                // Line and page granularity are only for static text or text field.
                if (!node.unmergedConfig.contains(SemanticsActions.GetTextLayoutResult)) {
                    return null
                }
                // TODO(b/157474582): Note now it only works for single Text/TextField until we
                //  fix the merging issue.
                val textLayoutResults = mutableListOf<TextLayoutResult>()
                val textLayoutResult: TextLayoutResult
                val getLayoutResult = node.unmergedConfig[SemanticsActions.GetTextLayoutResult]
                    .action?.invoke(textLayoutResults)
                if (getLayoutResult == true) {
                    textLayoutResult = textLayoutResults[0]
                } else {
                    return null
                }
                if (granularity == AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE) {
                    iterator = AccessibilityIterators.LineTextSegmentIterator.getInstance()
                    iterator.initialize(text, textLayoutResult)
                } else {
                    iterator = AccessibilityIterators.PageTextSegmentIterator.getInstance()
                    // TODO: the node should be text/textfield node instead of the current node.
                    iterator.initialize(text, textLayoutResult, node)
                }
            }
            else -> return null
        }
        return iterator
    }

    /**
     * Gets the text reported for accessibility purposes. If a text node has a content description
     * in the unmerged config, it will be used instead of the text.
     *
     * This function is basically prioritising the content description over the text or editable
     * text of the text and text field nodes.
     */
    private fun getIterableTextForAccessibility(node: SemanticsNode?): String? {
        if (node == null) {
            return null
        }
        // Note in android framework, TextView set this to its text. This is changed to
        // prioritize content description, even for Text.
        if (node.unmergedConfig.contains(SemanticsProperties.ContentDescription)) {
            return node.unmergedConfig[SemanticsProperties.ContentDescription]
                .fastJoinToString(",")
        }

        if (node.isTextField) {
            return node.unmergedConfig.getTextForTextField()?.text
        }

        return node.unmergedConfig.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
    }

    private fun SemanticsConfiguration.getTextForTextField(): AnnotatedString? {
        return getOrNull(SemanticsProperties.EditableText)
    }

    // TODO(b/160820721): use AccessibilityNodeProviderCompat instead of AccessibilityNodeProvider
    inner class MyNodeProvider : AccessibilityNodeProvider() {
        override fun createAccessibilityNodeInfo(virtualViewId: Int):
            AccessibilityNodeInfo? {
                return createNodeInfo(virtualViewId)
            }

        override fun performAction(
            virtualViewId: Int,
            action: Int,
            arguments: Bundle?
        ): Boolean {
            return performActionHelper(virtualViewId, action, arguments)
        }

        override fun addExtraDataToAccessibilityNodeInfo(
            virtualViewId: Int,
            info: AccessibilityNodeInfo,
            extraDataKey: String,
            arguments: Bundle?
        ) {
            addExtraDataToAccessibilityNodeInfoHelper(virtualViewId, info, extraDataKey, arguments)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private object Api24Impl {
        @DoNotInline
        @JvmStatic
        fun addSetProgressAction(
            info: AccessibilityNodeInfoCompat,
            semanticsNode: SemanticsNode
        ) {
            if (semanticsNode.enabled()) {
                semanticsNode.unmergedConfig.getOrNull(SemanticsActions.SetProgress)?.let {
                    info.addAction(
                        AccessibilityActionCompat(
                            android.R.id.accessibilityActionSetProgress,
                            it.label
                        )
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private object Api28Impl {
        @JvmStatic
        @DoNotInline
        fun setScrollEventDelta(event: AccessibilityEvent, deltaX: Int, deltaY: Int) {
            event.scrollDeltaX = deltaX
            event.scrollDeltaY = deltaY
        }
    }
}

private fun SemanticsNode.enabled() = (config.getOrNull(SemanticsProperties.Disabled) == null)

private fun SemanticsNode.propertiesDeleted(
    oldNode: AndroidComposeViewAccessibilityDelegateCompat.SemanticsNodeCopy
): Boolean {
    for (entry in oldNode.unmergedConfig) {
        if (!config.contains(entry.key)) {
            return true
        }
    }
    return false
}

private fun SemanticsNode.hasPaneTitle() = config.contains(SemanticsProperties.PaneTitle)
private val SemanticsNode.isPassword: Boolean get() = config.contains(SemanticsProperties.Password)
private val SemanticsNode.isTextField get() = this.unmergedConfig.contains(SemanticsActions.SetText)
private val SemanticsNode.isRtl get() = layoutInfo.layoutDirection == LayoutDirection.Rtl

private fun SemanticsNode.excludeLineAndPageGranularities(): Boolean {
    // text field that is not in focus
    if (isTextField && unmergedConfig.getOrNull(SemanticsProperties.Focused) != true) return true

    // text nodes that are part of the 'merged' text field, for example hint or label.
    val ancestor = layoutNode.findClosestParentNode {
        // looking for text field merging node
        val ancestorSemanticsConfiguration = it.outerSemantics?.collapsedSemanticsConfiguration()
        ancestorSemanticsConfiguration?.isMergingSemanticsOfDescendants == true &&
            ancestorSemanticsConfiguration.contains(SemanticsActions.SetText)
    }
    return ancestor != null &&
        ancestor.outerSemantics
        ?.collapsedSemanticsConfiguration()
        ?.getOrNull(SemanticsProperties.Focused) != true
}

private fun AccessibilityAction<*>.accessibilityEquals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AccessibilityAction<*>) return false

    if (label != other.label) return false
    if (action == null && other.action != null) return false
    if (action != null && other.action == null) return false

    return true
}

/**
 * Semantics node with adjusted bounds for the uncovered(by siblings) part.
 */
internal class SemanticsNodeWithAdjustedBounds(
    val semanticsNode: SemanticsNode,
    val adjustedBounds: android.graphics.Rect
)

/**
 * Finds pruned [SemanticsNode]s in the tree owned by this [SemanticsOwner]. A semantics node
 * completely covered by siblings drawn on top of it will be pruned. Return the results in a
 * map.
 */
internal fun SemanticsOwner
.getAllUncoveredSemanticsNodesToMap(): Map<Int, SemanticsNodeWithAdjustedBounds> {
    val root = unmergedRootSemanticsNode
    val nodes = mutableMapOf<Int, SemanticsNodeWithAdjustedBounds>()
    if (!root.layoutNode.isPlaced || !root.layoutNode.isAttached) {
        return nodes
    }
    val unaccountedSpace = Region().also { it.set(root.boundsInRoot.toAndroidRect()) }

    fun findAllSemanticNodesRecursive(currentNode: SemanticsNode) {
        val notAttachedOrPlaced =
            !currentNode.layoutNode.isPlaced || !currentNode.layoutNode.isAttached
        if ((unaccountedSpace.isEmpty && currentNode.id != root.id) ||
            (notAttachedOrPlaced && !currentNode.isFake)
        ) {
            return
        }
        val boundsInRoot = currentNode.touchBoundsInRoot.toAndroidRect()
        val region = Region().also { it.set(boundsInRoot) }
        val virtualViewId = if (currentNode.id == root.id) {
            AccessibilityNodeProviderCompat.HOST_VIEW_ID
        } else {
            currentNode.id
        }
        if (region.op(unaccountedSpace, region, Region.Op.INTERSECT)) {
            nodes[virtualViewId] = SemanticsNodeWithAdjustedBounds(currentNode, region.bounds)
            // Children could be drawn outside of parent, but we are using clipped bounds for
            // accessibility now, so let's put the children recursion inside of this if. If later
            // we decide to support children drawn outside of parent, we can move it out of the
            // if block.
            val children = currentNode.replacedChildren
            for (i in children.size - 1 downTo 0) {
                findAllSemanticNodesRecursive(children[i])
            }
            unaccountedSpace.op(boundsInRoot, unaccountedSpace, Region.Op.REVERSE_DIFFERENCE)
        } else {
            if (currentNode.isFake) {
                val parentNode = currentNode.parent
                // use parent bounds for fake node
                val boundsForFakeNode = if (parentNode?.layoutInfo?.isPlaced == true) {
                    parentNode.boundsInRoot
                } else {
                    Rect(0f, 0f, 10f, 10f)
                }
                nodes[virtualViewId] = SemanticsNodeWithAdjustedBounds(
                    currentNode,
                    boundsForFakeNode.toAndroidRect()
                )
            } else if (virtualViewId == AccessibilityNodeProviderCompat.HOST_VIEW_ID) {
                // Root view might have WRAP_CONTENT layout params in which case it will have zero
                // bounds if there is no other content with semantics. But we need to always send the
                // root view info as there are some other apps (e.g. Google Assistant) that depend
                // on accessibility info
                nodes[virtualViewId] = SemanticsNodeWithAdjustedBounds(currentNode, region.bounds)
            }
        }
    }

    findAllSemanticNodesRecursive(root)
    return nodes
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal object AccessibilityNodeInfoVerificationHelperMethods {
    @RequiresApi(Build.VERSION_CODES.O)
    @DoNotInline
    fun setAvailableExtraData(node: AccessibilityNodeInfo, data: List<String>) {
        node.availableExtraData = data
    }
}

/**
 * These objects are used as snapshot observation scopes for the purpose of sending accessibility
 * scroll events whenever the scroll offset changes.  There is one per scroller and their lifecycle
 * is the same as the scroller's lifecycle in the semantics tree.
 */
internal class ScrollObservationScope(
    val semanticsNodeId: Int,
    val allScopes: List<ScrollObservationScope>,
    var oldXValue: Float?,
    var oldYValue: Float?,
    var horizontalScrollAxisRange: ScrollAxisRange?,
    var verticalScrollAxisRange: ScrollAxisRange?
) : OwnerScope {
    override val isValid get() = allScopes.contains(this)
}

internal fun List<ScrollObservationScope>.findById(id: Int): ScrollObservationScope? {
    for (index in indices) {
        if (this[index].semanticsNodeId == id) {
            return this[index]
        }
    }
    return null
}
