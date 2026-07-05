#!/bin/sh
# Enforce the CLDC 1.1 / MIDP 2.0 (Java 1.3-level) constraints documented in
# CLAUDE.md. Fails if any BlackBerry-incompatible construct appears under src/.
#
# Patterns are deliberately conservative to avoid false positives from Javadoc
# HTML (e.g. <b>, <ul>) — they only match real language usage.
#
# Portable across GNU grep (CI) and BSD grep (macOS): no \b / \< word anchors.
SRC=src
fail=0

scan() {
  desc=$1
  pattern=$2
  matches=$(grep -rnE --include='*.java' "$pattern" "$SRC" 2>/dev/null || true)
  if [ -n "$matches" ]; then
    echo "FORBIDDEN: $desc"
    printf '%s\n' "$matches" | sed 's/^/  /'
    echo
    fail=1
  fi
}

scan "import java.text.* (no SimpleDateFormat/DecimalFormat in CLDC)" \
     'import[[:space:]][[:space:]]*java\.text\.'
scan "java.lang.reflect.* (CLDC has no reflection)" \
     'java\.lang\.reflect'
scan "StringBuilder (use StringBuffer)" \
     'StringBuilder'
scan "String.split( (not in CLDC String)" \
     '\.split[[:space:]]*\('
scan "String.format( / printf-style (not in CLDC)" \
     '\.format[[:space:]]*\('
scan "generics" \
     '(List|Map|Set|Vector|Hashtable|ArrayList|HashMap|LinkedList|Collection|Iterator|Enumeration)[[:space:]]*<'
scan "enhanced for-loop (for-each)" \
     'for[[:space:]]*\([^;]*:'
scan "enum declaration" \
     'enum[[:space:]][A-Za-z]'
scan "annotations (Java 5+)" \
     '@(Override|SuppressWarnings|Deprecated|FunctionalInterface|Functional)'

if [ "$fail" -ne 0 ]; then
  echo "CLDC guardrail check FAILED."
  exit 1
fi
echo "CLDC guardrail check passed: $(grep -rl '' --include='*.java' "$SRC" | wc -l | tr -d ' ') source files clean."
