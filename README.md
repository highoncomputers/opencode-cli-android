# OpenCode CLI for Android

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.0-brightgreen)]()

> **OpenCode** — The open source AI coding agent, now available on Android.

This project packages the [OpenCode CLI](https://github.com/anomalyco/opencode) as an Android APK, allowing you to run AI-powered coding commands directly from your Android device.

## Features

- **Terminal Emulator** — Full terminal environment with OpenCode CLI pre-configured
- **Built-in Node.js** — Downloads the latest Node.js runtime for ARM64 Android
- **OpenCode CLI** — Automatically installs the latest OpenCode package from npm
- **Material Design** — Clean, modern UI that follows Android design guidelines
- **Session Management** — Persistent terminal sessions

## Architecture

```
app/
├── src/main/java/ai/opencode/android/
│   ├── MainActivity.kt          # Landing screen
│   ├── TerminalActivity.kt      # Terminal emulator with shell process
│   ├── SettingsActivity.kt      # Setup & configuration
│   ├── OpenCodeApplication.kt   # Application class
│   └── opencode/
│       └── OpenCodeService.kt   # Foreground service
├── src/main/res/
│   ├── layout/                   # UI layouts
│   ├── values/                   # Themes, colors, strings
│   ├── drawable/                 # Vector icons
│   └── xml/                      # Network security config
└── build.gradle.kts              # Module build config
build.gradle.kts                   # Root build config
settings.gradle.kts                # Project settings
```

## How It Works

1. **First Launch** — The app downloads the latest Node.js LTS for ARM64 Linux and installs the OpenCode CLI via npm
2. **Terminal** — A full shell environment is started with the OpenCode CLI available in `PATH`
3. **Usage** — Type `opencode --help` to see available commands, or `opencode run` to start a coding session

### Available Commands

The full OpenCode CLI is available inside the terminal:

| Command | Description |
|---------|-------------|
| `opencode run` | Start an interactive coding session |
| `opencode --help` | Show all available commands |
| `opencode --version` | Show version |
| `opencode serve` | Start the OpenCode server |
| `opencode models` | List available AI models |

## Building from Source

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34+
- JDK 17+

### Build Steps

```bash
# Clone the repository
git clone https://github.com/highoncomputers/opencode-cli-android.git
cd opencode-cli-android

# Build the APK
./gradlew assembleRelease

# The APK will be at:
# app/build/outputs/apk/release/app-release.apk
```

### Building with GitHub Actions

The repository includes a GitHub Actions workflow that builds the APK automatically on every push and creates a release.

## Setup Process

On first launch, navigate to **Settings** and tap **Run Setup**. The setup process:

1. Downloads Node.js v22.14.0 for ARM64 (~40MB)
2. Extracts the Node.js runtime
3. Installs OpenCode CLI via npm
4. Verifies the installation

> **Note:** A stable internet connection is required for the initial setup. The download is ~40MB.

## Development

### Project Structure

- `app/` — Android application module
- `script/build-apk.sh` — Build script for CI/CD
- `.devcontainer/devcontainer.json` — GitHub Codespaces configuration

## License

MIT License — see [LICENSE](LICENSE) for details.

## Acknowledgments

- [OpenCode](https://github.com/anomalyco/opencode) — The open source coding agent
- [Node.js](https://nodejs.org/) — JavaScript runtime
- [Termux](https://termux.com/) — Terminal emulator for Android (inspiration)
