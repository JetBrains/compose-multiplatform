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

#include "skiko_sdl_core.h"

#include <SDL.h>

#ifndef GL_SILENCE_DEPRECATION
#define GL_SILENCE_DEPRECATION
#endif
#ifdef __APPLE__
#include <OpenGL/OpenGL.h>
#include <OpenGL/gl3.h>
#else
#include <GL/gl.h>
#endif

#include <algorithm>
#include <chrono>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <iostream>
#include <stdexcept>

namespace skiko::core {

namespace {

std::runtime_error SDLError(const char* operation) {
    return std::runtime_error(std::string(operation) + ": " + SDL_GetError());
}

} // namespace

struct SkikoSdlWindow::Impl {
    int width = 0;
    int height = 0;
    bool open = true;
    SDL_Window* window = nullptr;
    SDL_GLContext gl_context = nullptr;
#ifdef __APPLE__
    CGLContextObj cgl_context = nullptr;
#endif
    GLuint read_framebuffer = 0;

    ~Impl() {
        if (window != nullptr && gl_context != nullptr) {
#ifdef __APPLE__
            if (cgl_context != nullptr) {
                CGLSetCurrentContext(cgl_context);
            }
#endif
            if (read_framebuffer != 0) {
                glDeleteFramebuffers(1, &read_framebuffer);
            }
            SDL_GL_DeleteContext(gl_context);
        }
        if (window != nullptr) {
            SDL_DestroyWindow(window);
        }
        SDL_QuitSubSystem(SDL_INIT_VIDEO);
    }
};

SkikoSdlWindow::SkikoSdlWindow(int width, int height, const std::string& title)
    : impl_(std::make_unique<Impl>()) {
    SDL_SetMainReady();
    if (SDL_InitSubSystem(SDL_INIT_VIDEO) != 0) {
        throw SDLError("SDL_InitSubSystem");
    }

    impl_->width = width;
    impl_->height = height;

    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 2);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
    SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);
    SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 24);
    SDL_GL_SetAttribute(SDL_GL_STENCIL_SIZE, 8);

    impl_->window = SDL_CreateWindow(
        title.c_str(),
        SDL_WINDOWPOS_CENTERED,
        SDL_WINDOWPOS_CENTERED,
        width,
        height,
        SDL_WINDOW_SHOWN | SDL_WINDOW_ALLOW_HIGHDPI | SDL_WINDOW_OPENGL | SDL_WINDOW_RESIZABLE
    );

    if (impl_->window == nullptr) {
        throw SDLError("SDL_CreateWindow");
    }

    impl_->gl_context = SDL_GL_CreateContext(impl_->window);
    if (impl_->gl_context == nullptr) {
        throw SDLError("SDL_GL_CreateContext");
    }

    if (SDL_GL_MakeCurrent(impl_->window, impl_->gl_context) != 0) {
        throw SDLError("SDL_GL_MakeCurrent");
    }

#ifdef __APPLE__
    impl_->cgl_context = CGLGetCurrentContext();
    if (impl_->cgl_context == nullptr) {
        throw std::runtime_error("SDL did not expose a native CGL context");
    }
#endif

    SDL_GL_SetSwapInterval(0);
    glGenFramebuffers(1, &impl_->read_framebuffer);
}

SkikoSdlWindow::~SkikoSdlWindow() = default;

bool SkikoSdlWindow::IsOpen() const {
    return impl_->open;
}

void SkikoSdlWindow::SetTitle(const std::string& title) {
    if (impl_->window != nullptr) {
        SDL_SetWindowTitle(impl_->window, title.c_str());
    }
}

void SkikoSdlWindow::GetWindowSize(int* width, int* height) const {
    if (impl_->window != nullptr) {
        SDL_GetWindowSize(impl_->window, width, height);
    }
}

void SkikoSdlWindow::GetDrawableSize(int* width, int* height) const {
    if (impl_->window != nullptr) {
        SDL_GL_GetDrawableSize(impl_->window, width, height);
    }
}

