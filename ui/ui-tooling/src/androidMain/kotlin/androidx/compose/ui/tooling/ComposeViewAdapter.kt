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

package androidx.compose.ui.tooling

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.tooling.ComposableInvoker.invokeComposable
import androidx.compose.ui.tooling.animation.PreviewAnimationClock
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.SourceLocation
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntRect
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.lang.reflect.Method

private const val TOOLS_NS_URI = "http://schemas.android.com/tools"
private const val DESIGN_INFO_METHOD = "getDesignInfo"
private const val UPDATE_TRANSITION_FUNCTION_NAME = "updateTransition"

private val emptyContent: @Composable () -> Unit = @Composable {}

/**
 * Class containing the minimum information needed by the Preview to map components to the
 * source code and render boundaries.
 *
 * @suppress
 */
@OptIn(UiToolingDataApi::class)
data class ViewInfo(
    val fileName: String,
    val lineNumber: Int,
    val bounds: IntRect,
    val location: SourceLocation?,
    val children: List<ViewInfo>
) {
    fun hasBounds(): Boolean = bounds.bottom != 0 && bounds.right != 0

    fun allChildren(): List<ViewInfo> =
        children + children.flatMap { it.allChildren() }

    override fun toString(): String =
        """($fileName:$lineNumber,
            |bounds=(top=${bounds.top}, left=${bounds.left},
            |location=${location?.let { "(${it.offset}L${it.length}" } ?: "<none>"}
            |bottom=${bounds.bottom}, right=${bounds.right}),
            |childrenCount=${children.size})""".trimMargin()
}

/**
 * View adapter that renders a `@Composable`. The `@Composable` is found by
 * reading the `tools:composableName` attribute that contains the FQN. Additional attributes can
 * be used to customize the behaviour of this view:
 *  - `tools:parameterProviderClass`: FQN of the [PreviewParameterProvider] to be instantiated by
 *  the [ComposeViewAdapter] that will be used as source for the `@Composable` parameters.
 *  - `tools:parameterProviderIndex`: The index within the [PreviewParameterProvider] of the
 *  value to be used in this particular instance.
 *  - `tools:paintBounds`: If true, the component boundaries will be painted. This is only meant
 *  for debugging purposes.
 *  - `tools:printViewInfos`: If true, the [ComposeViewAdapter] will log the tree of [ViewInfo]
 *  to logcat for debugging.
 *  - `tools:animationClockStartTime`: When set, a [PreviewAnimationClock] will control the
 *  animations in the [ComposeViewAdapter] context.
 *
 * @suppress
 */
@Suppress("unused")
@OptIn(UiToolingDataApi::class)
internal class ComposeViewAdapter : FrameLayout {
    private val TAG = "ComposeViewAdapter"

    /**
     * [ComposeView] that will contain the [Composable] to preview.
     */
    private val composeView = ComposeView(context)

    /**
     * When enabled, generate and cache [ViewInfo] tree that can be inspected by the Preview
     * to map components to source code.
     */
    private var debugViewInfos = false

    /**
     * When enabled, paint the boundaries generated by layout nodes.
     */
    private var debugPaintBounds = false
    internal var viewInfos: List<ViewInfo> = emptyList()
    internal var designInfoList: List<String> = emptyList()
    private val slotTableRecord = CompositionDataRecord.create()

    /**
     * Simple function name of the Composable being previewed.
     */
    private var composableName = ""

    /**
     * Whether the current Composable has animations.
     */
    private var hasAnimations = false

    /**
     * Saved exception from the last composition. Since we can not handle the exception during the
     * composition, we save it and throw it during onLayout, this allows Studio to catch it and
     * display it to the user.
     */
    private val delayedException = ThreadSafeException()

    /**
     * The [Composable] to be rendered in the preview. It is initialized when this adapter
     * is initialized.
     */
    private var previewComposition: @Composable () -> Unit = {}

