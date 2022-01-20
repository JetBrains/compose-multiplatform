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

package android.view;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Stubs for RenderNode on M-P devices.
 */
public class RenderNode {
    @SuppressWarnings("UnusedVariable")
    private RenderNode(String name, View owningView) {
    }

    /** stub */
    public void destroy() {
    }

    /** stub */
    public static @NonNull RenderNode create(@Nullable String name, @Nullable View owningView) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public @NonNull DisplayListCanvas start(int width, int height) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public void end(@NonNull DisplayListCanvas canvas) {
    }

    /** stub */
    public void discardDisplayList() {
    }

    /**
     * Same stub as discardDisplayList however it was named destroyDisplayListData in Android M
     * and earlier
     */
    public void destroyDisplayListData() {
    }

    /** stub */
    public boolean isValid() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean hasIdentityMatrix() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public void getMatrix(@NonNull Matrix outMatrix) {
    }

    /** stub */
    public void getInverseMatrix(@NonNull Matrix outMatrix) {
    }

    /** stub */
    public boolean setLayerType(int layerType) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setLayerPaint(@Nullable Paint paint) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setClipBounds(@Nullable Rect rect) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setClipToBounds(boolean clipToBounds) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setProjectBackwards(boolean shouldProject) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setProjectionReceiver(boolean shouldReceive) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setOutline(@Nullable Outline outline) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean hasShadow() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setClipToOutline(boolean clipToOutline) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean getClipToOutline() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setRevealClip(boolean shouldClip,
            float x, float y, float radius) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setStaticMatrix(@NonNull Matrix matrix) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setAnimationMatrix(@NonNull Matrix matrix) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setAlpha(float alpha) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getAlpha() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setHasOverlappingRendering(boolean hasOverlappingRendering) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    @SuppressLint("KotlinPropertyAccess")
    public boolean hasOverlappingRendering() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setElevation(float lift) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getElevation() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setAmbientShadowColor(int color) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public int getAmbientShadowColor() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setSpotShadowColor(int color) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public int getSpotShadowColor() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setTranslationX(float translationX) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getTranslationX() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setTranslationY(float translationY) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getTranslationY() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setTranslationZ(float translationZ) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getTranslationZ() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setRotation(float rotation) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getRotation() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setRotationX(float rotationX) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getRotationX() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setRotationY(float rotationY) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getRotationY() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setScaleX(float scaleX) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getScaleX() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setScaleY(float scaleY) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getScaleY() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setPivotX(float pivotX) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getPivotX() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setPivotY(float pivotY) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getPivotY() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean isPivotExplicitlySet() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setCameraDistance(float distance) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public float getCameraDistance() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setLeft(int left) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setTop(int top) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setRight(int right) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setBottom(int bottom) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean setLeftTopRightBottom(int left, int top, int right, int bottom) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean offsetLeftAndRight(int offset) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean offsetTopAndBottom(int offset) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public void output() {
    }

    /** stub */
    public boolean isAttached() {
        throw new UnsupportedOperationException();
    }
}
