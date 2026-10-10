#!/usr/bin/env bash
# Full local verification loop for Water0.
#
# Runs unit tests, builds the debug APK, AND compiles the androidTest
# sources. That last one matters: the androidTest fake once lagged the
# repository interface while everything else was green, because the loop
# only ran testDebugUnitTest + assembleDebug. Never shorten this list.
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-$HOME/.jdk/17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleAndroidTest \
    --no-daemon --stacktrace
