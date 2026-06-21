#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ -n "${1:-}" ]; then
  export JAMMATCH_CLIENT_TOKEN="$1"
fi

mvn -pl common -am install -DskipTests -q

mvn -pl client org.openjfx:javafx-maven-plugin:0.0.8:run
