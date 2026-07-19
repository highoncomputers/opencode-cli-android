#!/bin/bash
set -euo pipefail

echo "========================================"
echo "  OpenCode CLI Android APK Builder"
echo "========================================"
echo ""

# Detect architecture
ARCH=$(uname -m)
echo "Host architecture: $ARCH"

# Set up Android SDK environment
if [ -n "${ANDROID_HOME:-}" ]; then
    echo "Android SDK home: $ANDROID_HOME"
elif [ -n "${ANDROID_SDK_ROOT:-}" ]; then
    echo "Android SDK root: $ANDROID_SDK_ROOT"
    export ANDROID_HOME="$ANDROID_SDK_ROOT"
else
    # Try to find Android SDK
    POSSIBLE_SDK_PATHS=(
        "/usr/local/lib/android/sdk"
        "/opt/android-sdk"
        "$HOME/android-sdk"
        "$HOME/Android/Sdk"
    )
    for sdk_path in "${POSSIBLE_SDK_PATHS[@]}"; do
        if [ -d "$sdk_path" ]; then
            echo "Found Android SDK at: $sdk_path"
            export ANDROID_HOME="$sdk_path"
            break
        fi
    done
fi

if [ -z "${ANDROID_HOME:-}" ]; then
    echo "ERROR: Android SDK not found!"
    echo "Please set ANDROID_HOME or ANDROID_SDK_ROOT"
    echo "Installing Android SDK command-line tools..."

    mkdir -p /opt/android-sdk
    cd /opt/android-sdk

    # Download command-line tools
    CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    wget -q "$CMDLINE_TOOLS_URL" -O cmdline-tools.zip
    unzip -q cmdline-tools.zip -d temp
    mkdir -p cmdline-tools
    mv temp/* cmdline-tools/latest/ 2>/dev/null || mv temp/cmdline-tools/* cmdline-tools/ 2>/dev/null || true
    rm -rf temp cmdline-tools.zip

    export ANDROID_HOME="/opt/android-sdk"
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

    # Accept licenses and install SDK
    yes | sdkmanager --licenses >/dev/null 2>&1 || true
    sdkmanager "platforms;android-34" "build-tools;34.0.0" >/dev/null 2>&1 || true
fi

export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

# Check Java
JAVA_VERSION=$(java -version 2>&1 | head -1)
echo "Java: $JAVA_VERSION"

echo ""
echo "Building APK..."
echo ""

# Navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$SCRIPT_DIR"

# Make gradlew executable
if [ ! -f gradlew ]; then
    echo "Generating Gradle wrapper..."
    gradle wrapper --distribution-type bin 2>/dev/null || {
        echo "Gradle not found, downloading..."
        mkdir -p gradle/wrapper
        cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
        # Download gradle-wrapper.jar
        GRADLE_WRAPPER_JAR_URL="https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar"
        wget -q "$GRADLE_WRAPPER_JAR_URL" -O gradle/wrapper/gradle-wrapper.jar 2>/dev/null || {
            echo "Cannot download gradle wrapper jar, installing gradle directly..."
            apt-get install -y gradle 2>/dev/null || true
        }
    }
fi

if [ -f gradlew ]; then
    chmod +x gradlew
    ./gradlew assembleRelease --no-daemon 2>&1 | tail -20
elif command -v gradle &>/dev/null; then
    gradle assembleRelease --no-daemon 2>&1 | tail -20
else
    echo "ERROR: Cannot find gradle. Please install it."
    exit 1
fi

echo ""
echo "========================================"
echo "  Build Complete!"
echo "========================================"

# Find the APK
APK_PATH=$(find app/build/outputs/apk -name "*.apk" 2>/dev/null | head -1)
if [ -n "$APK_PATH" ]; then
    echo "APK generated at: $APK_PATH"
    ls -lh "$APK_PATH"
    
    # Create artifacts directory
    mkdir -p artifacts
    cp "$APK_PATH" artifacts/
    echo "APK copied to artifacts/"
else
    echo "No APK found at expected path."
    echo "Checking build outputs..."
    find app/build -name "*.apk" 2>/dev/null || true
fi

echo ""
echo "Done."
