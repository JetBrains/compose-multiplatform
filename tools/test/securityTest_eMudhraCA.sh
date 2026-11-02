#!/usr/bin/env bash

# Check that security issues with compromised eMudhra root certificate is resolved on the machine
# See https://emudhra.we-have-a-problem.com/ for details
# This script is AI generated, serves as a temporary security check, and not suppose to be maintained long term

# Run with Bash (including Git Bash). No trust settings are modified.
set -uo pipefail

tmp=$(mktemp -d) || exit 1
trap 'rm -rf "$tmp"' EXIT
failed=0

# from 
hosts=(
  dv-g3b-emudhra.we-have-a-problem.com
  dv-g3b-aaa-emudhra.we-have-a-problem.com
  dv-g3b-noct-emudhra.we-have-a-problem.com
  dv-g3b-aaa-noct-emudhra.we-have-a-problem.com
)

# 0 = connection accepted, 42 = certificate rejected, anything else = error.
# Expiry, hostname mismatch, and network errors are inconclusive.
certificate_rejected() {
  grep -Eqi 'CRYPT_E_REVOKED|CERT_E_REVOKED|CERT_E_UNTRUSTEDROOT|CERT_E_CHAINING|certificate (is |has been )?revoked|certificate revoked|CERT_REVOKED|CERT_UNTRUSTED|CERT_REJECTED|UNABLE_TO_GET_ISSUER_CERT|UNABLE_TO_VERIFY_LEAF_SIGNATURE|SELF_SIGNED_CERT_IN_CHAIN|DEPTH_ZERO_SELF_SIGNED_CERT|unable to get (local )?issuer certificate|unable to verify the first certificate|self[- ]signed certificate|issuer.*(unknown|not recognized)|certificate.*not trusted|unknown CA' "$1"
}

# A broken installation is unavailable, rather than a certificate-test failure.
# Probe startup separately so TLS/control errors from a working client remain ERROR.
startup_check() {
  local name=$1 rc
  shift
  "$@" >"$tmp/startup-log" 2>&1
  rc=$?
  if [[ $rc != 0 ]]; then
    echo "SKIP $name: installation cannot start (exit $rc)"
    return 1
  fi
  return 0
}

check() {
  local name=$1 control=$2 host rc
  shift 2
  "$@" "$control" >"$tmp/log" 2>&1
  rc=$?
  if [[ $rc != 0 ]]; then
    echo "ERROR $name: healthy control failed (exit $rc)"
    cat "$tmp/log" >&2
    failed=1
    return
  fi
  for host in "${hosts[@]}"; do
    "$@" "https://$host/" >"$tmp/log" 2>&1
    rc=$?
    case $rc in
      42) ;;
      0) echo "FAIL $name: accepted $host"; failed=1; return ;;
      *) echo "ERROR $name: inconclusive for $host (exit $rc)"; cat "$tmp/log" >&2; failed=1; return ;;
    esac
  done
  echo "PASS $name"
}

curl_check() {
  local rc
  LC_ALL=C curl --silent --show-error --connect-timeout 10 --max-time 20 \
    --output /dev/null "$1" >"$tmp/curl-log" 2>&1
  rc=$?
  if [[ $rc == 35 || $rc == 60 ]] && certificate_rejected "$tmp/curl-log"; then
    return 42
  fi
  return "$rc"
}

wget_check() {
  local rc
  LC_ALL=C wget --tries=1 --timeout=20 --output-document=/dev/null "$1" \
    >"$tmp/wget-log" 2>&1
  rc=$?
  if [[ $rc == 5 ]] && certificate_rejected "$tmp/wget-log"; then
    return 42
  fi
  # An HTTP error still establishes that TLS succeeded.
  [[ $rc == 8 ]] && return 0
  return "$rc"
}

