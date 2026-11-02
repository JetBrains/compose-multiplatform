#!/usr/bin/env python3
"""
High-performance, isolated parallel test runner for wasm-sdl benchmarks.

Executes each benchmark in a dedicated temporary workspace using Node.js and native_skiko.node.
Features:
- Full process-group isolation (clean timeout kill of child processes)
- Independent JSON report directories (zero file contention or race conditions)
- Fresh report verification (checks mtime and condition parameters)
- Per-benchmark log capture with full error diagnostics on failure
- Configurable concurrency, timeouts, and frame counts
"""

import argparse
import concurrent.futures
import json
import os
import shutil
import signal
import subprocess
import sys
import tempfile
import time

ALL_BENCHMARKS = [
    "AnimatedVisibility",
    "CanvasDrawing",
    "HeavyShader",
    "LazyGrid",
    "LazyGrid-ItemLaunchedEffect",
    "LazyGrid-SmoothScroll",
    "LazyGrid-SmoothScroll-ItemLaunchedEffect",
    "LazyList",
    "MultipleComponents",
    "MultipleComponents-NoVectorGraphics",
    "TextLayout",
    "VisualEffects"
]

def find_node_executable():
    node_prop = os.environ.get("NODE_BINARY")
    if node_prop and os.path.exists(node_prop):
        return node_prop
    for candidate in [
        shutil.which("node"),
        "/opt/homebrew/bin/node",
        "/usr/local/bin/node",
        "/usr/bin/node"
    ]:
        if candidate and os.path.exists(candidate) and os.access(candidate, os.X_OK):
            return candidate
    return "node"

def find_native_skiko(project_root):
    env_path = os.environ.get("SKIKO_NATIVE_GL_ADDON")
    if env_path and os.path.isfile(env_path):
        return os.path.abspath(env_path)
    return None

def parse_duration_ms(val):
    if not val or val == "N/A":
        return None
    if isinstance(val, (int, float)):
        return float(val)
    if isinstance(val, str) and val.startswith("PT") and val.endswith("S"):
        try:
            return float(val[2:-1]) * 1000.0
        except ValueError:
            return None
    try:
        return float(val)
    except ValueError:
        return None