    // Note: the constant emptyContent below instead of a literal {} works around
    // https://youtrack.jetbrains.com/issue/KT-17467, which causes the compiler to emit classes
    // named `content` and `Content` (from the Content method's composable update scope)
    // which causes compilation problems on case-insensitive filesystems.
    @Suppress("RemoveExplicitTypeArguments")
    private val content = mutableStateOf<@Composable () -> Unit>(emptyContent)

    /**
     * When true, the composition will be immediately invalidated after being drawn. This will
     * force it to be recomposed on the next render. This is useful for live literals so the
     * whole composition happens again on the next render.
     */
    private var forceCompositionInvalidation = false

    /**
     * When true, the adapter will try to look objects that support the call
     * [DESIGN_INFO_METHOD] within the slot table and populate [designInfoList]. Used to
     * support rendering in Studio.
     */
    private var lookForDesignInfoProviders = false

    /**
     * An additional [String] argument that will be passed to objects that support the
     * [DESIGN_INFO_METHOD] call. Meant to be used by studio to as a way to request additional
     * information from the Preview.
     */
    private var designInfoProvidersArgument: String = ""

    /**
     * Callback invoked when onDraw has been called.
     */
    private var onDraw = {}

    private val debugBoundsPaint = Paint().apply {
        pathEffect = DashPathEffect(floatArrayOf(5f, 10f, 15f, 20f), 0f)
        style = Paint.Style.STROKE
        color = Color.Red.toArgb()
    }

    private var composition: Composition? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun walkTable(viewInfo: ViewInfo, indent: Int = 0) {
        Log.d(TAG, ("|  ".repeat(indent)) + "|-$viewInfo")
        viewInfo.children.forEach { walkTable(it, indent + 1) }
    }

    private val Group.fileName: String
        get() = location?.sourceFile ?: ""

    private val Group.lineNumber: Int
        get() = location?.lineNumber ?: -1

    /**
     * Returns true if this [Group] has no source position information
     */
    private fun Group.hasNullSourcePosition(): Boolean =
        fileName.isEmpty() && lineNumber == -1

    /**
     * Returns true if this [Group] has no source position information and no children
     */
    private fun Group.isNullGroup(): Boolean =
        hasNullSourcePosition() && children.isEmpty()

    private fun Group.toViewInfo(): ViewInfo {
        if (children.size == 1 && hasNullSourcePosition()) {
            // There is no useful information in this intermediate node, remove.
            return children.single().toViewInfo()
        }

        val childrenViewInfo = children
            .filter { !it.isNullGroup() }
            .map { it.toViewInfo() }

        // TODO: Use group names instead of indexing once it's supported
        return ViewInfo(
            location?.sourceFile ?: "",
            location?.lineNumber ?: -1,
            box,
            location,
            childrenViewInfo
        )
    }

