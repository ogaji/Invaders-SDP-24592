#!/bin/sh
set -eu
launch_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
java_command=java
if [ -n "${JAVA_HOME:-}" ]; then java_command="$JAVA_HOME/bin/java"; fi
exec "$java_command" -jar "$launch_dir/space-invaders-launcher.jar" "$@"