def run_isolated_benchmark(
    benchmark_name,
    package_dir,
    native_skiko_path,
    node_bin,
    logs_dir,
    archive_dir,
    frame_count,
    warmup_count,
    timeout_sec
):
    start_time = time.time()
    log_file_path = os.path.join(logs_dir, f"{benchmark_name}.log")
    
    result = {
        "benchmark": benchmark_name,
        "success": False,
        "duration_sec": 0.0,
        "error": None,
        "cpu_ms": None,
        "gpu_ms": None,
        "fps": None,
        "log_file": log_file_path
    }

    with tempfile.TemporaryDirectory(prefix=f"bench_{benchmark_name}_") as temp_dir:
        # Copy package files into temp workspace to ensure __dirname resolves to temp_dir
        for item in os.listdir(package_dir):
            if item == "build":
                continue
            src = os.path.join(package_dir, item)
            dst = os.path.join(temp_dir, item)
            if os.path.isdir(src):
                shutil.copytree(src, dst, symlinks=False)
            else:
                shutil.copy2(src, dst)

        reports_dir = os.path.join(temp_dir, "build/benchmarks/json-reports")
        os.makedirs(reports_dir, exist_ok=True)

        env = os.environ.copy()
        env["BENCHMARK_MODE"] = "true"
        env["BENCHMARK_REPORTS_DIR"] = reports_dir
        if native_skiko_path:
            env["SKIKO_NATIVE_GL_ADDON"] = native_skiko_path

        cmd = [
            node_bin,
            "launcher_sdl.mjs",
            f"benchmarks={benchmark_name}",
            f"frameCount={frame_count}",
            f"warmupCount={warmup_count}",
            f"reportsDir={reports_dir}",
            "saveStatsToJSON=true"
        ]

        proc = None
        try:
            with open(log_file_path, "w") as log_file:
                proc = subprocess.Popen(
                    cmd,
                    cwd=temp_dir,
                    env=env,
                    stdout=log_file,
                    stderr=subprocess.STDOUT,
                    text=True,
                    start_new_session=True
                )
                try:
                    proc.wait(timeout=timeout_sec)
                except subprocess.TimeoutExpired:
                    # Kill entire process group on timeout
                    try:
                        os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
                    except Exception:
                        proc.kill()
                    proc.wait()
                    result["duration_sec"] = time.time() - start_time
                    result["error"] = f"Timeout after {timeout_sec}s (log: {log_file_path})"
                    return result

            result["duration_sec"] = time.time() - start_time

            if proc.returncode != 0:
                with open(log_file_path, "r") as lf:
                    log_tail = lf.read()[-1500:]
                result["error"] = f"Process exited with code {proc.returncode}:\n{log_tail}"
                return result

            # Verify report output
            report_file = os.path.join(reports_dir, f"{benchmark_name}.json")
            if not os.path.exists(report_file):
                result["error"] = f"Report JSON not found at {report_file}"
                return result

            file_mtime = os.path.getmtime(report_file)
            if file_mtime < start_time - 1.0:
                result["error"] = f"Report file is stale (mtime {file_mtime} < start {start_time})"
                return result

            with open(report_file, "r") as rf:
                data = json.load(rf)

            conditions = data.get("conditions", {})
            actual_fc = conditions.get("frameCount")
            actual_wc = conditions.get("warmupCount")
            if actual_fc != frame_count or actual_wc != warmup_count:
                result["error"] = f"Condition mismatch: expected {frame_count}/{warmup_count}, got {actual_fc}/{actual_wc}"
                return result

            frame_info = data.get("averageFrameInfo", {})
            fps_info = data.get("averageFPSInfo", {})

            result["cpu_ms"] = parse_duration_ms(frame_info.get("cpuTime"))
            result["gpu_ms"] = parse_duration_ms(frame_info.get("gpuTime"))
            result["fps"] = fps_info.get("fps")
            result["success"] = True

            if archive_dir:
                os.makedirs(archive_dir, exist_ok=True)
                dst_report = os.path.join(archive_dir, f"{benchmark_name}.json")
                shutil.copy2(report_file, dst_report)

        except Exception as e:
            result["duration_sec"] = time.time() - start_time
            result["error"] = f"Exception: {str(e)}"
            if proc and proc.poll() is None:
                try:
                    os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
                except Exception:
                    proc.kill()
                proc.wait()

    return result

