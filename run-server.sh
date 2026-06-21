#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ -f .env ]; then
  set -a
  source ./.env
  set +a
fi

mvn -pl server -am install -DskipTests -q

mvn -pl server exec:java -Dexec.mainClass=server.net.GameServer
