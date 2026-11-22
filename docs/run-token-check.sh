#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/Test.kt" <<'KOTLIN'
import com.parkcast.mirror.SessionToken

fun main() {
    val a = SessionToken.generate()
    val b = SessionToken.generate()
    check(a != b)
    check(a.matches(Regex("[A-Za-z0-9_-]{32}")))
    check(SessionToken.matches(a, a))
    check(!SessionToken.matches(a, b))
    check(!SessionToken.matches(a, ""))
    println("PASS: token randomness, format, equality, and rejection")
}
KOTLIN
kotlinc "$ROOT/app/src/main/java/com/parkcast/mirror/SessionToken.kt" "$TMP/Test.kt" -include-runtime -d "$TMP/test.jar"
java -jar "$TMP/test.jar"
