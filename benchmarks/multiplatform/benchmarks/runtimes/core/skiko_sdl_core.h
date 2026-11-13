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

#ifndef SKIKO_SDL_CORE_H
#define SKIKO_SDL_CORE_H

#include "../abi/host_abi.h"

#include <cstddef>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

namespace skiko::core {

class SkikoSdlWindow {
public:
    SkikoSdlWindow(int width, int height, const std::string& title);
    ~SkikoSdlWindow();

    SkikoSdlWindow(const SkikoSdlWindow&) = delete;
    SkikoSdlWindow& operator=(const SkikoSdlWindow&) = delete;

    bool IsOpen() const;
    void SetTitle(const std::string& title);
    void GetWindowSize(int* width, int* height) const;
    void GetDrawableSize(int* width, int* height) const;

    void PollEvents(HostPointerEventCallback callback, void* user_data);
    void Present(const std::vector<HostCanvasTexture>& canvases);
    void* NativeGLContext() const;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

class SkikoAssetLoader {
public:
    SkikoAssetLoader();
    explicit SkikoAssetLoader(const std::vector<std::string>& search_paths);

    void AddSearchPath(const std::string& path);
    HostAsset LoadAsset(const std::string& relative_or_absolute_path) const;
    static void FreeAsset(HostAsset* asset);

private:
    std::vector<std::string> search_paths_;
};

double GetMonotonicTimeMillis();
uint64_t GetMonotonicTimeNanos();

} // namespace skiko::core

#endif // SKIKO_SDL_CORE_H