void SkikoSdlWindow::PollEvents(HostPointerEventCallback callback, void* user_data) {
    int pixel_width = impl_->width;
    int pixel_height = impl_->height;
    if (impl_->window != nullptr) {
        SDL_GL_GetDrawableSize(impl_->window, &pixel_width, &pixel_height);
    }
    int window_width = impl_->width;
    int window_height = impl_->height;
    if (impl_->window != nullptr) {
        SDL_GetWindowSize(impl_->window, &window_width, &window_height);
    }
    const double scale_x = (window_width > 0) ? static_cast<double>(pixel_width) / window_width : 1.0;
    const double scale_y = (window_height > 0) ? static_cast<double>(pixel_height) / window_height : 1.0;

    SDL_Event event;
    while (SDL_PollEvent(&event)) {
        if (event.type == SDL_QUIT) {
            impl_->open = false;
            if (impl_->window != nullptr) {
                SDL_HideWindow(impl_->window);
            }
        } else if (event.type == SDL_WINDOWEVENT) {
            if (event.window.windowID == SDL_GetWindowID(impl_->window)) {
                if (event.window.event == SDL_WINDOWEVENT_CLOSE) {
                    impl_->open = false;
                    if (impl_->window != nullptr) {
                        SDL_HideWindow(impl_->window);
                    }
                } else if (event.window.event == SDL_WINDOWEVENT_RESIZED ||
                           event.window.event == SDL_WINDOWEVENT_SIZE_CHANGED) {
                    impl_->width = event.window.data1;
                    impl_->height = event.window.data2;
                }
            }
        } else if (callback != nullptr) {
            switch (event.type) {
                case SDL_MOUSEBUTTONDOWN:
                    callback(
                        HOST_POINTER_EVENT_PRESS,
                        event.button.x * scale_x,
                        event.button.y * scale_y,
                        event.button.button,
                        0.0,
                        0.0,
                        user_data
                    );
                    break;
                case SDL_MOUSEBUTTONUP:
                    callback(
                        HOST_POINTER_EVENT_RELEASE,
                        event.button.x * scale_x,
                        event.button.y * scale_y,
                        event.button.button,
                        0.0,
                        0.0,
                        user_data
                    );
                    break;
                case SDL_MOUSEMOTION:
                    callback(
                        HOST_POINTER_EVENT_MOVE,
                        event.motion.x * scale_x,
                        event.motion.y * scale_y,
                        (event.motion.state != 0) ? 1 : 0,
                        0.0,
                        0.0,
                        user_data
                    );
                    break;
                case SDL_MOUSEWHEEL: {
                    int mouse_x = 0;
                    int mouse_y = 0;
                    SDL_GetMouseState(&mouse_x, &mouse_y);
                    callback(
                        HOST_POINTER_EVENT_SCROLL,
                        mouse_x * scale_x,
                        mouse_y * scale_y,
                        0,
                        event.wheel.preciseX * -20.0,
                        event.wheel.preciseY * -20.0,
                        user_data
                    );
                    break;
                }
                default:
                    break;
            }
        }
    }
}