git_check() {
  local rc
  LC_ALL=C GIT_TERMINAL_PROMPT=0 GIT_TRACE_CURL=1 GIT_TRACE_CURL_NO_DATA=1 \
    git -c http.lowSpeedLimit=1 -c http.lowSpeedTime=20 \
    ls-remote "$1" >"$tmp/git-log" 2>&1
  rc=$?
  if [[ $rc != 0 ]] && certificate_rejected "$tmp/git-log"; then
    return 42
  fi
  # A final HTTPS response proves TLS completed, even for a non-Git endpoint.
  if [[ $rc == 0 ]] || grep -Eqi '<= Recv header: HTTP/[0-9.]+ [2345][0-9][0-9]' "$tmp/git-log"; then
    return 0
  fi
  return 1
}

python_check() {
  "$python_bin" - "$1" <<'PY'
from __future__ import print_function
import ssl
import sys
try:
    import urllib.request as request
except ImportError:
    import urllib2 as request
try:
    request.urlopen(sys.argv[1], timeout=20).close()
except request.HTTPError:
    pass  # HTTP errors happen after successful TLS.
except Exception as error:
    reason = getattr(error, 'reason', error)
    if isinstance(reason, ssl.SSLError):
        # OpenSSL: unknown issuer, self-signed, revoked, or untrusted.
        if getattr(reason, 'verify_code', None) in (2, 18, 19, 20, 21, 23, 27, 28):
            sys.exit(42)
        text = str(reason).lower()
        if any(s in text for s in ('certificate revoked', 'certificate is revoked',
                                  'unable to get', 'self signed', 'self-signed')):
            sys.exit(42)
    sys.exit(1)
PY
}

node_check() {
  "$node_bin" - "$1" <<'JS'
const https = require('https');
const rejected = new Set([
  'CERT_REVOKED', 'UNABLE_TO_GET_ISSUER_CERT',
  'UNABLE_TO_GET_ISSUER_CERT_LOCALLY', 'UNABLE_TO_VERIFY_LEAF_SIGNATURE',
  'SELF_SIGNED_CERT_IN_CHAIN', 'DEPTH_ZERO_SELF_SIGNED_CERT',
  'CERT_UNTRUSTED', 'CERT_REJECTED'
]);
const request = https.get(process.argv[2], response => {
  response.destroy();
  process.exit(0);
});
const timer = setTimeout(() => process.exit(1), 20000);
request.on('error', error => {
  clearTimeout(timer);
  process.exit(rejected.has(error.code) ? 42 : 1);
});
JS
}

npm_check() {
  local rc
  LC_ALL=C npm ping --registry="$1" --fetch-retries=0 --fetch-timeout=20000 \
    >"$tmp/npm-log" 2>&1
  rc=$?
  if [[ $rc != 0 ]] && certificate_rejected "$tmp/npm-log"; then
    return 42
  fi
  # A non-registry endpoint can reject npm's HTTP request after successful TLS.
  if [[ $rc == 0 ]] || grep -Eq 'npm (ERR!|error) code E[45][0-9][0-9]' "$tmp/npm-log"; then
    return 0
  fi
  return 1
}

# Java 8-compatible bytecode avoids source-file launching (Java 11+ only).
cat >"$tmp/CheckTLS.java" <<'JAVA'
import java.net.URL;
import java.security.cert.CertPathValidatorException;
import java.security.cert.PKIXReason;
import javax.net.ssl.HttpsURLConnection;

class CheckTLS {
    public static void main(String[] args) {
        try {
            HttpsURLConnection c = (HttpsURLConnection) new URL(args[0]).openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(20000);
            c.connect();
            c.disconnect();
        } catch (Exception e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof CertPathValidatorException) {
                    CertPathValidatorException.Reason r =
                        ((CertPathValidatorException) cause).getReason();
                    if (r == CertPathValidatorException.BasicReason.REVOKED ||
                        r == PKIXReason.NO_TRUST_ANCHOR) System.exit(42);
                }
                if (cause.getClass().getName().endsWith("SunCertPathBuilderException"))
                    System.exit(42);
            }
            System.exit(1);
        }
    }
}
JAVA

native_path() {
  if command -v cygpath >/dev/null; then cygpath -m "$1"; else printf '%s\n' "$1"; fi
}

