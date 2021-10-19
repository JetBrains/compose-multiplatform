// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
// Modified by Alex Hosh (n34to0@gmail.com) 2021.
package com.intellij.openapi.editor.impl;

import com.intellij.diagnostic.Dumpable;
import com.intellij.ide.CopyProvider;
import com.intellij.ide.CutProvider;
import com.intellij.ide.DataManager;
import com.intellij.ide.DeleteProvider;
import com.intellij.ide.PasteProvider;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorDropHandler;
import com.intellij.openapi.editor.EditorGutter;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.IndentsModel;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.LineExtensionInfo;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.LatencyListener;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorPopupHandler;
import com.intellij.openapi.editor.ex.ErrorStripeEvent;
import com.intellij.openapi.editor.ex.ErrorStripeListener;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.PrioritizedDocumentListener;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.ex.ScrollingModelEx;
import com.intellij.openapi.editor.ex.SoftWrapChangeListener;
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterClient;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.editor.impl.view.EditorView;
import com.intellij.openapi.editor.markup.LineMarkerRenderer;
import com.intellij.openapi.editor.markup.LineMarkerRendererEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.TraceableDisposable;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleSettingsChangeEvent;
import com.intellij.psi.codeStyle.CodeStyleSettingsListener;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollBar;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.mac.MacGestureSupportInstaller;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.DeprecatedMethodException;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ButtonlessScrollBarUI;
import com.intellij.util.ui.UIUtil;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.plaf.ScrollBarUI;
import org.intellij.lang.annotations.JdkConstants;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.TextHitInfo;
import java.awt.geom.Point2D;
import java.awt.im.InputMethodRequests;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public final class IpwEditorImpl extends UserDataHolderBase implements EditorEx, HighlighterClient, Queryable, Dumpable,
    CodeStyleSettingsListener, FocusListener {
    public static final int TEXT_ALIGNMENT_LEFT = 0;
    public static final int TEXT_ALIGNMENT_RIGHT = 1;

    private static final Logger LOG = Logger.getInstance(IpwEditorImpl.class);
    private static final Key<JComponent> PERMANENT_HEADER = Key.create("PERMANENT_HEADER");
    static final Key<Boolean> CONTAINS_BIDI_TEXT = Key.create("contains.bidi.text");
    @SuppressWarnings("unused")
    public static final Key<Boolean> FORCED_SOFT_WRAPS = Key.create("forced.soft.wraps");
    @SuppressWarnings("unused")
    public static final Key<Boolean> SOFT_WRAPS_EXIST = Key.create("soft.wraps.exist");
    @SuppressWarnings("WeakerAccess")
    public static final Key<Boolean> DISABLE_CARET_POSITION_KEEPING = Key.create("editor.disable.caret.position.keeping");
    @SuppressWarnings("unused")
    public static final Key<Boolean> DISABLE_CARET_SHIFT_ON_WHITESPACE_INSERTION = Key.create("editor.disable.caret.shift.on.whitespace.insertion");
    private static final Key<BufferedImage> BUFFER = Key.create("buffer");
    @NotNull private final DocumentEx myDocument;

    private final JPanel myPanel;
    // @NotNull private final JScrollPane myScrollPane;
    @NotNull private final EditorComponentImpl myEditorComponent;
    @NotNull private final EditorGutterComponentImpl myGutterComponent;
    private final TraceableDisposable myTraceableDisposable = new TraceableDisposable(true);
    private final FocusModeModel myFocusModeModel;
    private volatile long myLastTypedActionTimestamp = -1;
    private String myLastTypedAction;
    private LatencyListener myLatencyPublisher;

    private final Map<Object, Cursor> myCustomCursors = new LinkedHashMap<>();
    @SuppressWarnings("unused")
    boolean myCursorSetExternally;

    // @NotNull private final MyScrollBar myVerticalScrollBar;

    private boolean myIsInsertMode = true;

    @NotNull private final CaretCursor myCaretCursor;

    @NotNull private final SettingsImpl mySettings;

    private boolean isReleased;

    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);
    private MyEditable myEditable;

    @NotNull
    private EditorColorsScheme myScheme;
    private boolean myIsViewer;
    @NotNull private final SelectionModelImpl mySelectionModel;
    @NotNull private final EditorMarkupModelImpl myMarkupModel;
    @NotNull private final EditorFilteringMarkupModelEx myDocumentMarkupModel;
    @NotNull private final MarkupModelListener myMarkupModelListener;
    @NotNull private final List<HighlighterListener> myHighlighterListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    @NotNull private final FoldingModelImpl myFoldingModel;
    @NotNull private final ScrollingModelImpl myScrollingModel;
    @NotNull private final CaretModelImpl myCaretModel;
    @NotNull private final SoftWrapModelImpl mySoftWrapModel;
    @NotNull private final InlayModelImpl myInlayModel;

    private int myHorizontalTextAlignment = TEXT_ALIGNMENT_LEFT;

    private volatile EditorHighlighter myHighlighter; // updated in EDT, but can be accessed from other threads (under read action)
    private Disposable myHighlighterDisposable = Disposer.newDisposable();
    private final TextDrawingCallback myTextDrawingCallback = new MyTextDrawingCallback();

    @MagicConstant(intValues = {VERTICAL_SCROLLBAR_LEFT, VERTICAL_SCROLLBAR_RIGHT})
    private int         myScrollBarOrientation;

    private final EditorScrollingPositionKeeper myScrollingPositionKeeper;

    @Nullable
    private final Project myProject;
    private final List<FocusChangeListener> myFocusListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    private MyInputMethodHandler myInputMethodRequestsHandler;
    private InputMethodRequests myInputMethodRequestsSwingWrapper;
    private boolean myIsOneLineMode;
    private boolean myIsRendererMode;
    private VirtualFile myVirtualFile;
    private boolean myIsColumnMode;
    @Nullable private Color myForcedBackground;

    private boolean myEmbeddedIntoDialogWrapper;

    private Predicate<? super RangeHighlighter> myHighlightingFilter;

    @NotNull private final IndentsModel myIndentsModel;

    @Nullable
    private CharSequence myPlaceholderText;
    @Nullable private TextAttributes myPlaceholderAttributes;
    private boolean myShowPlaceholderWhenFocused;

    private boolean myScrollToCaret = true;

    private boolean myPurePaintingMode;
    private boolean myPaintSelection;

    private final EditorSizeAdjustmentStrategy mySizeAdjustmentStrategy = new EditorSizeAdjustmentStrategy();
    private final Disposable myDisposable = Disposer.newDisposable();


    @SuppressWarnings("unused")
    public final boolean myDisableRtl = Registry.is("editor.disable.rtl");
    @SuppressWarnings("unused")
    public final Object myFractionalMetricsHintValue = calcFractionalMetricsHint();

    final EditorView myView;

    private boolean myNeedToSelectPreviousChar;

    private String myContextMenuGroupId = IdeActions.GROUP_BASIC_EDITOR_POPUP;
    private final List<EditorPopupHandler> myPopupHandlers = new ArrayList<>();

    private boolean myUseEditorAntialiasing = true;

    private volatile int myExpectedCaretOffset = -1;

    private final EditorKind myKind;

    IpwEditorImpl(@NotNull Document document, boolean viewer, @Nullable Project project, @NotNull EditorKind kind) {
        assertIsDispatchThread();
        myProject = project;
        myDocument = (DocumentEx)document;
        myScheme = createBoundColorSchemeDelegate(EditorColorsManager.getInstance().getGlobalScheme());
        // myScrollPane = new MyScrollPane(); // create UI after scheme initialization
        myIsViewer = viewer;
        myKind = kind;
        mySettings = new SettingsImpl(this, kind);

        MarkupModelEx documentMarkup = (MarkupModelEx)DocumentMarkupModel.forDocument(myDocument, myProject, true);

        mySelectionModel = new SelectionModelImpl(this);
        myMarkupModel = new EditorMarkupModelImpl(this);
        myDocumentMarkupModel = new EditorFilteringMarkupModelEx(this, documentMarkup);
        myFoldingModel = new FoldingModelImpl(this);
        myCaretModel = new CaretModelImpl(this);
        myScrollingModel = new ScrollingModelImpl(this);
        myInlayModel = new InlayModelImpl(this);
        Disposer.register(myCaretModel, myInlayModel);
        mySoftWrapModel = new SoftWrapModelImpl(this);

        myMarkupModelListener = new MarkupModelListener() {
            @Override
            public void afterAdded(@NotNull RangeHighlighterEx highlighter) {
                TextAttributes attributes = highlighter.getTextAttributes(getColorsScheme());
                canImpactGutterSize(highlighter);
                EditorUtil.attributesImpactFontStyle(attributes);
                EditorUtil.attributesImpactForegroundColor(attributes);

            }

            @Override
            public void beforeRemoved(@NotNull RangeHighlighterEx highlighter) {
                TextAttributes attributes = highlighter.getTextAttributes(getColorsScheme());
                canImpactGutterSize(highlighter);
                EditorUtil.attributesImpactFontStyle(attributes);
                EditorUtil.attributesImpactForegroundColor(attributes);

            }

            @Override
            public void attributesChanged(@NotNull RangeHighlighterEx highlighter,
                                          boolean renderersChanged, boolean fontStyleChanged, boolean foregroundColorChanged) {

            }
        };

        myMarkupModel.addErrorMarkerListener(new ErrorStripeListener() {
            @Override
            public void errorMarkerChanged(@NotNull ErrorStripeEvent e) {

            }
        }, myCaretModel);

        myDocumentMarkupModel.addMarkupModelListener(myCaretModel, myMarkupModelListener);
        myMarkupModel.addMarkupModelListener(myCaretModel, myMarkupModelListener);
        myDocument.addDocumentListener(myFoldingModel, myCaretModel);
        myDocument.addDocumentListener(myCaretModel, myCaretModel);

        myDocument.addDocumentListener(new EditorDocumentAdapter(), myCaretModel);
        myDocument.addDocumentListener(mySoftWrapModel, myCaretModel);
        myDocument.addDocumentListener(myMarkupModel, myCaretModel);

        myFoldingModel.addListener(mySoftWrapModel, myCaretModel);

        myInlayModel.addListener(myFoldingModel, myCaretModel);
        myInlayModel.addListener(myCaretModel, myCaretModel);

        myIndentsModel = new IndentsModelImpl(this);
        myCaretModel.addCaretListener(new IndentsModelCaretListener(this));

        myCaretModel.addCaretListener(myMarkupModel, myCaretModel);

        myCaretCursor = new CaretCursor();

        myScrollBarOrientation = VERTICAL_SCROLLBAR_RIGHT;

        mySoftWrapModel.addSoftWrapChangeListener(new SoftWrapChangeListener() {
            @Override
            public void recalculationEnds() {
                if (myCaretModel.isUpToDate()) {
                    myCaretModel.updateVisualPosition();
                }
            }

            @Override
            public void softWrapsChanged() {
                myGutterComponent.clearLineToGutterRenderersCache();
            }
        });

        EditorHighlighter highlighter = new NullEditorHighlighter();
        setHighlighter(highlighter);

        new FoldingPopupManager(this);

        myEditorComponent = new EditorComponentImpl(this);
        // myVerticalScrollBar = (MyScrollBar)myScrollPane.getVerticalScrollBar();
        // if (shouldScrollBarBeOpaque()) myVerticalScrollBar.setOpaque(true);
        myPanel = new JPanel();

        ComponentUtil.putClientProperty(myPanel, UIUtil.NOT_IN_HIERARCHY_COMPONENTS,
            (Iterable<? extends Component>)(Iterable<JComponent>)() -> {
                JComponent component = getPermanentHeaderComponent();
                if (component != null && component.getParent() == null) {
                    return Collections.singleton(component).iterator();
                }
                return Collections.emptyIterator();
            });

        myGutterComponent = new EditorGutterComponentImpl(this);
        ComponentUtil.putClientProperty(myGutterComponent, ColorKey.FUNCTION_KEY, key -> getColorsScheme().getColor(key));
        initComponent();

        myView = new EditorView(this);
        myView.reinitSettings();

        if (UISettings.getInstance().getPresentationMode()) {
            setFontSize(UISettings.getInstance().getPresentationModeFontSize());
        }

        myGutterComponent.updateSize();
        Dimension preferredSize = getPreferredSize();
        myEditorComponent.setSize(preferredSize);

        if (SystemInfo.isMac && SystemInfo.isJetBrainsJvm) {
            MacGestureSupportInstaller.installOnComponent(getComponent());
        }

        myScrollingModel.addVisibleAreaListener(e -> {
        });
        myScrollingModel.addVisibleAreaListener(myMarkupModel);

        CodeStyleSettingsManager.getInstance(myProject).addListener(this);

        myFocusModeModel = new FocusModeModel(this);
        Disposer.register(myDisposable, myFocusModeModel);
        myPopupHandlers.add(new DefaultPopupHandler());

        myScrollingPositionKeeper = new EditorScrollingPositionKeeper(this);
        Disposer.register(myDisposable, myScrollingPositionKeeper);
    }

    @SuppressWarnings("unused")
    public void applyFocusMode() {
        myFocusModeModel.applyFocusMode(myCaretModel.getPrimaryCaret());
    }

    @SuppressWarnings("unused")
    public boolean isInFocusMode(@NotNull FoldRegion region) {
        return myFocusModeModel.isInFocusMode(region);
    }

    @SuppressWarnings("unused")
    public Segment getFocusModeRange() {
        return myFocusModeModel.getFocusModeRange();
    }

    @SuppressWarnings("unused")
    @NotNull
    public FocusModeModel getFocusModeModel() {
        return myFocusModeModel;
    }

    @Override
    public void focusGained(@NotNull FocusEvent e) {
        myCaretCursor.activate();
        fireFocusGained(e);
    }

    @Override
    public void focusLost(@NotNull FocusEvent e) {
        clearCaretThread();
        fireFocusLost(e);
    }

    private boolean canImpactGutterSize(@NotNull RangeHighlighterEx highlighter) {
        if (highlighter.getGutterIconRenderer() != null) return true;
        LineMarkerRenderer lineMarkerRenderer = highlighter.getLineMarkerRenderer();
        if (lineMarkerRenderer == null) return false;
        LineMarkerRendererEx.Position position = EditorGutterComponentImpl.getLineMarkerPosition(lineMarkerRenderer);
        return position == LineMarkerRendererEx.Position.LEFT && !myGutterComponent.myForceLeftFreePaintersAreaShown ||
            position == LineMarkerRendererEx.Position.RIGHT && !myGutterComponent.myForceRightFreePaintersAreaShown;
    }

    boolean shouldScrollBarBeOpaque() {
        return false;
    }

    @SuppressWarnings("unused")
    @NotNull
    static Color adjustThumbColor(@NotNull Color base, boolean dark) {
        return dark ? ColorUtil.withAlpha(ColorUtil.shift(base, 1.35), 0.5)
            : ColorUtil.withAlpha(ColorUtil.shift(base, 0.68), 0.4);
    }

    @SuppressWarnings("unused")
    boolean isDarkEnough() {
        return ColorUtil.isDark(getBackgroundColor());
    }

    @NotNull
    @Override
    public EditorColorsScheme createBoundColorSchemeDelegate(@Nullable final EditorColorsScheme customGlobalScheme) {
        return customGlobalScheme;
    }

    @Override
    public int getPrefixTextWidthInPixels() {
        return (int)myView.getPrefixTextWidthInPixels();
    }

    @Override
    public void setPrefixTextAndAttributes(@Nullable String prefixText, @Nullable TextAttributes attributes) {
        mySoftWrapModel.recalculate();
        myView.setPrefix(prefixText, attributes);
    }

    @Override
    public boolean isPurePaintingMode() {
        return myPurePaintingMode;
    }

    @Override
    public void setPurePaintingMode(boolean enabled) {
        myPurePaintingMode = enabled;
    }

    @Override
    public void registerLineExtensionPainter(@NotNull IntFunction<? extends Collection<? extends LineExtensionInfo>> lineExtensionPainter) {
    }

    @SuppressWarnings("unused")
    public boolean processLineExtensions(int line, @NotNull Processor<? super LineExtensionInfo> processor) {
        return true;
    }

    @Override
    public void registerScrollBarRepaintCallback(@Nullable ButtonlessScrollBarUI.ScrollbarRepaintCallback callback) {
    }

    @Override
    public int getExpectedCaretOffset() {
        int expectedCaretOffset = myExpectedCaretOffset;
        return expectedCaretOffset == -1 ? getCaretModel().getOffset() : expectedCaretOffset;
    }

    @Override
    public void setContextMenuGroupId(@Nullable String groupId) {
        myContextMenuGroupId = groupId;
    }

    @Nullable
    @Override
    public String getContextMenuGroupId() {
        return myContextMenuGroupId;
    }

    @Override
    public void installPopupHandler(@NotNull EditorPopupHandler popupHandler) {
        myPopupHandlers.add(popupHandler);
    }

    @Override
    public void uninstallPopupHandler(@NotNull EditorPopupHandler popupHandler) {
        myPopupHandlers.remove(popupHandler);
    }

    @Override
    public void setCustomCursor(@NotNull Object requestor, @Nullable Cursor cursor) {
        if (cursor == null) {
            myCustomCursors.remove(requestor);
        }
        else {
            myCustomCursors.put(requestor, cursor);
        }
    }

    @Override
    public void setViewer(boolean isViewer) {
        myIsViewer = isViewer;
    }

    @Override
    public boolean isViewer() {
        return myIsViewer || myIsRendererMode;
    }

    @Override
    public boolean isRendererMode() {
        return myIsRendererMode;
    }

    @Override
    public void setRendererMode(boolean isRendererMode) {
        myIsRendererMode = isRendererMode;
    }

    @Override
    public void setFile(VirtualFile vFile) {
        myVirtualFile = vFile;
        reinitSettings();
    }

    @Override
    public VirtualFile getVirtualFile() {
        return myVirtualFile;
    }

    @Override
    @NotNull
    public SelectionModelImpl getSelectionModel() {
        return mySelectionModel;
    }

    @Override
    @NotNull
    public MarkupModelEx getMarkupModel() {
        return myMarkupModel;
    }

    @Override
    @NotNull
    public MarkupModelEx getFilteredDocumentMarkupModel() {
        return myDocumentMarkupModel;
    }

    @Override
    @NotNull
    public FoldingModelImpl getFoldingModel() {
        return myFoldingModel;
    }

    @Override
    @NotNull
    public CaretModelImpl getCaretModel() {
        return myCaretModel;
    }

    @Override
    @NotNull
    public ScrollingModelEx getScrollingModel() {
        return myScrollingModel;
    }

    @Override
    @NotNull
    public SoftWrapModelImpl getSoftWrapModel() {
        return mySoftWrapModel;
    }

    @NotNull
    @Override
    public InlayModelImpl getInlayModel() {
        return myInlayModel;
    }

    @NotNull
    @Override
    public EditorKind getEditorKind() {
        return myKind;
    }

    @Override
    @NotNull
    public EditorSettings getSettings() {
        assertReadAccess();
        return mySettings;
    }

    @SuppressWarnings("unused")
    public void resetSizes() {
        myView.reset();
    }

    @Override
    public void reinitSettings() {
        reinitSettings(true);
    }

    private void reinitSettings(boolean updateGutterSize) {
        assertIsDispatchThread();

        boolean softWrapsUsedBefore = mySoftWrapModel.isSoftWrappingEnabled();

        mySettings.reinitSettings();
        mySoftWrapModel.reinitSettings();
        myCaretModel.reinitSettings();
        mySelectionModel.reinitSettings();
        myView.reinitSettings();
        myFoldingModel.refreshSettings();
        myFoldingModel.rebuild();
        myInlayModel.reinitSettings();

        if (softWrapsUsedBefore ^ mySoftWrapModel.isSoftWrappingEnabled()) {
            validateSize();
        }

        myHighlighter.setColorScheme(myScheme);
        myMarkupModel.rebuild();

        myGutterComponent.reinitSettings(updateGutterSize);
        myGutterComponent.revalidate();

        // make sure carets won't appear at invalid positions (e.g. on Tab width change)
        getCaretModel().doWithCaretMerging(() -> myCaretModel.getAllCarets().forEach(caret -> caret.moveToOffset(caret.getOffset())));

        // if (myVirtualFile != null && myProject != null) {
        //     EditorNotifications.getInstance(myProject).updateNotifications(myVirtualFile);
        // }

        if (myFocusModeModel != null) {
            myFocusModeModel.clearFocusMode();
        }
    }

    @SuppressWarnings("unused")
    void throwEditorNotDisposedError(@NonNls @NotNull final String msg) {
        myTraceableDisposable.throwObjectNotDisposedError(msg);
    }

    public void throwDisposalError(@NonNls @NotNull String msg) {
        myTraceableDisposable.throwDisposalError(msg);
    }

    // EditorFactory.releaseEditor should be used to release editor
    @SuppressWarnings("unused")
    void release() {
        assertIsDispatchThread();
        if (isReleased) {
            throwDisposalError("Double release of editor:");
        }
        myTraceableDisposable.kill(null);

        isReleased = true;
        mySizeAdjustmentStrategy.cancelAllRequests();

        myFoldingModel.dispose();
        mySoftWrapModel.release();
        myMarkupModel.dispose();

        myScrollingModel.dispose();
        myGutterComponent.dispose();
        Disposer.dispose(myCaretModel);
        Disposer.dispose(mySoftWrapModel);
        Disposer.dispose(myView);
        clearCaretThread();

        myFocusListeners.clear();

        myEditorComponent.removeFocusListener(this);

        CodeStyleSettingsManager.removeListener(myProject, this);

        Disposer.dispose(myDisposable);
        // myVerticalScrollBar.setPersistentUI(JBScrollBar.createUI(null)); // clear error panel's cached image
    }

    private void clearCaretThread() {
    }

    private void initComponent() {
        myPanel.setLayout(new BorderLayout());

        myGutterComponent.setOpaque(true);

        // myScrollPane.setViewportView(myEditorComponent);
        // myScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        // myScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // myScrollPane.setRowHeaderView(myGutterComponent);

        myEditorComponent.setAutoscrolls(false); // we have our own auto-scrolling code

        myEditorComponent.addFocusListener(this);

        /*UiNotifyConnector connector = new UiNotifyConnector(myEditorComponent, new Activatable() {
            @Override
            public void showNotify() {
                myGutterComponent.updateSizeOnShowNotify();
            }
        });
        Disposer.register(getDisposable(), connector);
*/
        // update area available for soft wrapping on component shown/hidden
        myPanel.addHierarchyListener(e -> mySoftWrapModel.getApplianceManager().updateAvailableArea());

    }

    @Override
    public void setFontSize(final int fontSize) {
        setFontSize(fontSize, null);
    }

    private void setFontSize(int fontSize, @Nullable Point zoomCenter) {
        int oldFontSize = myScheme.getEditorFontSize();

        Rectangle visibleArea = myScrollingModel.getVisibleArea();
        Point zoomCenterRelative = zoomCenter == null ? new Point() : zoomCenter;
        Point zoomCenterAbsolute = new Point(visibleArea.x + zoomCenterRelative.x, visibleArea.y + zoomCenterRelative.y);
        LogicalPosition zoomCenterLogical = xyToLogicalPosition(zoomCenterAbsolute);
        int oldLineHeight = getLineHeight();
        int intraLineOffset = zoomCenterAbsolute.y % oldLineHeight;

        myScheme.setEditorFontSize(fontSize);
        fontSize = myScheme.getEditorFontSize(); // resulting font size might be different due to applied min/max limits
        myPropertyChangeSupport.firePropertyChange(PROP_FONT_SIZE, oldFontSize, fontSize);
        // Update vertical scroll bar bounds if necessary (we had a problem that use increased editor font size and it was not possible
        // to scroll to the bottom of the document).
        // myScrollPane.getViewport().invalidate();

        /*Point shiftedZoomCenterAbsolute = logicalPositionToXY(zoomCenterLogical);
        myScrollingModel.disableAnimation();
        try {
            int targetX = visibleArea.x == 0 ? 0 : shiftedZoomCenterAbsolute.x - zoomCenterRelative.x; // stick to left border if it's visible
            int targetY = shiftedZoomCenterAbsolute.y - zoomCenterRelative.y +
                (intraLineOffset * getLineHeight() + oldLineHeight / 2) / oldLineHeight;
            myScrollingModel.scroll(targetX, targetY);
        } finally {
            myScrollingModel.enableAnimation();
        }*/
    }

    @SuppressWarnings("unused")
    public int getFontSize() {
        return myScheme.getEditorFontSize();
    }

    @NotNull
    public ActionCallback type(@NotNull final String text) {
        final ActionCallback result = new ActionCallback();

        for (int i = 0; i < text.length(); i++) {
            myLastTypedActionTimestamp = System.currentTimeMillis();
            char c = text.charAt(i);
            myLastTypedAction = Character.toString(c);
            if (!processKeyTyped(c)) {
                result.setRejected();
                return result;
            }
        }

        result.setDone();

        return result;
    }

    private boolean processKeyTyped(@SuppressWarnings("unused") char c) {
        return true;
    }

    @SuppressWarnings("unused")
    void processKeyTypedImmediately(char c, @NotNull Graphics graphics, @NotNull DataContext dataContext) {
    }

    @SuppressWarnings("unused")
    void processKeyTypedNormally(char c, @NotNull DataContext dataContext) {
        EditorActionManager.getInstance();
        TypedAction.getInstance().actionPerformed(this, c, dataContext);
    }

    private void fireFocusLost(@NotNull FocusEvent event) {
        for (FocusChangeListener listener : myFocusListeners) {
            listener.focusLost(this, event);
        }
    }

    private void fireFocusGained(@NotNull FocusEvent event) {
        for (FocusChangeListener listener : myFocusListeners) {
            listener.focusGained(this, event);
        }
    }

    @Override
    public void setHighlighter(@NotNull final EditorHighlighter highlighter) {
        if (isReleased) return; // do not set highlighter to the released editor
        assertIsDispatchThread();
        final Document document = getDocument();
        Disposer.dispose(myHighlighterDisposable);

        document.addDocumentListener(highlighter);
        myHighlighterDisposable = () -> document.removeDocumentListener(highlighter);
        Disposer.register(myDisposable, myHighlighterDisposable);
        highlighter.setEditor(this);
        highlighter.setText(document.getImmutableCharSequence());
        if (!(highlighter instanceof EmptyEditorHighlighter)) {
            EditorHighlighterCache.rememberEditorHighlighterForCachesOptimization(document, highlighter);
        }
        myHighlighter = highlighter;

        if (myPanel != null) {
            reinitSettings();
        }
    }

    @NotNull
    @Override
    public EditorHighlighter getHighlighter() {
        assertReadAccess();
        return myHighlighter;
    }

    @Override
    @NotNull
    public EditorComponentImpl getContentComponent() {
        return myEditorComponent;
    }

    @NotNull
    @Override
    public EditorGutterComponentImpl getGutterComponentEx() {
        return myGutterComponent;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        myPropertyChangeSupport.addPropertyChangeListener(listener);
    }
    @Override
    public void addPropertyChangeListener(@NotNull final PropertyChangeListener listener, @NotNull Disposable parentDisposable) {
        addPropertyChangeListener(listener);
        Disposer.register(parentDisposable, () -> removePropertyChangeListener(listener));
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        myPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void setInsertMode(boolean mode) {
    }

    @Override
    public boolean isInsertMode() {
        return myIsInsertMode;
    }

    @Override
    public void setColumnMode(boolean mode) {
        assertIsDispatchThread();
        boolean oldValue = myIsColumnMode;
        myIsColumnMode = mode;
        myPropertyChangeSupport.firePropertyChange(PROP_COLUMN_MODE, oldValue, mode);
    }

    @Override
    public boolean isColumnMode() {
        return myIsColumnMode;
    }

    @Override
    public int yToVisualLine(int y) {
        return myView.yToVisualLine(y);
    }

    @Override
    @NotNull
    public VisualPosition xyToVisualPosition(@NotNull Point p) {
        return myView.xyToVisualPosition(p);
    }

    @Override
    @NotNull
    public VisualPosition xyToVisualPosition(@NotNull Point2D p) {
        return myView.xyToVisualPosition(p);
    }

    @Override
    @NotNull
    public Point2D offsetToPoint2D(int offset, boolean leanTowardsLargerOffsets, boolean beforeSoftWrap) {
        return myView.offsetToXY(offset, leanTowardsLargerOffsets, beforeSoftWrap);
    }

    @Override
    @NotNull
    public Point offsetToXY(int offset, boolean leanForward, boolean beforeSoftWrap) {
        Point2D point2D = offsetToPoint2D(offset, leanForward, beforeSoftWrap);
        return new Point((int)point2D.getX(), (int)point2D.getY());
    }

    @Override
    @NotNull
    public VisualPosition offsetToVisualPosition(int offset) {
        return offsetToVisualPosition(offset, false, false);
    }

    @Override
    @NotNull
    public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
        return myView.offsetToVisualPosition(offset, leanForward, beforeSoftWrap);
    }

    @SuppressWarnings("unused")
    public int offsetToVisualColumnInFoldRegion(@NotNull FoldRegion region, int offset, boolean leanTowardsLargerOffsets) {
        assertIsDispatchThread();
        return myView.offsetToVisualColumnInFoldRegion(region, offset, leanTowardsLargerOffsets);
    }

    @SuppressWarnings("unused")
    public int visualColumnToOffsetInFoldRegion(@NotNull FoldRegion region, int visualColumn, boolean leansRight) {
        assertIsDispatchThread();
        return myView.visualColumnToOffsetInFoldRegion(region, visualColumn, leansRight);
    }

    @Override
    @NotNull
    public LogicalPosition offsetToLogicalPosition(int offset) {
        return myView.offsetToLogicalPosition(offset);
    }

    @SuppressWarnings("unused")
    @TestOnly
    public void setCaretActive() {
    }

    // optimization: do not do column calculations here since we are interested in line number only
    public int offsetToVisualLine(int offset) {
        return offsetToVisualLine(offset, false);
    }

    @Override
    public int offsetToVisualLine(int offset, boolean beforeSoftWrap) {
        return myView.offsetToVisualLine(offset, beforeSoftWrap);
    }

    @SuppressWarnings("unused")
    public int visualLineStartOffset(int visualLine) {
        return myView.visualLineToOffset(visualLine);
    }

    @Override
    @NotNull
    public LogicalPosition xyToLogicalPosition(@NotNull Point p) {
        Point pp = p.x >= 0 && p.y >= 0 ? p : new Point(Math.max(p.x, 0), Math.max(p.y, 0));
        return visualToLogicalPosition(xyToVisualPosition(pp));
    }

    private int logicalToVisualLine(int logicalLine) {
        return logicalLine < myDocument.getLineCount() ? offsetToVisualLine(myDocument.getLineStartOffset(logicalLine)) :
            logicalToVisualPosition(new LogicalPosition(logicalLine, 0)).line;
    }

    @SuppressWarnings("unused")
    int logicalLineToY(int line) {
        int visualLine = logicalToVisualLine(line);
        return visualLineToY(visualLine);
    }

    @Override
    @NotNull
    public Point logicalPositionToXY(@NotNull LogicalPosition pos) {
        VisualPosition visible = logicalToVisualPosition(pos);
        return visualPositionToXY(visible);
    }

    @Override
    @NotNull
    public Point visualPositionToXY(@NotNull VisualPosition visible) {
        Point2D point2D = myView.visualPositionToXY(visible);
        return new Point((int)point2D.getX(), (int)point2D.getY());
    }

    @Override
    @NotNull
    public Point2D visualPositionToPoint2D(@NotNull VisualPosition visible) {
        return myView.visualPositionToXY(visible);
    }

    @SuppressWarnings("unused")
    public float getScale() {
        if (!Registry.is("editor.scale.gutter.icons")) return 1f;
        float normLineHeight = getLineHeight() / myScheme.getLineSpacing(); // normalized, as for 1.0f line spacing
        return normLineHeight / JBUIScale.scale(16f);
    }

    @SuppressWarnings("unused")
    public int findNearestDirectionBoundary(int offset, boolean lookForward) {
        return myView.findNearestDirectionBoundary(offset, lookForward);
    }

    @Override
    public int visualLineToY(int line) {
        return myView.visualLineToY(line);
    }

    @Override
    public void repaint(int startOffset, int endOffset) {
    }

    @SuppressWarnings("unused")
    public void addHighlighterListener(@NotNull HighlighterListener listener, @NotNull Disposable parentDisposable) {
        ContainerUtil.add(listener, myHighlighterListeners, parentDisposable);
    }

    @SuppressWarnings("unused")
    void repaint(int startOffset, int endOffset, boolean invalidateTextLayout) {
    }

    @SuppressWarnings("unused")
    void repaintLines(int startLine, int endLine) {
    }

    private void bulkUpdateStarted() {
        if (myInlayModel.isInBatchMode()) LOG.error("Document bulk mode shouldn't be started from batch inlay operation");

        myView.getPreferredSize(); // make sure size is calculated (in case it will be required while bulk mode is active)

        myScrollingModel.onBulkDocumentUpdateStarted();

        myScrollingPositionKeeper.savePosition();

        myCaretModel.onBulkDocumentUpdateStarted();
        mySoftWrapModel.onBulkDocumentUpdateStarted();
        myFoldingModel.onBulkDocumentUpdateStarted();
    }

    private void bulkUpdateFinished() {
        if (myInlayModel.isInBatchMode()) LOG.error("Document bulk mode shouldn't be finished from batch inlay operation");

        myFoldingModel.onBulkDocumentUpdateFinished();
        mySoftWrapModel.onBulkDocumentUpdateFinished();
        myView.reset();
        myCaretModel.onBulkDocumentUpdateFinished();

        validateSize();

        updateGutterSize();

        if (!Boolean.TRUE.equals(getUserData(DISABLE_CARET_POSITION_KEEPING))) {
            myScrollingPositionKeeper.restorePosition(true);
        }
    }

    private void beforeChangedUpdate(@SuppressWarnings("unused") @NotNull DocumentEvent e) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (isStickySelection()) {
            setStickySelection(false);
        }
        if (myDocument.isInBulkUpdate()) {
            // Assuming that the job is done at bulk listener callback methods.
            return;
        }
    }

    @SuppressWarnings("unused")
    void invokeDelayedErrorStripeRepaint() {
    }

    private void changedUpdate(@NotNull DocumentEvent e) {
        if (myDocument.isInBulkUpdate()) return;

        if (getGutterComponentEx().getCurrentAccessibleLine() != null) {
            escapeGutterAccessibleLine(e.getOffset(), e.getOffset() + e.getNewLength());
        }

        validateSize();
    }

    private void escapeGutterAccessibleLine(int offsetStart, int offsetEnd) {
        int startVisLine = offsetToVisualLine(offsetStart);
        int endVisLine = offsetToVisualLine(offsetEnd);
        int line = getCaretModel().getPrimaryCaret().getVisualPosition().line;
        if (startVisLine <= line && endVisLine >= line) {
            getGutterComponentEx().escapeCurrentAccessibleLine();
        }
    }

    @SuppressWarnings("unused")
    public void hideCursor() {
    }

    @SuppressWarnings("unused")
    public boolean isCursorHidden() {
        return false;
    }

    @SuppressWarnings("unused")
    public boolean isScrollToCaret() {
        return myScrollToCaret;
    }

    @SuppressWarnings("unused")
    public void setScrollToCaret(boolean scrollToCaret) {
        myScrollToCaret = scrollToCaret;
    }

    @NotNull
    public Disposable getDisposable() {
        return myDisposable;
    }

    private boolean updatingSize; // accessed from EDT only
    private void updateGutterSize() {
        assertIsDispatchThread();
        if (!updatingSize) {
            updatingSize = true;
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    if (!isDisposed()) {
                        myGutterComponent.updateSize();
                    }
                }
                finally {
                    updatingSize = false;
                }
            }, ModalityState.any(), __->isDisposed());
        }
    }

    void validateSize() {
    }

    @SuppressWarnings("unused")
    void recalculateSizeAndRepaint() {
    }

    @Override
    @NotNull
    public DocumentEx getDocument() {
        return myDocument;
    }

    @Override
    @NotNull
    public JComponent getComponent() {
        return myPanel;
    }

    @Override
    public void addEditorMouseListener(@NotNull EditorMouseListener listener) {
    }

    @Override
    public void removeEditorMouseListener(@NotNull EditorMouseListener listener) {
    }

    @Override
    public void addEditorMouseMotionListener(@NotNull EditorMouseMotionListener listener) {
    }

    @Override
    public void removeEditorMouseMotionListener(@NotNull EditorMouseMotionListener listener) {
    }

    @Override
    public boolean isStickySelection() {
        return false;
    }

    @Override
    public void setStickySelection(boolean enable) {
    }

    @SuppressWarnings("unused")
    public void setHorizontalTextAlignment(@MagicConstant(intValues = {TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_RIGHT}) int alignment) {
        myHorizontalTextAlignment = alignment;
    }

    @SuppressWarnings("unused")
    public boolean isRightAligned() {
        return myHorizontalTextAlignment == TEXT_ALIGNMENT_RIGHT;
    }

    @Override
    public boolean isDisposed() {
        return isReleased;
    }

    @SuppressWarnings("unused")
    public void stopDumbLater() {
        if (ApplicationManager.getApplication().isUnitTestMode()) return;
        ApplicationManager.getApplication().invokeLater(this::stopDumb, ModalityState.current(), __ -> isDisposed());
    }

    private void stopDumb() {
        putUserData(BUFFER, null);
    }

    @SuppressWarnings("unused")
    public void startDumb() {
    }

    @SuppressWarnings("unused")
    public boolean isDumb() {
        return getUserData(BUFFER) != null;
    }

    @SuppressWarnings("unused")
    void paint(@NotNull Graphics2D g) {
    }

    @SuppressWarnings("unused")
    @NotNull
    static Color getDisposedBackground() {
        return new JBColor(new Color(128, 255, 128), new Color(128, 255, 128));
    }

    @NotNull
    @Override
    public IndentsModel getIndentsModel() {
        return myIndentsModel;
    }

    @Override
    public void setHeaderComponent(JComponent header) {
    }

    @Override
    public boolean hasHeaderComponent() {
        return false;
    }

    @Override
    @Nullable
    public JComponent getPermanentHeaderComponent() {
        return getUserData(PERMANENT_HEADER);
    }

    @Override
    public void setPermanentHeaderComponent(@Nullable JComponent component) {
        putUserData(PERMANENT_HEADER, component);
    }

    @Override
    @Nullable
    public JComponent getHeaderComponent() {
        return null;
    }

    @Override
    public void setBackgroundColor(Color color) {
        // myScrollPane.setBackground(color);

        // if (getBackgroundIgnoreForced().equals(color)) {
        //     myForcedBackground = null;
        //     return;
        // }
        // myForcedBackground = color;
    }

    @SuppressWarnings("unused")
    @NotNull
    Color getForegroundColor() {
        return myScheme.getDefaultForeground();
    }

    @NotNull
    @Override
    public Color getBackgroundColor() {
        if (myForcedBackground != null) return myForcedBackground;

        return getBackgroundIgnoreForced();
    }

    @NotNull
    @Override
    public TextDrawingCallback getTextDrawingCallback() {
        return myTextDrawingCallback;
    }

    @Override
    public void setPlaceholder(@Nullable CharSequence text) {
        myPlaceholderText = text;
    }

    @Override
    public void setPlaceholderAttributes(@Nullable TextAttributes attributes) {
        myPlaceholderAttributes = attributes;
    }

    @SuppressWarnings("unused")
    @Nullable
    public TextAttributes getPlaceholderAttributes() {
        return myPlaceholderAttributes;
    }

    @SuppressWarnings("unused")
    public CharSequence getPlaceholder() {
        return myPlaceholderText;
    }

    @Override
    public void setShowPlaceholderWhenFocused(boolean show) {
        myShowPlaceholderWhenFocused = show;
    }

    @SuppressWarnings("unused")
    public boolean getShowPlaceholderWhenFocused() {
        return myShowPlaceholderWhenFocused;
    }

    @SuppressWarnings("unused")
    Color getBackgroundColor(@NotNull final TextAttributes attributes) {
        final Color attrColor = attributes.getBackgroundColor();
        return Comparing.equal(attrColor, myScheme.getDefaultBackground()) ? getBackgroundColor() : attrColor;
    }

    @NotNull
    private Color getBackgroundIgnoreForced() {
        Color color = myScheme.getDefaultBackground();
        if (myDocument.isWritable()) {
            return color;
        }
        Color readOnlyColor = myScheme.getColor(EditorColors.READONLY_BACKGROUND_COLOR);
        return readOnlyColor != null ? readOnlyColor : color;
    }

    @SuppressWarnings("unused")
    @Nullable
    public TextRange getComposedTextRange() {
        return myInputMethodRequestsHandler == null || myInputMethodRequestsHandler.composedText == null ?
            null : myInputMethodRequestsHandler.composedTextRange;
    }

    @Override
    public int getMaxWidthInRange(int startOffset, int endOffset) {
        return myView.getMaxWidthInRange(startOffset, endOffset);
    }

    @SuppressWarnings("unused")
    public boolean isPaintSelection() {
        return myPaintSelection || !isOneLineMode() || IJSwingUtilities.hasFocus(getContentComponent());
    }

    @SuppressWarnings("unused")
    public void setPaintSelection(boolean paintSelection) {
        myPaintSelection = paintSelection;
    }

    @Override
    @NotNull
    @NonNls
    public String dumpState() {
        return "allow caret inside tab: " + mySettings.isCaretInsideTabs()
            + ", allow caret after line end: " + mySettings.isVirtualSpace()
            + ", soft wraps: " + (mySoftWrapModel.isSoftWrappingEnabled() ? "on" : "off")
            + ", caret model: " + getCaretModel().dumpState()
            + ", soft wraps data: " + getSoftWrapModel().dumpState()
            + "\n\nfolding data: " + getFoldingModel().dumpState()
            + "\ninlay model: " + getInlayModel().dumpState()
            + (myDocument instanceof DocumentImpl ? "\n\ndocument info: " + ((DocumentImpl)myDocument).dumpState() : "")
            + "\nfont preferences: " + myScheme.getFontPreferences()
            + "\npure painting mode: " + myPurePaintingMode
            + "\ninsets: " + myEditorComponent.getInsets()
            + (myView == null ? "" : "\nview: " + myView.dumpState());
    }

    @SuppressWarnings("unused")
    public CaretRectangle[] getCaretLocations(boolean onlyIfShown) {
        return myCaretCursor.getCaretLocations(onlyIfShown);
    }

    @Override
    public int getAscent() {
        return myView.getAscent();
    }

    @Override
    public int getLineHeight() {
        return myView.getLineHeight();
    }

    @SuppressWarnings("unused")
    public int getDescent() {
        return myView.getDescent();
    }

    @SuppressWarnings("unused")
    public int getCharHeight() {
        return myView.getCharHeight();
    }

    @SuppressWarnings("unused")
    @NotNull
    public FontMetrics getFontMetrics(@JdkConstants.FontStyle int fontType) {
        EditorFontType ft;
        if (fontType == Font.PLAIN) ft = EditorFontType.PLAIN;
        else if (fontType == Font.BOLD) ft = EditorFontType.BOLD;
        else if (fontType == Font.ITALIC) ft = EditorFontType.ITALIC;
        else if (fontType == (Font.BOLD | Font.ITALIC)) ft = EditorFontType.BOLD_ITALIC;
        else {
            LOG.error("Unknown font type: " + fontType);
            ft = EditorFontType.PLAIN;
        }

        return myEditorComponent.getFontMetrics(myScheme.getFont(ft));
    }

    @SuppressWarnings("unused")
    public int getPreferredHeight() {
        return isReleased ? 0 : myView.getPreferredHeight();
    }

    @NotNull
    public Dimension getPreferredSize() {
        return isReleased ? new Dimension()
            : Registry.is("idea.true.smooth.scrolling.dynamic.scrollbars")
                ? new Dimension(getPreferredWidthOfVisibleLines(), myView.getPreferredHeight())
                : myView.getPreferredSize();
    }

    /* When idea.true.smooth.scrolling=true, this method is used to compute width of currently visible line range
     rather than width of the whole document.

     As transparent scrollbars, by definition, prevent blit-acceleration of scrolling, and we really need blit-acceleration
     because not all hardware can render pixel-by-pixel scrolling with acceptable FPS without it (we now have 4K-5K displays, you know).
     To have both the hardware acceleration and the transparent scrollbars we need to completely redesign JViewport machinery to support
     independent layers, which is (probably) possible, but it's a rather cumbersome task.

     Another approach is to make scrollbars opaque, but only in the editor (as editor is a slow-to-draw component with large screen area).
     This is what "true smooth scrolling" option currently does. Interestingly, making the vertical scrollbar opaque might actually be
     a good thing because on modern displays (size, aspect ratio) code rarely extends beyond the right screen edge, and even
     when it does, its mixing with the navigation bar only reduces intelligibility of both the navigation bar and the code itself.

     Horizontal scrollbar is another story - a single long line of text forces horizontal scrollbar in the whole document,
     and in that case "transparent" scrollbar has some merits. However, instead of using transparency, we can hide horizontal
     scrollbar altogether when it's not needed for currently visible content. In a sense, this approach is superior,
     as even "transparent" scrollbar is only semi-transparent (thus we may prefer "on-demand" scrollbar in the general case).

     Hiding the horizontal scrollbar also solves another issue - when both scrollbars are visible, vertical scrolling with
     a high-precision touchpad can result in unintentional horizontal shifts (because of the touchpad sensitivity).
     When visible content fully fits horizontally (i.e. in most cases), hiding the unneeded scrollbar
     reliably prevents the horizontal  "jitter".

     Keep in mind that this functionality is experimental and may need more polishing.

     In principle, we can apply this method to other components by defining, for example,
     VariableWidth interface and supporting it in JBScrollPane. */
    private int getPreferredWidthOfVisibleLines() {
        Rectangle area = getScrollingModel().getVisibleArea();
        VisualPosition begin = xyToVisualPosition(area.getLocation());
        VisualPosition end = xyToVisualPosition(new Point(area.x + area.width, area.y + area.height));
        return Math.max(myView.getPreferredWidth(begin.line, end.line), getScrollingWidth());
    }

    /* Returns the width of current horizontal scrolling state.
     Complements the getPreferredWidthOfVisibleLines() method to allows to retain horizontal
     scrolling position that is beyond the width of currently visible lines. */
    private int getScrollingWidth() {
        /*JScrollBar scrollbar = myScrollPane.getHorizontalScrollBar();
        if (scrollbar != null) {
            BoundedRangeModel model = scrollbar.getModel();
            if (model != null) {
                return model.getValue() + model.getExtent();
            }
        }*/
        return 0;
    }

    @NotNull
    @Override
    public Dimension getContentSize() {
        return isReleased ? new Dimension() : myView.getPreferredSize();
    }

    @Override
    public JScrollPane getScrollPane() {
        return new JBScrollPane() {
            @Override
            public void updateUI() {
            }

            @Override
            public void repaint(long tm, int x, int y, int width, int height) {
            }

            @Override
            public void repaint(Rectangle r) {
            }
        };
    }

    @Override
    public void setBorder(Border border) {
        // myScrollPane.setBorder(border);
    }

    @Override
    public Insets getInsets() {
        return null;
    }

    @Override
    public int logicalPositionToOffset(@NotNull LogicalPosition pos) {
        return myView.logicalPositionToOffset(pos);
    }

    public int getVisibleLineCount() {
        return Math.max(1, getVisibleLogicalLinesCount() + getSoftWrapModel().getSoftWrapsIntroducedLinesNumber());
    }

    private int getVisibleLogicalLinesCount() {
        return getDocument().getLineCount() - myFoldingModel.getTotalNumberOfFoldedLines();
    }

    @Override
    @NotNull
    public VisualPosition logicalToVisualPosition(@NotNull LogicalPosition logicalPos) {
        return myView.logicalToVisualPosition(logicalPos, false);
    }

    @Override
    @NotNull
    public LogicalPosition visualToLogicalPosition(@NotNull VisualPosition visiblePos) {
        return myView.visualToLogicalPosition(visiblePos);
    }

    @NotNull
    @Override
    public DataContext getDataContext() {
        return getProjectAwareDataContext(DataManager.getInstance().getDataContext(getContentComponent()));
    }

    @NotNull
    private DataContext getProjectAwareDataContext(@NotNull final DataContext original) {
        if (CommonDataKeys.PROJECT.getData(original) == myProject) return original;

        return dataId -> {
            if (CommonDataKeys.PROJECT.is(dataId)) {
                return myProject;
            }
            return original.getData(dataId);
        };
    }

    @Override
    public EditorMouseEventArea getMouseEventArea(@NotNull MouseEvent e) {
        if (myGutterComponent != e.getSource()) return EditorMouseEventArea.EDITING_AREA;

        int x = myGutterComponent.convertX(e.getX());

        return myGutterComponent.getEditorMouseAreaByOffset(x);
    }

    @NotNull
    static Object calcFractionalMetricsHint() {
        return Registry.is("editor.text.fractional.metrics")
            ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
            : RenderingHints.VALUE_FRACTIONALMETRICS_OFF;
    }

    @SuppressWarnings("unused")
    void updateCaretCursor() {
    }

    @Override
    public boolean setCaretVisible(boolean b) {
        boolean old = myCaretCursor.isActive();
        if (b) {
            myCaretCursor.activate();
        }
        else {
            myCaretCursor.passivate();
        }
        return old;
    }

    @Override
    public boolean setCaretEnabled(boolean enabled) {
        boolean old = myCaretCursor.isEnabled();
        myCaretCursor.setEnabled(enabled);
        return old;
    }

    @Override
    public void addFocusListener(@NotNull FocusChangeListener listener) {
        myFocusListeners.add(listener);
    }

    @Override
    public void addFocusListener(@NotNull FocusChangeListener listener, @NotNull Disposable parentDisposable) {
        ContainerUtil.add(listener, myFocusListeners, parentDisposable);
    }

    @Override
    @Nullable
    public Project getProject() {
        return myProject;
    }

    @Override
    public boolean isOneLineMode() {
        return myIsOneLineMode;
    }

    @Override
    public boolean isEmbeddedIntoDialogWrapper() {
        return myEmbeddedIntoDialogWrapper;
    }

    @Override
    public void setEmbeddedIntoDialogWrapper(boolean b) {
        assertIsDispatchThread();

        myEmbeddedIntoDialogWrapper = b;
        // myScrollPane.setFocusable(!b);
        // myEditorComponent.setFocusCycleRoot(!b);
        // myEditorComponent.setFocusable(b);
    }

    @Override
    public void setOneLineMode(boolean isOneLineMode) {
        myIsOneLineMode = isOneLineMode;
        getScrollPane().setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
        JBScrollPane pane = ObjectUtils.tryCast(getScrollPane(), JBScrollPane.class);
        JComponent component = pane == null ? null : pane.getStatusComponent();
        if (component != null) component.setVisible(!isOneLineMode());
        reinitSettings();
    }

    public static final class CaretRectangle {
        public final Point2D myPoint;
        public final float myWidth;
        public final Caret myCaret;
        public final boolean myIsRtl;

        private CaretRectangle(@NotNull Point2D point, float width, Caret caret, boolean isRtl) {
            myPoint = point;
            myWidth = Math.max(width, 2);
            myCaret = caret;
            myIsRtl = isRtl;
        }
    }

    final class CaretCursor {
        private CaretRectangle[] myLocations;
        private boolean myEnabled;

        @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
        private boolean myIsShown;

        private CaretCursor() {
            myLocations = new CaretRectangle[] {new CaretRectangle(new Point(0, 0), 0, null, false)};
            setEnabled(true);
        }

        public boolean isEnabled() {
            return myEnabled;
        }

        public void setEnabled(boolean enabled) {
            myEnabled = enabled;
        }

        private void activate() {
            myIsShown = true;
        }

        public boolean isActive() {
            return myIsShown;
        }

        private void passivate() {
            myIsShown = false;
        }

        CaretRectangle[] getCaretLocations(boolean onlyIfShown) {
            if (onlyIfShown && (!isEnabled() || !myIsShown || isRendererMode() || !IJSwingUtilities.hasFocus(getContentComponent()))) return null;
            return myLocations;
        }
    }

    private class OpaqueAwareScrollBar extends JBScrollBar {

        private OpaqueAwareScrollBar(@JdkConstants.AdjustableOrientation int orientation) {
            super(orientation);
        }

        @Override
        public void setOpaque(boolean opaque) {
            super.setOpaque(opaque || shouldScrollBarBeOpaque());
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return true;
        }
    }

    class MyScrollBar extends OpaqueAwareScrollBar {
        private ScrollBarUI myPersistentUI;

        private MyScrollBar(@JdkConstants.AdjustableOrientation int orientation) {
            super(orientation);
        }

        void setPersistentUI(@NotNull ScrollBarUI ui) {
            myPersistentUI = ui;
            setUI(ui);
        }

        @Override
        public void setUI(ScrollBarUI ui) {
            if (myPersistentUI == null) myPersistentUI = ui;
        }

        @SuppressWarnings("unused")
        int getDecScrollButtonHeight() {
            return 15;
        }

        @SuppressWarnings("unused")
        int getIncScrollButtonHeight() {
            return 15;
        }

        @Override
        public int getUnitIncrement(int direction) {
            return 0;
        }

        @Override
        public int getBlockIncrement(int direction) {
            return 0;
        }

    }

    @NotNull
    private MyEditable getViewer() {
        MyEditable editable = myEditable;
        if (editable == null) {
            myEditable = editable = new MyEditable();
        }
        return editable;
    }

    @Override
    public @NotNull CopyProvider getCopyProvider() {
        return getViewer();
    }

    @Override
    public @NotNull CutProvider getCutProvider() {
        return getViewer();
    }

    @Override
    public @NotNull PasteProvider getPasteProvider() {
        return getViewer();
    }

    @Override
    public @NotNull DeleteProvider getDeleteProvider() {
        return getViewer();
    }

    private class MyEditable implements CutProvider, CopyProvider, PasteProvider, DeleteProvider, DumbAware {
        @Override
        public void performCopy(@NotNull DataContext dataContext) {
            executeAction(IdeActions.ACTION_EDITOR_COPY, dataContext);
        }

        @Override
        public boolean isCopyEnabled(@NotNull DataContext dataContext) {
            return true;
        }

        @Override
        public boolean isCopyVisible(@NotNull DataContext dataContext) {
            return getSelectionModel().hasSelection(true);
        }

        @Override
        public void performCut(@NotNull DataContext dataContext) {
            executeAction(IdeActions.ACTION_EDITOR_CUT, dataContext);
        }

        @Override
        public boolean isCutEnabled(@NotNull DataContext dataContext) {
            return !isViewer();
        }

        @Override
        public boolean isCutVisible(@NotNull DataContext dataContext) {
            return isCutEnabled(dataContext) && getSelectionModel().hasSelection(true);
        }

        @Override
        public void performPaste(@NotNull DataContext dataContext) {
            executeAction(IdeActions.ACTION_EDITOR_PASTE, dataContext);
        }

        @Override
        public boolean isPastePossible(@NotNull DataContext dataContext) {
            // Copy of isPasteEnabled. See interface method javadoc.
            return !isViewer();
        }

        @Override
        public boolean isPasteEnabled(@NotNull DataContext dataContext) {
            return !isViewer();
        }

        @Override
        public void deleteElement(@NotNull DataContext dataContext) {
            executeAction(IdeActions.ACTION_EDITOR_DELETE, dataContext);
        }

        @Override
        public boolean canDeleteElement(@NotNull DataContext dataContext) {
            return !isViewer();
        }

        private void executeAction(@NotNull String actionId, @NotNull DataContext dataContext) {
            EditorAction action = (EditorAction)ActionManager.getInstance().getAction(actionId);
            if (action != null) {
                action.actionPerformed(IpwEditorImpl.this, dataContext);
            }
        }
    }

    @Override
    public void setColorsScheme(@NotNull EditorColorsScheme scheme) {
        assertIsDispatchThread();
        myScheme = scheme;
        reinitSettings();
    }

    @Override
    @NotNull
    public EditorColorsScheme getColorsScheme() {
        return myScheme;
    }

    static void assertIsDispatchThread() {
        ApplicationManager.getApplication().assertIsDispatchThread();
    }

    private static void assertReadAccess() {
        ApplicationManager.getApplication().assertReadAccessAllowed();
    }

    @Override
    public void setVerticalScrollbarOrientation(int type) {
        assertIsDispatchThread();
        /*if (myScrollBarOrientation == type) return;
        int currentHorOffset = myScrollingModel.getHorizontalScrollOffset();
        myScrollBarOrientation = type;
        myScrollPane.putClientProperty(JBScrollPane.Flip.class,
            type == VERTICAL_SCROLLBAR_LEFT
                ? JBScrollPane.Flip.HORIZONTAL
                : null);
        myScrollingModel.scrollHorizontally(currentHorOffset);*/
    }

    @Override
    public void setVerticalScrollbarVisible(boolean b) {
        // myScrollPane
        //     .setVerticalScrollBarPolicy(b ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    }

    @Override
    public void setHorizontalScrollbarVisible(boolean b) {
        // myScrollPane.setHorizontalScrollBarPolicy(
        //     b ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    @Override
    public int getVerticalScrollbarOrientation() {
        return myScrollBarOrientation;
    }

    @SuppressWarnings("unused")
    public boolean isMirrored() {
        return myScrollBarOrientation != EditorEx.VERTICAL_SCROLLBAR_RIGHT;
    }

    @SuppressWarnings("unused")
    @NotNull
    MyScrollBar getVerticalScrollBar() {
        return new MyScrollBar(0) {
            @Override
            public void repaint(long tm, int x, int y, int width, int height) {

            }

            @Override
            public void repaint(Rectangle r) {

            }

            @Override
            public void updateUI() {
            }
        };
    }

    @SuppressWarnings("unused")
    void replaceInputMethodText(@NotNull InputMethodEvent e) {
        if (isReleased) return;
        getInputMethodRequests();
        myInputMethodRequestsHandler.replaceInputMethodText(e);
    }

    @SuppressWarnings("unused")
    void inputMethodCaretPositionChanged(@NotNull InputMethodEvent e) {
        if (isReleased) return;
        getInputMethodRequests();
        myInputMethodRequestsHandler.setInputMethodCaretPosition(e);
    }

    @NotNull
    InputMethodRequests getInputMethodRequests() {
        if (myInputMethodRequestsHandler == null) {
            myInputMethodRequestsHandler = new MyInputMethodHandler();
            myInputMethodRequestsSwingWrapper = new MyInputMethodHandleSwingThreadWrapper(myInputMethodRequestsHandler);
        }
        return myInputMethodRequestsSwingWrapper;
    }

    @Override
    public boolean processKeyTyped(@NotNull KeyEvent e) {
        myLastTypedActionTimestamp = -1;
        if (e.getID() != KeyEvent.KEY_TYPED) return false;
        char c = e.getKeyChar();
        if (UIUtil.isReallyTypedEvent(e)) { // Hack just like in javax.swing.text.DefaultEditorKit.DefaultKeyTypedAction
            myLastTypedActionTimestamp = e.getWhen();
            myLastTypedAction = Character.toString(c);
            processKeyTyped(c);
            return true;
        }
        else {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public void recordLatencyAwareAction(@NotNull String actionId, long timestampMs) {
        myLastTypedActionTimestamp = timestampMs;
        myLastTypedAction = actionId;
    }

    @SuppressWarnings("unused")
    void measureTypingLatency() {
        if (myLastTypedActionTimestamp == -1) {
            return;
        }

        LatencyListener latencyPublisher = myLatencyPublisher;
        if (latencyPublisher == null) {
            latencyPublisher = ApplicationManager.getApplication().getMessageBus().syncPublisher(LatencyListener.TOPIC);
            myLatencyPublisher = latencyPublisher;
        }

        latencyPublisher.recordTypingLatency(this, myLastTypedAction, System.currentTimeMillis() - myLastTypedActionTimestamp);
        myLastTypedActionTimestamp = -1;
    }

    @SuppressWarnings("unused")
    public boolean isProcessingTypedAction() {
        return myLastTypedActionTimestamp != -1;
    }

    @SuppressWarnings("unused")
    void beforeModalityStateChanged() {
        myScrollingModel.beforeModalityStateChanged();
    }

    @SuppressWarnings("unused")
    public void setDropHandler(@NotNull EditorDropHandler dropHandler) {
    }

    @Deprecated
    public void setHighlightingFilter(@Nullable Condition<? super RangeHighlighter> filter) {
        setHighlightingPredicate(filter == null ? null : highlighter -> filter.value(highlighter));
        DeprecatedMethodException.report("Use setHighlightingPredicate() instead");
    }

    public void setHighlightingPredicate(@Nullable Predicate<? super RangeHighlighter> filter) {
        if (myHighlightingFilter == filter) return;
        Predicate<? super RangeHighlighter> oldFilter = myHighlightingFilter;
        myHighlightingFilter = filter;

        for (RangeHighlighter highlighter : myDocumentMarkupModel.getDelegate().getAllHighlighters()) {
            boolean oldAvailable = oldFilter == null || oldFilter.test(highlighter);
            boolean newAvailable = filter == null || filter.test(highlighter);
            if (oldAvailable != newAvailable) {
                TextAttributes attributes = highlighter.getTextAttributes(getColorsScheme());
                myMarkupModelListener.attributesChanged((RangeHighlighterEx)highlighter, true,
                    EditorUtil.attributesImpactFontStyle(attributes),
                    EditorUtil.attributesImpactForegroundColor(attributes));
                myMarkupModel.getErrorStripeMarkersModel().attributesChanged((RangeHighlighterEx)highlighter, true);
            }
        }
    }

    @SuppressWarnings("unused")
    boolean isHighlighterAvailable(@NotNull RangeHighlighter highlighter) {
        return myHighlightingFilter == null || myHighlightingFilter.test(highlighter);
    }

    private static final class MyInputMethodHandleSwingThreadWrapper implements InputMethodRequests {
        private final InputMethodRequests myDelegate;

        private MyInputMethodHandleSwingThreadWrapper(InputMethodRequests delegate) {
            myDelegate = delegate;
        }

        @NotNull
        @Override
        public Rectangle getTextLocation(final TextHitInfo offset) {
            return execute(() -> myDelegate.getTextLocation(offset));
        }

        @Override
        public TextHitInfo getLocationOffset(final int x, final int y) {
            return execute(() -> myDelegate.getLocationOffset(x, y));
        }

        @Override
        public int getInsertPositionOffset() {
            return execute(myDelegate::getInsertPositionOffset);
        }

        @NotNull
        @Override
        public AttributedCharacterIterator getCommittedText(final int beginIndex, final int endIndex,
                                                            final AttributedCharacterIterator.Attribute[] attributes) {
            return execute(() -> myDelegate.getCommittedText(beginIndex, endIndex, attributes));
        }

        @Override
        public int getCommittedTextLength() {
            return execute(myDelegate::getCommittedTextLength);
        }

        @Override
        @Nullable
        public AttributedCharacterIterator cancelLatestCommittedText(AttributedCharacterIterator.Attribute[] attributes) {
            return null;
        }

        @Override
        public AttributedCharacterIterator getSelectedText(final AttributedCharacterIterator.Attribute[] attributes) {
            return execute(() -> myDelegate.getSelectedText(attributes));
        }

        private static <T> T execute(@NotNull Computable<T> computable) {
            return UIUtil.invokeAndWaitIfNeeded(computable);
        }
    }

    private class MyInputMethodHandler implements InputMethodRequests {
        private String composedText;
        private ProperTextRange composedTextRange;

        @NotNull
        @Override
        public Rectangle getTextLocation(TextHitInfo offset) {
            if (isDisposed()) return new Rectangle();
            Point caret = logicalPositionToXY(getCaretModel().getLogicalPosition());
            Rectangle r = new Rectangle(caret, new Dimension(1, getLineHeight()));
            Point p = getLocationOnScreen(getContentComponent());
            r.translate(p.x, p.y);
            return r;
        }

        @Override
        @Nullable
        public TextHitInfo getLocationOffset(int x, int y) {
            if (composedText != null) {
                Point p = getLocationOnScreen(getContentComponent());
                p.x = x - p.x;
                p.y = y - p.y;
                int pos = logicalPositionToOffset(xyToLogicalPosition(p));
                if (composedTextRange.containsOffset(pos)) {
                    return TextHitInfo.leading(pos - composedTextRange.getStartOffset());
                }
            }
            return null;
        }

        private Point getLocationOnScreen(Component component) {
            Point location = new Point();
            SwingUtilities.convertPointToScreen(location, component);
            if (LOG.isDebugEnabled() && !component.isShowing()) {
                Class<?> type = component.getClass();
                Component parent = component.getParent();
                while (parent != null && !parent.isShowing()) {
                    type = parent.getClass();
                    parent = parent.getParent();
                }
                String message = type.getName() + " is not showing";
                if (parent != null) message += " on visible  " + parent.getClass().getName();
                LOG.debug(message);
            }
            return location;
        }

        @Override
        public int getInsertPositionOffset() {
            int composedStartIndex = 0;
            int composedEndIndex = 0;
            if (composedText != null) {
                composedStartIndex = composedTextRange.getStartOffset();
                composedEndIndex = composedTextRange.getEndOffset();
            }

            int caretIndex = getCaretModel().getOffset();

            if (caretIndex < composedStartIndex) {
                return caretIndex;
            }
            if (caretIndex < composedEndIndex) {
                return composedStartIndex;
            }
            return caretIndex - (composedEndIndex - composedStartIndex);
        }

        @NotNull
        private String getText(int startIdx, int endIdx) {
            if (startIdx >= 0 && endIdx > startIdx) {
                CharSequence chars = getDocument().getImmutableCharSequence();
                return chars.subSequence(startIdx, endIdx).toString();
            }

            return "";
        }

        @NotNull
        @Override
        public AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex, AttributedCharacterIterator.Attribute[] attributes) {
            int composedStartIndex = 0;
            int composedEndIndex = 0;
            if (composedText != null) {
                composedStartIndex = composedTextRange.getStartOffset();
                composedEndIndex = composedTextRange.getEndOffset();
            }

            String committed;
            if (beginIndex < composedStartIndex) {
                if (endIndex <= composedStartIndex) {
                    committed = getText(beginIndex, endIndex - beginIndex);
                }
                else {
                    int firstPartLength = composedStartIndex - beginIndex;
                    committed = getText(beginIndex, firstPartLength) + getText(composedEndIndex, endIndex - beginIndex - firstPartLength);
                }
            }
            else {
                committed = getText(beginIndex + composedEndIndex - composedStartIndex, endIndex - beginIndex);
            }

            return new AttributedString(committed).getIterator();
        }

        @Override
        public int getCommittedTextLength() {
            int length = getDocument().getTextLength();
            if (composedText != null) {
                length -= composedText.length();
            }
            return length;
        }

        @Override
        @Nullable
        public AttributedCharacterIterator cancelLatestCommittedText(AttributedCharacterIterator.Attribute[] attributes) {
            return null;
        }

        @Override
        @Nullable
        public AttributedCharacterIterator getSelectedText(AttributedCharacterIterator.Attribute[] attributes) {
            String text = getSelectionModel().getSelectedText();
            return text == null ? null : new AttributedString(text).getIterator();
        }

        private void createComposedString(int composedIndex, @NotNull AttributedCharacterIterator text) {
            StringBuilder strBuf = new StringBuilder();

            // create attributed string with no attributes
            for (char c = text.setIndex(composedIndex); c != CharacterIterator.DONE; c = text.next()) {
                strBuf.append(c);
            }

            composedText = strBuf.toString();
        }

        private void setInputMethodCaretPosition(@NotNull InputMethodEvent e) {
            if (composedText != null) {
                int dot = composedTextRange.getStartOffset();

                TextHitInfo caretPos = e.getCaret();
                if (caretPos != null) {
                    dot += caretPos.getInsertionIndex();
                }

                getCaretModel().moveToOffset(dot);
                getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            }
        }

        private void runUndoTransparent(@NotNull final Runnable runnable) {
            CommandProcessor.getInstance().runUndoTransparentAction(
                () -> CommandProcessor.getInstance().executeCommand(myProject, () -> ApplicationManager.getApplication().runWriteAction(runnable),
                    "", getDocument(), UndoConfirmationPolicy.DEFAULT, getDocument()));
        }

        private boolean hasRelevantCommittedText(@NotNull InputMethodEvent e) {
            if (e.getCommittedCharacterCount() <= 0) return false;
            AttributedCharacterIterator text = e.getText();
            return text == null || text.first() != 0xA5 /* Yen character */;
        }

        private void replaceInputMethodText(@NotNull InputMethodEvent e) {
            if (myNeedToSelectPreviousChar && SystemInfo.isMac &&
                (Registry.is("ide.mac.pressAndHold.brute.workaround") || Registry.is("ide.mac.pressAndHold.workaround") &&
                    (hasRelevantCommittedText(e) || e.getCaret() == null))) {
                // This is required to support input of accented characters using press-and-hold method (http://support.apple.com/kb/PH11264).
                // JDK currently properly supports this functionality only for TextComponent/JTextComponent descendants.
                // For our editor component we need this workaround.
                // After https://bugs.openjdk.java.net/browse/JDK-8074882 is fixed, this workaround should be replaced with a proper solution.
                myNeedToSelectPreviousChar = false;
                getCaretModel().runForEachCaret(caret -> {
                    int caretOffset = caret.getOffset();
                    if (caretOffset > 0) {
                        caret.setSelection(caretOffset - 1, caretOffset);
                    }
                });
            }

            boolean isCaretMoved = false;
            int caretPositionToRestore = 0;

            int commitCount = e.getCommittedCharacterCount();
            AttributedCharacterIterator text = e.getText();

            // old composed text deletion
            final Document doc = getDocument();

            if (composedText != null) {
                if (!isViewer() && doc.isWritable()) {
                    runUndoTransparent(() -> {
                        int docLength = doc.getTextLength();
                        ProperTextRange range = composedTextRange.intersection(new TextRange(0, docLength));
                        if (range != null) {
                            doc.deleteString(range.getStartOffset(), range.getEndOffset());
                        }
                    });
                    isCaretMoved = getCaretModel().getOffset() != composedTextRange.getStartOffset();
                    if (isCaretMoved) {
                        caretPositionToRestore = getCaretModel().getCurrentCaret().getOffset();
                        // if caret set furter in the doc, we should add commitCount
                        if (caretPositionToRestore > composedTextRange.getStartOffset()) {
                            caretPositionToRestore += commitCount;
                        }
                        getCaretModel().moveToOffset(composedTextRange.getStartOffset());
                    }
                }
                composedText = null;
            }

            if (text != null) {
                text.first();

                // committed text insertion
                if (commitCount > 0) {
                    for (char c = text.current(); commitCount > 0; c = text.next(), commitCount--) {
                        if (c >= 0x20 && c != 0x7F) { // Hack just like in javax.swing.text.DefaultEditorKit.DefaultKeyTypedAction
                            processKeyTyped(c);
                        }
                    }
                }

                // new composed text insertion
                if (!isViewer() && doc.isWritable()) {
                    int composedTextIndex = text.getIndex();
                    if (composedTextIndex < text.getEndIndex()) {
                        createComposedString(composedTextIndex, text);

                        runUndoTransparent(() -> EditorModificationUtil.insertStringAtCaret(IpwEditorImpl.this, composedText, false, false));

                        composedTextRange = ProperTextRange.from(getCaretModel().getOffset(), composedText.length());
                    }
                }
            }

            if (isCaretMoved) {
                getCaretModel().moveToOffset(caretPositionToRestore);
            }
        }
    }

    @SuppressWarnings("unused")
    boolean useEditorAntialiasing() {
        return myUseEditorAntialiasing;
    }

    @SuppressWarnings("unused")
    public void setUseEditorAntialiasing(boolean value) {
        myUseEditorAntialiasing = value;
    }

    @SuppressWarnings("unused")
    static boolean handleDrop(@NotNull IpwEditorImpl editor, @NotNull final Transferable t, int dropAction) {
        return true;
    }

    private class EditorDocumentAdapter implements PrioritizedDocumentListener {
        @Override
        public void beforeDocumentChange(@NotNull DocumentEvent e) {
            beforeChangedUpdate(e);
        }

        @Override
        public void documentChanged(@NotNull DocumentEvent e) {
            changedUpdate(e);
        }

        @Override
        public void bulkUpdateStarting(@NotNull Document document) {
            bulkUpdateStarted();
        }

        @Override
        public void bulkUpdateFinished(@NotNull Document document) {
            IpwEditorImpl.this.bulkUpdateFinished();
        }

        @Override
        public int getPriority() {
            return EditorDocumentPriorities.EDITOR_DOCUMENT_ADAPTER;
        }
    }

    @Override
    @NotNull
    public EditorGutter getGutter() {
        return getGutterComponentEx();
    }

    @SuppressWarnings("unused")
    public boolean isInDistractionFreeMode() {
        return EditorUtil.isRealFileEditor(this)
            && (Registry.is("editor.distraction.free.mode") || isInPresentationMode());
    }

    boolean isInPresentationMode() {
        return UISettings.getInstance().getPresentationMode() && EditorUtil.isRealFileEditor(this);
    }

    @Override
    public void putInfo(@NotNull Map<? super String, ? super String> info) {
        final VisualPosition visual = getCaretModel().getVisualPosition();
        info.put("caret", visual.getLine() + ":" + visual.getColumn());
    }

    @Override
    public void codeStyleSettingsChanged(@NotNull CodeStyleSettingsChangeEvent event) {
    }

    @SuppressWarnings("unused")
    public void bidiTextFound() {
        // if (myProject != null && myVirtualFile != null && replace(CONTAINS_BIDI_TEXT, null, Boolean.TRUE)) {
        //     EditorNotifications.getInstance(myProject).updateNotifications(myVirtualFile);
        // }
    }

    @SuppressWarnings("unused")
    @TestOnly
    void validateState() {
        myView.validateState();
        mySoftWrapModel.validateState();
        myFoldingModel.validateState();
        myCaretModel.validateState();
        myInlayModel.validateState();
    }

    @Override
    public String toString() {
        return "EditorImpl[" + FileDocumentManager.getInstance().getFile(myDocument) + "]";
    }

    private class DefaultPopupHandler extends ContextMenuPopupHandler {
        @Nullable
        @Override
        public ActionGroup getActionGroup(@NotNull EditorMouseEvent event) {
            String contextMenuGroupId = myContextMenuGroupId;
            Inlay<?> inlay = myInlayModel.getElementAt(event.getMouseEvent().getPoint());
            if (inlay != null) {
                ActionGroup group = inlay.getRenderer().getContextMenuGroup(inlay);
                if (group != null) return group;
                String inlayContextMenuGroupId = inlay.getRenderer().getContextMenuGroupId(inlay);
                if (inlayContextMenuGroupId != null) contextMenuGroupId = inlayContextMenuGroupId;
            }
            return ContextMenuPopupHandler.getGroupForId(contextMenuGroupId);
        }
    }

    /*@DirtyUI
    private final class MyScrollPane extends JBScrollPane {
        private MyScrollPane() {
            setupCorners();
        }

        @Override
        public void setUI(ScrollPaneUI ui) {
            // disable standard Swing keybindings
            setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
        }

        @Override
        public void layout() {

        }

        @Override
        protected void processMouseWheelEvent(@NotNull MouseWheelEvent e) {
        }

        @NotNull
        @Override
        public JScrollBar createHorizontalScrollBar() {
            return new OpaqueAwareScrollBar(Adjustable.HORIZONTAL);
        }

        @NotNull
        @Override
        public JScrollBar createVerticalScrollBar() {
            return new MyScrollBar(Adjustable.VERTICAL);
        }

        @Override
        protected void setupCorners() {
            super.setupCorners();
        }
    }*/

    private class MyTextDrawingCallback implements TextDrawingCallback {
        @Override
        public void drawChars(@NotNull Graphics g,
                              char[] data,
                              int start,
                              int end,
                              int x,
                              int y,
                              @NotNull Color color,
                              @NotNull FontInfo fontInfo) {
        }
    }

    private static class NullEditorHighlighter extends EmptyEditorHighlighter {
        private static final TextAttributes NULL_ATTRIBUTES = new TextAttributes();

        NullEditorHighlighter() {
            super(NULL_ATTRIBUTES);
        }

        @Override
        public void setAttributes(TextAttributes attributes) {}

        @Override
        public void setColorScheme(@NotNull EditorColorsScheme scheme) {}
    }
}
