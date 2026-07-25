#!/usr/bin/env bash
# PostToolUse hook: format a single edited Kotlin file with the ktlint CLI.
#
# Deliberately uses the standalone ktlint binary, NOT ./gradlew ktlintFormat —
# a Gradle invocation on every file edit would be too slow.
# Exits 0 in every path so a formatting problem never blocks Claude.

set -uo pipefail

input=$(cat)

file=$(printf '%s' "$input" | python3 -c \
  'import sys,json
try:
    d = json.load(sys.stdin)
    print(d.get("tool_input", {}).get("file_path", ""))
except Exception:
    print("")' 2>/dev/null || true)

[ -z "$file" ] && exit 0
[ -f "$file" ] || exit 0

case "$file" in
  *.kt|*.kts) ;;
  *) exit 0 ;;
esac

command -v ktlint >/dev/null 2>&1 || exit 0

ktlint --format --log-level=none "$file" >/dev/null 2>&1 || true
exit 0