def main():
    parser = argparse.ArgumentParser(description="Isolated parallel runner for wasm-sdl benchmarks")
    parser.add_argument("--benchmarks", default=",".join(ALL_BENCHMARKS), help="Comma-separated benchmark names")
    parser.add_argument("--concurrency", type=int, default=1, help="Max concurrent benchmark processes (default: 1; >1 for smoke testing only)")
    parser.add_argument("--frameCount", type=int, default=100, help="Number of benchmark frames (default: 100)")
    parser.add_argument("--warmupCount", type=int, default=10, help="Number of warmup frames (default: 10)")
    parser.add_argument("--timeout", type=int, default=60, help="Per-benchmark timeout in seconds (default: 60)")
    parser.add_argument("--build", action="store_true", help="Compile wasm distribution before running")
    parser.add_argument("--archive", default=None, help="Directory to archive JSON reports (e.g. benchmarks/build/benchmarks/archive/wasm-sdl/1.11.1_run1)")
    args = parser.parse_args()

    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    package_dir = os.path.join(project_root, "build/wasm/packages/compose-benchmarks-benchmarks/kotlin")
    node_bin = find_node_executable()
    native_skiko = find_native_skiko(project_root)

    print(f"Node Executable: {node_bin}")
    print(f"Native Skiko Addon: {native_skiko}")

    if not native_skiko:
        print("[ERROR] Native Skiko addon path is required. Set SKIKO_NATIVE_GL_ADDON=/path/to/native_skiko.node.")
        sys.exit(1)

    if args.concurrency > 1:
        print("[WARN] Concurrency > 1 is enabled for smoke testing/diagnostics only; GPU timing metrics may be contaminated by concurrent rendering workloads.")

    if args.build or not os.path.exists(os.path.join(package_dir, "compose-benchmarks-benchmarks.mjs")):
        print("Compiling Kotlin/Wasm production executable...")
        gradle_cmd = [
            "./gradlew",
            ":benchmarks:wasmJsProcessResources",
            ":benchmarks:wasmJsProductionExecutableCompileSync",
            "-Pkotlin.incremental.wasm=false",
            "-Pkotlin.incremental=false"
        ]
        build_res = subprocess.run(gradle_cmd, cwd=project_root)
        if build_res.returncode != 0:
            print("Gradle build failed!")
            sys.exit(build_res.returncode)

    logs_dir = os.path.join(project_root, "benchmarks/build/benchmarks/logs")
    os.makedirs(logs_dir, exist_ok=True)

    archive_dir = args.archive
    if archive_dir and not os.path.isabs(archive_dir):
        archive_dir = os.path.join(project_root, archive_dir)

    shared_reports_dir = os.path.join(project_root, "benchmarks/build/benchmarks/json-reports")
    before_snapshot = {}
    if os.path.exists(shared_reports_dir):
        for f in os.listdir(shared_reports_dir):
            p = os.path.join(shared_reports_dir, f)
            if os.path.isfile(p):
                before_snapshot[f] = os.path.getmtime(p)

    benchmarks = [b.strip() for b in args.benchmarks.split(",") if b.strip()]
    print(f"\nRunning {len(benchmarks)} benchmarks with concurrency={args.concurrency}, frameCount={args.frameCount}, warmupCount={args.warmupCount}, timeout={args.timeout}s\n")

    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        future_map = {
            executor.submit(
                run_isolated_benchmark,
                b,
                package_dir,
                native_skiko,
                node_bin,
                logs_dir,
                archive_dir,
                args.frameCount,
                args.warmupCount,
                args.timeout
            ): b for b in benchmarks
        }
        for future in concurrent.futures.as_completed(future_map):
            res = future.result()
            results.append(res)
            status = "PASS" if res["success"] else "FAIL"
            cpu_str = f"{res['cpu_ms']:.2f} ms" if res['cpu_ms'] is not None else "N/A"
            gpu_str = f"{res['gpu_ms']:.2f} ms" if res['gpu_ms'] is not None else "N/A"
            fps_str = f"{res['fps']:.1f}" if res['fps'] is not None else "N/A"
            print(f"[{status}] {res['benchmark']:45s} | CPU: {cpu_str:>9} | GPU: {gpu_str:>9} | FPS: {fps_str:>7} | Time: {res['duration_sec']:.1f}s")
            if not res["success"]:
                print(f"       Error: {res['error']}")

    # Verify shared directory was not contaminated
    if os.path.exists(shared_reports_dir):
        after_snapshot = {}
        for f in os.listdir(shared_reports_dir):
            p = os.path.join(shared_reports_dir, f)
            if os.path.isfile(p):
                after_snapshot[f] = os.path.getmtime(p)
        for f, mtime in after_snapshot.items():
            if f not in before_snapshot or before_snapshot[f] != mtime:
                print(f"[ERROR] Isolation assertion failed: shared report file {f} was modified in {shared_reports_dir}")
                sys.exit(1)

    print("\n" + "=" * 80)
    print("SUMMARY")
    print("=" * 80)
    results.sort(key=lambda r: r["benchmark"])
    passed = sum(1 for r in results if r["success"])
    failed = len(results) - passed
    print(f"Total: {len(results)}, Passed: {passed}, Failed: {failed}")
    if failed > 0:
        sys.exit(1)

if __name__ == "__main__":
    main()
