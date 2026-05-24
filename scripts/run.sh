#!/usr/bin/env sh
set -eu

mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=${1:-8080}"
