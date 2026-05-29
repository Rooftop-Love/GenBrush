# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Qwen_Image is an Android app built with Jetpack Compose and Kotlin. It is currently a fresh project from the Android Studio template, with the package `com.example.qwen_image`.

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.example.qwen_image.ExampleUnitTest"

# Run instrumented (on-device) tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Tech Stack

- **Language**: Kotlin (2.2.10)
- **UI**: Jetpack Compose with Material 3 (BOM 2026.02.01)
- **Build**: Gradle 9.4.1, AGP 9.2.1, Kotlin DSL build scripts
- **Min SDK**: 24 (Android 7.0), **Target/Compile SDK**: 36
- **Dependency catalog**: `gradle/libs.versions.toml` (version catalog)

## Architecture

- Single-module Gradle project (`app`)
- `MainActivity` is the entry point, using `ComponentActivity` with Compose `setContent`
- Theme: `Qwen_ImageTheme` in `ui/theme/` with dynamic color support (Android 12+)
- All source lives under `app/src/main/java/com/example/qwen_image/`

## Key Conventions

- Build scripts use Kotlin DSL (`.gradle.kts`) with version catalog aliases from `libs.versions.toml`
- Compose dependencies are managed via the Compose BOM — do not pin individual Compose library versions
- ProGuard/R8 minification is disabled for release builds by default
