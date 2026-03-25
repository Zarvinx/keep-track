#!/bin/bash
[ -f .env ] && export $(grep -v '^#' .env | xargs)
./gradlew --no-daemon test
