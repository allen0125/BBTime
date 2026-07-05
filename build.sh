#!/bin/sh
# Build bbtime.cod for BlackBerry OS 4.5+ using RIM's rapc compiler.
#
# Requirements:
#   - A BlackBerry JDE / Component Pack (>= 4.5) that provides rapc (as the
#     native `bin/rapc` launcher OR `bin/rapc.jar`) and `net_rim_api.jar`. rapc
#     is a RIM tool; it is NOT part of this repo and cannot be installed from a
#     package manager. Point BB_JDE at the SDK root.
#   - A JDK on PATH (rapc drives javac + preverify).
#
# Environment:
#   BB_JDE   (required) SDK root; expects bin/rapc(.jar) and lib/net_rim_api.jar
#   BB_API   (optional) explicit path to net_rim_api.jar (overrides the default)
#
# Usage:
#   BB_JDE=/path/to/BlackBerry/JDE ./build.sh
#
# Output:
#   build/bbtime.cod   (+ .cso/.jar/.jad siblings) ready to load onto a device
#   or into the BlackBerry simulator.
set -e

: "${BB_JDE:?Set BB_JDE to your BlackBerry JDE / Component Pack root (must contain bin/rapc or bin/rapc.jar and lib/net_rim_api.jar)}"

API="${BB_API:-$BB_JDE/lib/net_rim_api.jar}"
APP="bbtime"
OUT="build"

# Resolve how to invoke rapc:
#   - native launcher (Windows/macOS JDE): $BB_JDE/bin/rapc
#   - portable jar (Linux/CI):             java -jar $BB_JDE/bin/rapc.jar
#   - rapc already on PATH
if [ -x "$BB_JDE/bin/rapc" ]; then
  RAPC="$BB_JDE/bin/rapc"
elif [ -f "$BB_JDE/bin/rapc.exe" ]; then
  RAPC="$BB_JDE/bin/rapc.exe"
elif [ -f "$BB_JDE/bin/rapc.jar" ]; then
  RAPC="java -jar $BB_JDE/bin/rapc.jar"
elif command -v rapc >/dev/null 2>&1; then
  RAPC="rapc"
else
  echo "rapc not found (looked for $BB_JDE/bin/rapc, rapc.exe, rapc.jar, and PATH)" >&2
  exit 1
fi

[ -f "$API" ] || { echo "net_rim_api.jar not found at: $API (set BB_API to override)" >&2; exit 1; }

rm -rf "$OUT"
mkdir -p "$OUT"

# Gather all sources (rapc wants explicit file arguments).
SOURCES=$(find src -name '*.java')
[ -n "$SOURCES" ] || { echo "no .java sources found under src/" >&2; exit 1; }

echo "Compiling with: $RAPC"
# $RAPC is intentionally unquoted so the "java -jar ..." form splits into words.
$RAPC \
  import="$API" \
  codename="$OUT/$APP" \
  -quiet \
  "$APP.rapc" \
  $SOURCES

echo "Built $OUT/$APP.cod"
echo "Install with the BlackBerry Desktop Manager (bbtime.alx) or load via the simulator."
