#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$ROOT_DIR/gradle/wrapper/gradle-wrapper.jar"
GRADLE_VERSION="8.10.2"

if [[ -f "$WRAPPER_JAR" ]]; then
  exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
fi

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

LOCAL_GRADLE="$ROOT_DIR/.gradle-local/gradle-$GRADLE_VERSION/bin/gradle"
if [[ -x "$LOCAL_GRADLE" ]]; then
  exec "$LOCAL_GRADLE" "$@"
fi

mkdir -p "$ROOT_DIR/.gradle-local"
DIST_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
DIST_ZIP="$ROOT_DIR/.gradle-local/gradle-$GRADLE_VERSION-bin.zip"

echo "No Gradle installation or wrapper jar found. Attempting to download Gradle $GRADLE_VERSION..." >&2
if command -v curl >/dev/null 2>&1; then
  curl -fL "$DIST_URL" -o "$DIST_ZIP"
elif command -v wget >/dev/null 2>&1; then
  wget -O "$DIST_ZIP" "$DIST_URL"
else
  echo "Cannot download Gradle: neither curl nor wget is available. Install Gradle $GRADLE_VERSION+ or add gradle/wrapper/gradle-wrapper.jar." >&2
  exit 127
fi

if command -v unzip >/dev/null 2>&1; then
  unzip -q "$DIST_ZIP" -d "$ROOT_DIR/.gradle-local"
else
  echo "Cannot unpack Gradle: unzip is not available. Install unzip or Gradle $GRADLE_VERSION+." >&2
  exit 127
fi

exec "$LOCAL_GRADLE" "$@"
