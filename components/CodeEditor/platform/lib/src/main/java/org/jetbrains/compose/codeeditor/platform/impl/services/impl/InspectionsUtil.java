package org.jetbrains.compose.codeeditor.platform.impl.services.impl;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionProfileKt;
import com.intellij.codeInspection.ex.InspectionToolRegistrar;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.InspectionToolsSupplier;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.profile.codeInspection.BaseInspectionProfileManager;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.UtilKt;

import java.lang.reflect.Field;
import java.util.UUID;

final class InspectionsUtil {

    private static final Field myToolField;

    static {
        try {
            myToolField = ReflectionUtil
                .findField(InspectionToolWrapper.class, InspectionProfileEntry.class, "myTool");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private InspectionsUtil() {}

    static void configureInspections(Project project, Disposable rootDisposable) {
        var tools = LocalInspectionTool.EMPTY_ARRAY;
        var toolSupplier = new InspectionToolsSupplier.Simple(UtilKt.mapSmart(tools, InspectionToolRegistrar::wrapTool));
        Disposer.register(rootDisposable, toolSupplier);
        var profile = new InspectionProfileImpl(UUID.randomUUID().toString(), toolSupplier,
            ((BaseInspectionProfileManager)InspectionProfileManager.getInstance()));
        var profileManager = ProjectInspectionProfileManager.getInstance(project);
        Disposer.register(rootDisposable, () -> {
            profileManager.deleteProfile(profile);
            profileManager.setCurrentProfile(null);
            clearAllToolsIn(InspectionProfileKt.getBASE_PROFILE());
        });

        profileManager.addProfile(profile);
        profileManager.setCurrentProfile(profile);
    }

    private static void clearAllToolsIn(InspectionProfileImpl profile) {
        if (!profile.wasInitialized()) return;

        for (var state : profile.getAllTools()) {
            var wrapper = state.getTool();
            if (wrapper.getExtension() != null) {
                ReflectionUtil.resetField(wrapper, myToolField);
            }
        }
    }

}