void SkikoSdlWindow::Present(const std::vector<HostCanvasTexture>& canvases) {
    if (!impl_->open || impl_->window == nullptr || canvases.empty()) {
        return;
    }

#ifdef __APPLE__
    if (CGLSetCurrentContext(impl_->cgl_context) != kCGLNoError) {
        throw std::runtime_error("CGLSetCurrentContext failed for presentation context");
    }
#else
    SDL_GL_MakeCurrent(impl_->window, impl_->gl_context);
#endif

    int pixel_width = 0;
    int pixel_height = 0;
    SDL_GL_GetDrawableSize(impl_->window, &pixel_width, &pixel_height);

    int window_width = impl_->width;
    int window_height = impl_->height;
    SDL_GetWindowSize(impl_->window, &window_width, &window_height);

    const double scale_x = (window_width > 0) ? static_cast<double>(pixel_width) / window_width : 1.0;
    const double scale_y = (window_height > 0) ? static_cast<double>(pixel_height) / window_height : 1.0;

    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
    glViewport(0, 0, pixel_width, pixel_height);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_STENCIL_TEST);
    glDisable(GL_BLEND);
    glEnable(GL_SCISSOR_TEST);
    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
    glScissor(0, 0, pixel_width, pixel_height);
    glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);

    if (canvases.size() == 1) {
        const auto& canvas = canvases[0];
        glBindFramebuffer(GL_READ_FRAMEBUFFER, impl_->read_framebuffer);
        glFramebufferTexture2D(
            GL_READ_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            canvas.texture_id,
            0
        );

        if (glCheckFramebufferStatus(GL_READ_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw std::runtime_error("Shared canvas framebuffer is incomplete");
        }

        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glScissor(0, 0, pixel_width, pixel_height);

        glBlitFramebuffer(
            0,
            0,
            canvas.source_width,
            canvas.source_height,
            0,
            0,
            pixel_width,
            pixel_height,
            GL_COLOR_BUFFER_BIT,
            GL_LINEAR
        );
    } else {
        for (const auto& canvas : canvases) {
            const int x = static_cast<int>(canvas.x * scale_x);
            const int top = static_cast<int>(canvas.y * scale_y);
            const int width = static_cast<int>(canvas.display_width * scale_x);
            const int height = static_cast<int>(canvas.display_height * scale_y);
            const int bottom = pixel_height - top - height;

            glBindFramebuffer(GL_READ_FRAMEBUFFER, impl_->read_framebuffer);
            glFramebufferTexture2D(
                GL_READ_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D,
                canvas.texture_id,
                0
            );

            if (glCheckFramebufferStatus(GL_READ_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw std::runtime_error("Shared canvas framebuffer is incomplete");
            }

            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
            glScissor(x, bottom, width, height);

            glBlitFramebuffer(
                0,
                0,
                canvas.source_width,
                canvas.source_height,
                x,
                bottom,
                x + width,
                bottom + height,
                GL_COLOR_BUFFER_BIT,
                GL_LINEAR
            );
        }
    }

    glDisable(GL_SCISSOR_TEST);
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    SDL_GL_SwapWindow(impl_->window);
}

void* SkikoSdlWindow::NativeGLContext() const {
#ifdef __APPLE__
    if (impl_->cgl_context != nullptr) {
        CGLSetCurrentContext(impl_->cgl_context);
    }
    return impl_->cgl_context;
#else
    return impl_->gl_context;
#endif
}

SkikoAssetLoader::SkikoAssetLoader() = default;

SkikoAssetLoader::SkikoAssetLoader(const std::vector<std::string>& search_paths)
    : search_paths_(search_paths) {}

void SkikoAssetLoader::AddSearchPath(const std::string& path) {
    search_paths_.push_back(path);
}

HostAsset SkikoAssetLoader::LoadAsset(const std::string& relative_or_absolute_path) const {
    std::vector<std::string> candidate_paths;
    candidate_paths.reserve(1 + search_paths_.size());
    candidate_paths.push_back(relative_or_absolute_path);

    for (const auto& base : search_paths_) {
        std::string full_path = base;
        if (!full_path.empty() && full_path.back() != '/') {
            full_path += '/';
        }
        full_path += relative_or_absolute_path;
        candidate_paths.push_back(full_path);
    }

    for (const auto& path : candidate_paths) {
        std::ifstream file(path, std::ios::binary | std::ios::ate);
        if (file.is_open()) {
            std::streamsize size = file.tellg();
            file.seekg(0, std::ios::beg);
            auto* buffer = new uint8_t[size];
            if (file.read(reinterpret_cast<char*>(buffer), size)) {
                HostAsset asset;
                asset.data = buffer;
                asset.size = static_cast<size_t>(size);
                asset.internal_handle = buffer;
                return asset;
            }
            delete[] buffer;
        }
    }

    HostAsset empty{};
    return empty;
}

void SkikoAssetLoader::FreeAsset(HostAsset* asset) {
    if (asset != nullptr && asset->internal_handle != nullptr) {
        delete[] static_cast<uint8_t*>(asset->internal_handle);
        asset->data = nullptr;
        asset->size = 0;
        asset->internal_handle = nullptr;
    }
}

double GetMonotonicTimeMillis() {
    using namespace std::chrono;
    const auto now = steady_clock::now().time_since_epoch();
    return duration_cast<duration<double, std::milli>>(now).count();
}

uint64_t GetMonotonicTimeNanos() {
    using namespace std::chrono;
    const auto now = steady_clock::now().time_since_epoch();
    return static_cast<uint64_t>(duration_cast<nanoseconds>(now).count());
}

} // namespace skiko::core
