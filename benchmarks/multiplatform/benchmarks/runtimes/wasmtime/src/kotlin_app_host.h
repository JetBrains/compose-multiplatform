#pragma once

#include <chrono>
#include <cstdint>
#include <deque>
#include <memory>
#include <string>
#include <vector>

#include <wasmtime.h>

namespace skiko::core {
class SkikoSdlWindow;
}

class SkikoHost;

class KotlinAppHost {
public:
    KotlinAppHost(
        wasmtime_context_t* context,
        SkikoHost* skiko,
        const std::string& benchmark_args = "",
        skiko::core::SkikoSdlWindow* window = nullptr
    );
    ~KotlinAppHost();

    KotlinAppHost(const KotlinAppHost&) = delete;
    KotlinAppHost& operator=(const KotlinAppHost&) = delete;

    void RegisterImports(
        wasmtime_linker_t* linker,
        const wasmtime_module_t* application_module
    );
    void AttachInstance(const wasmtime_instance_t& instance);
    void Start();
    size_t AnimationCallbackCount() const;
    bool HasPendingWork() const;
    bool ShouldExit() const;
    void RunNextAnimationCallback(double timestamp_millis);
    void DrainReadyCallbacks();
    void RunExpiredTimers(const std::chrono::steady_clock::time_point& now);
    std::chrono::steady_clock::time_point NextDeadline(const std::chrono::steady_clock::time_point& default_deadline) const;
    void DispatchPointerEvent(
        int32_t event_type,
        double x,
        double y,
        int32_t button,
        double scroll_delta_x,
        double scroll_delta_y
    );
    const std::string& Title() const;

private:
    struct HostValue;
    struct Callback {
        enum class Signature { UnitToUnit, JsToJs, JsToUnit, DoubleToUnit };
        Signature signature = Signature::JsToJs;
        wasmtime_anyref_t function{};
        wasmtime_func_t trampoline{};
        bool has_trampoline = false;

        ~Callback();
    };
    struct ReadyCallback {
        std::shared_ptr<Callback> callback;
        HostValue* arg = nullptr;
    };
    struct Timer {
        uint32_t id = 0;
        std::chrono::steady_clock::time_point due_time;
        std::shared_ptr<Callback> callback;
        bool is_interval = false;
        uint32_t interval_ms = 0;
    };
    struct Binding;

    static void DeleteHostValue(void* value);
    static void DeleteBinding(void* value);

    static wasm_trap_t* Dispatch(
        void* environment,
        wasmtime_caller_t* caller,
        const wasmtime_val_t* args,
        size_t nargs,
        wasmtime_val_t* results,
        size_t nresults
    );
    wasm_trap_t* CallHost(
        const Binding& binding,
        wasmtime_caller_t* caller,
        const wasmtime_val_t* args,
        size_t nargs,
        wasmtime_val_t* results,
        size_t nresults
    );
    bool Invoke(const std::shared_ptr<Callback>& callback, double timestamp, HostValue* arg = nullptr);
    void SetExternResult(wasmtime_val_t* result, HostValue* value);
    HostValue* GetHostValue(const wasmtime_val_t& value) const;
    std::string GetString(const wasmtime_val_t& value) const;

    wasmtime_context_t* context_;
    SkikoHost* skiko_;
    wasmtime_instance_t instance_{};
    bool attached_ = false;
    HostValue* last_thrown_exception_ = nullptr;
    std::deque<ReadyCallback> ready_callbacks_;
    std::deque<std::shared_ptr<Callback>> animation_callbacks_;
    std::shared_ptr<Callback> pointer_callback_;
    std::shared_ptr<Callback> coroutine_window_process_;
    std::vector<Timer> timers_;
    uint32_t next_timer_id_ = 1;
    int next_animation_id_ = 1;
    std::string title_ = "Compose MPP Demo (WASM + Wasmtime Native SDL)";
    std::string benchmark_args_;
    skiko::core::SkikoSdlWindow* window_ = nullptr;
    bool should_exit_ = false;
};
