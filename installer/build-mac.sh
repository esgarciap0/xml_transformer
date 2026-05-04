#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${APP_NAME:-XML Transformer}"
VERSION="${VERSION:-1.0.0}"
VENDOR="${VENDOR:-EdwinSoftware}"
MAIN_CLASS="${MAIN_CLASS:-xml.json.transformer.Main}"
ICON_PATH="${ICON_PATH:-../src/main/resources/app.icns}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MVNW="$PROJECT_ROOT/mvnw"

if [[ ! -x "$MVNW" ]]; then
  echo "Maven wrapper is not executable: $MVNW" >&2
  echo "Run: chmod +x mvnw installer/build-mac.sh" >&2
  exit 1
fi

command -v jpackage >/dev/null 2>&1 || {
  echo "jpackage is not available in PATH." >&2
  exit 1
}

DIST_ROOT="$PROJECT_ROOT/dist"
MAC_DIST="$DIST_ROOT/mac"
APP_IMAGE_DEST="$MAC_DIST/app-image"
INSTALLER_DEST="$MAC_DIST/installer"
APP_IMAGE_DIR="$APP_IMAGE_DEST/$APP_NAME.app"

echo "Building shaded JAR..."
(cd "$PROJECT_ROOT" && "$MVNW" -q -DskipTests clean package)

JAR="$(find "$PROJECT_ROOT/target" -maxdepth 1 -name '*-shaded.jar' | head -n 1)"
if [[ -z "$JAR" ]]; then
  echo "No *-shaded.jar found in target." >&2
  exit 1
fi

RESOURCE_DIR="$PROJECT_ROOT/src/main/resources"
ICON_ABS=""
if [[ -f "$SCRIPT_DIR/$ICON_PATH" ]]; then
  ICON_ABS="$(cd "$(dirname "$SCRIPT_DIR/$ICON_PATH")" && pwd)/$(basename "$ICON_PATH")"
else
  echo "Icon not found, jpackage will use the default icon: $ICON_PATH"
fi

rm -rf "$MAC_DIST"
mkdir -p "$APP_IMAGE_DEST" "$INSTALLER_DEST"

echo "Generating macOS app image..."
APP_IMAGE_ARGS=(
  --type app-image
  --name "$APP_NAME"
  --app-version "$VERSION"
  --vendor "$VENDOR"
  --input "$PROJECT_ROOT/target"
  --main-jar "$(basename "$JAR")"
  --main-class "$MAIN_CLASS"
  --resource-dir "$RESOURCE_DIR"
  --dest "$APP_IMAGE_DEST"
)
if [[ -n "$ICON_ABS" ]]; then
  APP_IMAGE_ARGS+=(--icon "$ICON_ABS")
fi
jpackage "${APP_IMAGE_ARGS[@]}"

if [[ ! -d "$APP_IMAGE_DIR" ]]; then
  echo "Expected app image was not generated: $APP_IMAGE_DIR" >&2
  exit 1
fi

echo "Generating macOS DMG installer..."
DMG_ARGS=(
  --type dmg
  --name "$APP_NAME"
  --app-version "$VERSION"
  --vendor "$VENDOR"
  --app-image "$APP_IMAGE_DIR"
  --dest "$INSTALLER_DEST"
)
if [[ -n "$ICON_ABS" ]]; then
  DMG_ARGS+=(--icon "$ICON_ABS")
fi
jpackage "${DMG_ARGS[@]}"

echo "macOS build completed."
echo "App image: $APP_IMAGE_DIR"
echo "Installer output: $INSTALLER_DEST"