    /**
     * Processes the recorded slot table and re-generates the [viewInfos] attribute.
     */
    private fun processViewInfos() {
        viewInfos = slotTableRecord.store.map { it.asTree() }.map { it.toViewInfo() }.toList()

        if (debugViewInfos) {
            viewInfos.forEach {
                walkTable(it)
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // If there was a pending exception then throw it here since Studio will catch it and show
        // it to the user.
        delayedException.throwIfPresent()

        processViewInfos()
        if (composableName.isNotEmpty()) {
            // TODO(b/160126628): support other APIs, e.g. animate
            findAndTrackTransitions()
            if (lookForDesignInfoProviders) {
                findDesignInfoProviders()
            }
        }
    }

    override fun onAttachedToWindow() {
        ViewTreeLifecycleOwner.set(composeView.rootView, FakeSavedStateRegistryOwner)
        super.onAttachedToWindow()
    }

    /**
     * Finds all the transition animations defined in the Compose tree where the root is the
     * `@Composable` being previewed. We only return animations defined in the user code, i.e.
     * the ones we've got source information for.
     */
    @Suppress("UNCHECKED_CAST")
    private fun findAndTrackTransitions() {
        @Suppress("UNCHECKED_CAST")
        fun List<Group>.findTransitionObjects(): List<Transition<Any>> {
            val rememberCalls = mapNotNull { it.firstOrNull { call -> call.name == "remember" } }
            return rememberCalls.mapNotNull {
                it.data.firstOrNull { data ->
                    data is Transition<*>
                } as? Transition<Any>
            }
        }

        val slotTrees = slotTableRecord.store.map { it.asTree() }
        val transitions = mutableSetOf<Transition<Any>>()
        val animatedVisibilityParentTransitions = mutableSetOf<Transition<Any>>()
        val animatedContentParentTransitions = mutableSetOf<Transition<Any>>()
        // Check all the slot tables, since some animations might not be present in the same
        // table as the one containing the `@Composable` being previewed, e.g. when they're
        // defined using sub-composition.
        slotTrees.forEach { tree ->
            transitions.addAll(
                // Find `updateTransition` calls in the user code, i.e. when source location is
                // known.
                tree.findAll { it.name == UPDATE_TRANSITION_FUNCTION_NAME && it.location != null }
                    .findTransitionObjects()
            )
            // Find `AnimatedVisibility` calls in the user code, i.e. when source location is
            // known. Then, find the underlying `updateTransition` it uses.
            animatedVisibilityParentTransitions.addAll(
                tree.findAll {
                    it.name == "AnimatedVisibility" && it.location != null
                }.mapNotNull {
                    it.children.firstOrNull { updateTransitionCall ->
                        updateTransitionCall.name == UPDATE_TRANSITION_FUNCTION_NAME
                    }
                }.findTransitionObjects()
            )

            animatedContentParentTransitions.addAll(
                tree.findAll {
                    it.name == "AnimatedContent" && it.location != null
                }.mapNotNull {
                    it.children.firstOrNull { updateTransitionCall ->
                        updateTransitionCall.name == UPDATE_TRANSITION_FUNCTION_NAME
                    }
                }.findTransitionObjects()
            )

            // Remove all AnimatedVisibility parent transitions from the transitions list,
            // otherwise we'd duplicate them in the Android Studio Animation Preview because we
            // will track them separately.
            transitions.removeAll(animatedVisibilityParentTransitions)

            // Remove all AnimatedContent parent transitions from the transitions list, so we can
            // ignore these animations while support is not added to Animation Preview.
            transitions.removeAll(animatedContentParentTransitions)
        }

        hasAnimations = transitions.isNotEmpty() || animatedVisibilityParentTransitions.isNotEmpty()
        // Make the `PreviewAnimationClock` track all the transitions found.
        if (::clock.isInitialized) {
            transitions.forEach { clock.trackTransition(it) }
            animatedVisibilityParentTransitions.forEach {
                clock.trackAnimatedVisibility(it, ::requestLayout)
            }
        }
    }

    /**
     * Find all data objects within the slotTree that can invoke '[DESIGN_INFO_METHOD]', and store
     * their result in [designInfoList].
     */
    private fun findDesignInfoProviders() {
        val slotTrees = slotTableRecord.store.map { it.asTree() }

        designInfoList = slotTrees.flatMap { rootGroup ->
            rootGroup.findAll { group ->
                group.children.any { child ->
                    child.name == "remember" && child.data.any {
                        it?.getDesignInfoMethodOrNull() != null
                    }
                }
            }.mapNotNull { group ->
                // Get the DesignInfoProviders from the children, the parent group is needed to
                // know the location on screen of the layout
                group.children.forEach { child ->
                    child.data.forEach {
                        if (it?.getDesignInfoMethodOrNull() != null) {
                            return@mapNotNull it.invokeGetDesignInfo(group.box.left, group.box.top)
                        }
                    }
                }
                return@mapNotNull null
            }
        }
    }

    /**
     * Check if the object supports the method call for [DESIGN_INFO_METHOD], which is expected
     * to take two Integer arguments for coordinates and a String for additional encoded
     * arguments that may be provided from Studio.
     */
    private fun Any.getDesignInfoMethodOrNull(): Method? {
        return try {
            javaClass.getDeclaredMethod(
                DESIGN_INFO_METHOD,
                Integer.TYPE,
                Integer.TYPE,
                String::class.java
            )
        } catch (e: NoSuchMethodException) {
            null
        }
    }

    @Suppress("BanUncheckedReflection")
    private fun Any.invokeGetDesignInfo(x: Int, y: Int): String? {
        return this.getDesignInfoMethodOrNull()?.let { designInfoMethod ->
            try {
                // Workaround for unchecked Method.invoke
                val result = designInfoMethod.invoke(
                    this,
                    x,
                    y,
                    designInfoProvidersArgument
                )
                (result as String).ifEmpty { null }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun Group.firstOrNull(predicate: (Group) -> Boolean): Group? {
        return findGroupsThatMatchPredicate(this, predicate, true).firstOrNull()
    }

    private fun Group.findAll(predicate: (Group) -> Boolean): List<Group> {
        return findGroupsThatMatchPredicate(this, predicate)
    }

    /**
     * Search [Group]s that match a given [predicate], starting from a given [root]. An optional
     * boolean parameter can be set if we're interested in a single occurrence. If it's set, we
     * return early after finding the first matching [Group].
     */
    private fun findGroupsThatMatchPredicate(
        root: Group,
        predicate: (Group) -> Boolean,
        findOnlyFirst: Boolean = false
    ): List<Group> {
        val result = mutableListOf<Group>()
        val stack = mutableListOf(root)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (predicate(current)) {
                if (findOnlyFirst) {
                    return listOf(current)
                }
                result.add(current)
            }
            stack.addAll(current.children)
        }
        return result
    }

    private fun invalidateComposition() {
        // Invalidate the full composition by setting it to empty and back to the actual value
        content.value = {}
        content.value = previewComposition
        // Invalidate the state of the view so it gets redrawn
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)

        if (forceCompositionInvalidation) invalidateComposition()

        onDraw()
        if (!debugPaintBounds) {
            return
        }

        viewInfos
            .flatMap { listOf(it) + it.allChildren() }
            .forEach {
                if (it.hasBounds()) {
                    canvas?.apply {
                        val pxBounds = android.graphics.Rect(
                            it.bounds.left,
                            it.bounds.top,
                            it.bounds.right,
                            it.bounds.bottom
                        )
                        drawRect(pxBounds, debugBoundsPaint)
                    }
                }
            }
    }

    /**
     * Clock that controls the animations defined in the context of this [ComposeViewAdapter].
     *
     * @suppress
     */
    @VisibleForTesting
    internal lateinit var clock: PreviewAnimationClock

    /**
     * Wraps a given [Preview] method an does any necessary setup.
     */
    @Composable
    private fun WrapPreview(content: @Composable () -> Unit) {
        // We need to replace the FontResourceLoader to avoid using ResourcesCompat.
        // ResourcesCompat can not load fonts within Layoutlib and, since Layoutlib always runs
        // the latest version, we do not need it.
        @Suppress("DEPRECATION")
        CompositionLocalProvider(
            LocalFontLoader provides LayoutlibFontResourceLoader(context),
            LocalFontFamilyResolver provides createFontFamilyResolver(context),
            LocalOnBackPressedDispatcherOwner provides FakeOnBackPressedDispatcherOwner,
            LocalActivityResultRegistryOwner provides FakeActivityResultRegistryOwner,
        ) {
            Inspectable(slotTableRecord, content)
        }
    }

    /**
     * Initializes the adapter and populates it with the given [Preview] composable.
     * @param className name of the class containing the preview function
     * @param methodName `@Preview` method name
     * @param parameterProvider [Class] for the [PreviewParameterProvider] to be used as
     * parameter input for this call. If null, no parameters will be passed to the composable.
     * @param parameterProviderIndex when [parameterProvider] is not null, this index will
     * reference the element in the [Sequence] to be used as parameter.
     * @param debugPaintBounds if true, the view will paint the boundaries around the layout
     * elements.
     * @param debugViewInfos if true, it will generate the [ViewInfo] structures and will log it.
     * @param animationClockStartTime if positive, [clock] will be defined and will control the
     * animations defined in the context of the `@Composable` being previewed.
     * @param forceCompositionInvalidation if true, the composition will be invalidated on every
     * draw, forcing it to recompose on next render.
     * @param lookForDesignInfoProviders if true, it will try to populate [designInfoList].
     * @param designInfoProvidersArgument String to use as an argument when populating
     * [designInfoList].
     * @param onCommit callback invoked after every commit of the preview composable.
     * @param onDraw callback invoked after every draw of the adapter. Only for test use.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @VisibleForTesting
    internal fun init(
        className: String,
        methodName: String,
        parameterProvider: Class<out PreviewParameterProvider<*>>? = null,
        parameterProviderIndex: Int = 0,
        debugPaintBounds: Boolean = false,
        debugViewInfos: Boolean = false,
        animationClockStartTime: Long = -1,
        forceCompositionInvalidation: Boolean = false,
        lookForDesignInfoProviders: Boolean = false,
        designInfoProvidersArgument: String? = null,
        onCommit: () -> Unit = {},
        onDraw: () -> Unit = {}
    ) {
        this.debugPaintBounds = debugPaintBounds
        this.debugViewInfos = debugViewInfos
        this.composableName = methodName
        this.forceCompositionInvalidation = forceCompositionInvalidation
        this.lookForDesignInfoProviders = lookForDesignInfoProviders
        this.designInfoProvidersArgument = designInfoProvidersArgument ?: ""
        this.onDraw = onDraw

        previewComposition = @Composable {
            SideEffect(onCommit)

            WrapPreview {
                val composer = currentComposer
                // We need to delay the reflection instantiation of the class until we are in the
                // composable to ensure all the right initialization has happened and the Composable
                // class loads correctly.
                val composable = {
                    try {
                        invokeComposable(
                            className,
                            methodName,
                            composer,
                            *getPreviewProviderParameters(parameterProvider, parameterProviderIndex)
                        )
                    } catch (t: Throwable) {
                        // If there is an exception, store it for later but do not catch it so
                        // compose can handle it and dispose correctly.
                        var exception: Throwable = t
                        // Find the root cause and use that for the delayedException.
                        while (exception is ReflectiveOperationException) {
                            exception = exception.cause ?: break
                        }
                        delayedException.set(exception)
                        throw t
                    }
                }
                if (animationClockStartTime >= 0) {
                    // When animation inspection is enabled, i.e. when a valid (non-negative)
                    // `animationClockStartTime` is passed, set the Preview Animation Clock. This
                    // clock will control the animations defined in this `ComposeViewAdapter`
                    // from Android Studio.
                    clock = PreviewAnimationClock {
                        // Invalidate the descendants of this ComposeViewAdapter's only grandchild
                        // (an AndroidOwner) when setting the clock time to make sure the Compose
                        // Preview will animate when the states are read inside the draw scope.
                        val composeView = getChildAt(0) as ComposeView
                        (composeView.getChildAt(0) as? ViewRootForTest)
                            ?.invalidateDescendants()
                        // Send pending apply notifications to ensure the animation duration will
                        // be read in the correct frame.
                        Snapshot.sendApplyNotifications()
                    }
                }
                composable()
            }
        }
        composeView.setContent(previewComposition)
        invalidate()
    }

    /**
     * Disposes the Compose elements allocated during [init]
     */
    internal fun dispose() {
        composeView.disposeComposition()
        if (::clock.isInitialized) {
            clock.dispose()
        }
        FakeViewModelStoreOwner.viewModelStore.clear()
    }

    /**
     *  Returns whether this `@Composable` has animations. This allows Android Studio to decide if
     *  the Animation Inspector icon should be displayed for this preview. The reason for using a
     *  method instead of the property directly is we use Java reflection to call it from Android
     *  Studio, and to find the property we'd need to filter the method names using `contains`
     *  instead of `equals`.
     *
     *  @suppress
     */
    fun hasAnimations() = hasAnimations

    private fun init(attrs: AttributeSet) {
        // ComposeView and lifecycle initialization
        ViewTreeLifecycleOwner.set(this, FakeSavedStateRegistryOwner)
        setViewTreeSavedStateRegistryOwner(FakeSavedStateRegistryOwner)
        ViewTreeViewModelStoreOwner.set(this, FakeViewModelStoreOwner)
        addView(composeView)

        val composableName = attrs.getAttributeValue(TOOLS_NS_URI, "composableName") ?: return
        val className = composableName.substringBeforeLast('.')
        val methodName = composableName.substringAfterLast('.')
        val parameterProviderIndex = attrs.getAttributeIntValue(
            TOOLS_NS_URI,
            "parameterProviderIndex", 0
        )
        val parameterProviderClass = attrs.getAttributeValue(TOOLS_NS_URI, "parameterProviderClass")
            ?.asPreviewProviderClass()

        val animationClockStartTime = try {
            attrs.getAttributeValue(TOOLS_NS_URI, "animationClockStartTime").toLong()
        } catch (e: Exception) {
            -1L
        }

        val forceCompositionInvalidation = attrs.getAttributeBooleanValue(
            TOOLS_NS_URI,
            "forceCompositionInvalidation", false
        )

        init(
            className = className,
            methodName = methodName,
            parameterProvider = parameterProviderClass,
            parameterProviderIndex = parameterProviderIndex,
            debugPaintBounds = attrs.getAttributeBooleanValue(
                TOOLS_NS_URI,
                "paintBounds",
                debugPaintBounds
            ),
            debugViewInfos = attrs.getAttributeBooleanValue(
                TOOLS_NS_URI,
                "printViewInfos",
                debugViewInfos
            ),
            animationClockStartTime = animationClockStartTime,
            forceCompositionInvalidation = forceCompositionInvalidation,
            lookForDesignInfoProviders = attrs.getAttributeBooleanValue(
                TOOLS_NS_URI,
                "findDesignInfoProviders",
                lookForDesignInfoProviders
            ),
            designInfoProvidersArgument = attrs.getAttributeValue(
                TOOLS_NS_URI,
                "designInfoProvidersArgument"
            )
        )
    }

    @SuppressLint("VisibleForTests")
    private val FakeSavedStateRegistryOwner = object : SavedStateRegistryOwner {
        private val lifecycle = LifecycleRegistry.createUnsafe(this)
        private val controller = SavedStateRegistryController.create(this).apply {
            performRestore(Bundle())
        }

        init {
            lifecycle.currentState = Lifecycle.State.RESUMED
        }

        override val savedStateRegistry: SavedStateRegistry
            get() = controller.savedStateRegistry

        override fun getLifecycle(): Lifecycle = lifecycle
    }

    private val FakeViewModelStoreOwner = object : ViewModelStoreOwner {
        private val viewModelStore = ViewModelStore()

        override fun getViewModelStore() = viewModelStore
    }

    private val FakeOnBackPressedDispatcherOwner = object : OnBackPressedDispatcherOwner {
        private val onBackPressedDispatcher = OnBackPressedDispatcher()

        override fun getOnBackPressedDispatcher() = onBackPressedDispatcher
        override fun getLifecycle() = FakeSavedStateRegistryOwner.lifecycle
    }

    private val FakeActivityResultRegistryOwner = object : ActivityResultRegistryOwner {
        private val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                throw IllegalStateException("Calling launch() is not supported in Preview")
            }
        }

        override fun getActivityResultRegistry(): ActivityResultRegistry = activityResultRegistry
    }
}
