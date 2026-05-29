# Qwen Image

An Android app for AI-powered image generation and editing, built with Jetpack Compose and powered by Alibaba Cloud's DashScope API (Qwen-Image models).

## Features

- **Text-to-Image** - Generate images from text prompts using Qwen-Image models
- **Image Editing** - Edit existing images with AI assistance
- **Gallery** - Browse and manage generated images
- **Multiple Models** - Support for multiple Qwen-Image model versions
- **Customizable Sizes** - Various output image dimensions
- **Secure API Key Storage** - API keys encrypted with AES-256 via EncryptedSharedPreferences
- **Material 3 Design** - Modern UI with dynamic color support (Android 12+)
- **Bilingual UI** - Chinese and English localization

## Screenshots

<!-- TODO: Add screenshots -->

## Requirements

- Android 7.0 (API 24) or higher
- A DashScope API key from [Alibaba Cloud](https://dashscope.aliyun.com/)

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/Rooftop-Love/qwen_image.git
   ```

2. Open the project in Android Studio (Ladybug or newer recommended).

3. Build and run on a device or emulator.

4. On first launch, go to **Settings** and enter your DashScope API key.

## Build

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM with Repository pattern |
| Networking | OkHttp + Kotlin Serialization |
| Build | Gradle 9.4.1 (Kotlin DSL) + AGP 9.2.1 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

## Project Structure

```
app/src/main/java/com/example/qwen_image/
├── MainActivity.kt              # Entry point
├── QwenImageApp.kt              # Application class (DI, HTTP client)
├── data/
│   ├── local/                   # Local storage (images, preferences)
│   ├── remote/                  # DashScope API client & models
│   └── repository/              # Data repository
└── ui/
    ├── components/              # Reusable Compose components
    ├── gallery/                 # Image gallery screen
    ├── imageedit/               # Image editing screen
    ├── texttoimage/             # Text-to-image screen
    ├── settings/                # Settings screen
    ├── localization/            # String resources (zh/en)
    ├── navigation/              # Navigation graph
    └── theme/                   # Material 3 theme
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [DashScope API](https://dashscope.aliyun.com/) by Alibaba Cloud
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3](https://m3.material.io/)