# Keep the discovered launcher path. Resolving Oracle's java-path links can
# produce a target that exists but cannot start directly on Windows.
# Only make Git Bash's implicit .exe lookup explicit, so java and java.exe
# entries for the same launcher are deduplicated by sort -u below.
resolve_java_bin() {
  local path=$1
  [[ -f ${path}.exe ]] && path=${path}.exe
  printf '%s\n' "$path"
}

java_check() {
  local rc
  "$java_bin" -cp "$(native_path "$tmp")" CheckTLS "$1"
  rc=$?
  if [[ $rc == 126 || $rc == 127 ]]; then
    printf 'Java launcher could not start: %s (exit %s)\n' "$java_bin" "$rc" >&2
    printf 'Try running "%s" -version to check the Java installation.\n' "$java_bin" >&2
  fi
  return "$rc"
}

control=https://example.com/
for client in curl wget git npm; do
  if command -v "$client" >/dev/null; then
    startup_check "$client" "$client" --version || continue
    if [[ $client == npm ]]; then
      check npm https://registry.npmjs.org/ npm_check
    else
      check "$client" "$control" "${client}_check"
    fi
  else
    echo "SKIP $client: not installed"
  fi
done

for python_bin in python3 python; do
  if command -v "$python_bin" >/dev/null; then
    startup_check "$python_bin" "$python_bin" --version || continue
    check "$python_bin" "$control" python_check
  else
    echo "SKIP $python_bin: not installed"
  fi
done

while IFS= read -r node_bin; do
  startup_check "$node_bin" "$node_bin" --version || continue
  check "$node_bin" "$control" node_check
done < <(
  { command -v node || true; find /usr/emsdk -type f -name node -executable 2>/dev/null; } | sort -u
)

# Discover standard installations, JAVA_HOME, and java on PATH.
{
  command -v java || true
  if [[ -n ${JAVA_HOME:-} ]]; then
    java_home_path=$JAVA_HOME
    command -v cygpath >/dev/null && java_home_path=$(cygpath -u "$JAVA_HOME")
    for java_candidate in "$java_home_path/bin/java" "$java_home_path/bin/java.exe"; do
      [[ -f $java_candidate ]] && printf '%s\n' "$java_candidate"
    done
  fi
  find /usr/lib/jvm /usr/java /opt "$HOME/.local" \
    '/c/Program Files/Java' '/c/Program Files/Eclipse Adoptium' \
    '/c/Program Files/Microsoft' '/c/Program Files/JetBrains' \
    -type f \( -name java -o -name java.exe \) 2>/dev/null
} | while IFS= read -r path; do resolve_java_bin "$path"; done | sort -u >"$tmp/javas"

# Filter before looking for a compiler: a stale Oracle helper must not prevent
# other runtimes from being tested or require a compiler when none can start.
: >"$tmp/working-javas"
while IFS= read -r java_bin; do
  if startup_check "$java_bin" "$java_bin" -version; then
    printf '%s\n' "$java_bin" >>"$tmp/working-javas"
  fi
done <"$tmp/javas"

if [[ -s $tmp/working-javas ]]; then
  compiler=$(command -v javac || true)
  if [[ -z $compiler ]]; then
    while IFS= read -r java_bin; do
      for candidate in "${java_bin%/*}/javac" "${java_bin%/*}/javac.exe"; do
        [[ -f $candidate ]] && compiler=$candidate && break 2
      done
    done <"$tmp/working-javas"
  fi
  if [[ -n $compiler ]] && "$compiler" -source 8 -target 8 \
      -d "$(native_path "$tmp")" "$(native_path "$tmp/CheckTLS.java")" \
      >"$tmp/javac-log" 2>&1; then
    while IFS= read -r java_bin; do
      check "$java_bin" "$control" java_check
    done <"$tmp/working-javas"
  else
    echo 'ERROR Java: a JDK compiler supporting -source 8 -target 8 is required'
    [[ -f $tmp/javac-log ]] && cat "$tmp/javac-log" >&2
    failed=1
  fi
else
  echo 'SKIP Java: no working runtime found'
fi

if [[ $failed != 0 ]]; then
  echo 'FAIL: one or more checks failed or were inconclusive.'
  exit 1
fi
echo 'PASS: all available client checks passed (see SKIP entries for untested installations).'
exit 0
