#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
mode=${1:-same-process}
case "$mode" in same-process|separate-process) ;; *) echo "Usage: $0 [same-process|separate-process]" >&2; exit 2;; esac
mvn -q -DskipTests compile
if [ -n "${JAVA_HOME:-}" ]; then JAVA="$JAVA_HOME/bin/java"; else JAVA=java; fi
exec "$JAVA" -cp target/classes com.example.players.Main "$mode"
