#!/bin/sh
set -eu
launch_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec sh "$launch_dir/launch.sh" "$@"
