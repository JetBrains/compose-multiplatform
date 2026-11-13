/*
 * Copyright 2026 The Android Open Source Project
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

#ifndef SKIKO_HOST_ABI_H
#define SKIKO_HOST_ABI_H

#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Standardized Compose WASM Pointer Event Types.
 * Matches androidx.compose.ui.input.pointer.PointerEventType mapping.
 */
typedef enum HostPointerEventType {
    HOST_POINTER_EVENT_UNKNOWN = 0,
    HOST_POINTER_EVENT_PRESS = 1,
    HOST_POINTER_EVENT_RELEASE = 2,
    HOST_POINTER_EVENT_MOVE = 3,
    HOST_POINTER_EVENT_ENTER = 4,
    HOST_POINTER_EVENT_EXIT = 5,
    HOST_POINTER_EVENT_SCROLL = 6
} HostPointerEventType;

/**
 * Standardized Pointer Buttons.
 */
typedef enum HostPointerButton {
    HOST_POINTER_BUTTON_PRIMARY = 1,
    HOST_POINTER_BUTTON_SECONDARY = 2,
    HOST_POINTER_BUTTON_TERTIARY = 4,
    HOST_POINTER_BUTTON_BACK = 8,
    HOST_POINTER_BUTTON_FORWARD = 16
} HostPointerButton;

/**
 * Texture and layout definition for presenting an offscreen canvas onto the window.
 */
typedef struct HostCanvasTexture {
    uint32_t texture_id;
    int32_t source_width;
    int32_t source_height;
    int32_t x;
    int32_t y;
    int32_t display_width;
    int32_t display_height;
} HostCanvasTexture;

/**
 * Callback signatures for event propagation.
 */
typedef void (*HostPointerEventCallback)(
    int32_t event_type,
    double x,
    double y,
    int32_t button,
    double scroll_delta_x,
    double scroll_delta_y,
    void* user_data
);

typedef void (*HostKeyEventCallback)(
    int32_t key_code,
    int32_t action,
    int32_t modifiers,
    void* user_data
);

typedef void (*HostResizeEventCallback)(
    int32_t width,
    int32_t height,
    void* user_data
);

typedef void (*HostAnimationFrameCallback)(
    double timestamp_millis,
    void* user_data
);

/**
 * Asset representation loaded by the host asset loader.
 */
typedef struct HostAsset {
    const uint8_t* data;
    size_t size;
    void* internal_handle;
} HostAsset;

#ifdef __cplusplus
}
#endif

#endif // SKIKO_HOST_ABI_H
