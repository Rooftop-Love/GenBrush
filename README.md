# GenBrush

AI 图像生成与编辑 Android 应用，基于 Jetpack Compose 构建，支持 DashScope（阿里云百炼）和 SD WebUI Forge 双后端。

## Features

- **文生图** - 输入提示词生成图片，支持反向提示词
- **图像编辑** - 选择本地图片，用提示词进行 AI 编辑
- **双后端** - DashScope 云端 API 或本地 SD WebUI Forge 服务器
- **LoRA 支持** - SD WebUI 后端可选择 LoRA 模型并调节权重
- **模型管理** - 预置模型 + 自定义模型，可自由启用/禁用
- **相册** - 浏览生成历史，支持搜索、类型筛选、排序、收藏、批量操作
- **图片详情** - 全屏查看、保存、分享、复制提示词、基于原图重新生成/编辑
- **数据迁移** - 导出/导入图片和设置数据
- **安全存储** - API Key 通过 AES-256 加密存储（EncryptedSharedPreferences）
- **Material 3** - 现代 UI，Android 12+ 支持动态取色
- **中英双语** - 支持中文/英文界面切换

## Requirements

- Android 7.0 (API 24) 或更高版本
- DashScope 后端：需要阿里云百炼平台 API Key（[申请地址](https://dashscope.aliyun.com/)）
- SD WebUI 后端：需要运行 SD WebUI Forge 并开启 API（局域网访问）

## Getting Started

1. 克隆仓库：
   ```bash
   git clone https://github.com/Rooftop-Love/genbrush.git
   ```

2. 在 Android Studio（建议 Ladybug 或更新版本）中打开项目。

3. 构建并运行到设备或模拟器。

4. 首次启动后进入 **设置**：
   - **DashScope 后端**：输入 API Key
   - **SD WebUI 后端**：输入服务器地址（手机和电脑需在同一局域网）

## Build

```bash
# 调试构建
./gradlew assembleDebug

# 发布构建
./gradlew assembleRelease

# 单元测试
./gradlew test

# 设备测试
./gradlew connectedAndroidTest
```

Windows 上使用 `gradlew.bat` 代替 `./gradlew`。

## Tech Stack

| 组件 | 技术 |
|---|---|
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository |
| 网络 | OkHttp + Gson |
| 图片加载 | Coil 3 |
| 本地存储 | Room + EncryptedSharedPreferences |
| 构建 | Gradle 9.4.1 (Kotlin DSL) + AGP 9.2.1 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

## Project Structure

```
app/src/main/java/com/example/genbrush/
├── GenBrushApp.kt                 # Application（HTTP 客户端、依赖初始化）
├── MainActivity.kt                # 入口 Activity
├── data/
│   ├── local/
│   │   ├── db/                    # Room 数据库（AppDatabase、ImageDao、ImageEntity）
│   │   ├── DataMigrationManager.kt # 数据导入/导出
│   │   ├── ImageSaver.kt          # 保存图片到系统相册（OOM 保护）
│   │   ├── ImageStore.kt          # 图片文件存储与管理
│   │   └── PreferencesManager.kt  # 加密偏好设置
│   ├── remote/
│   │   ├── DashScopeApi.kt        # DashScope API 客户端
│   │   ├── StableDiffusionApi.kt  # SD WebUI API 客户端
│   │   └── model/                 # API 数据模型
│   └── repository/
│       └── GenerationRepository.kt # 数据仓库（双后端调度）
└── ui/
    ├── common/                    # 通用工具（ErrorMapper）
    ├── components/                # 可复用组件（ModelSelector、SizeSelector、LoraSelector 等）
    ├── gallery/                   # 相册（列表、搜索、筛选、批量操作）
    ├── imageedit/                 # 图像编辑界面
    ├── texttoimage/               # 文生图界面
    ├── settings/                  # 设置（API 配置、模型管理、数据迁移）
    ├── localization/              # 中英文字符串资源
    ├── navigation/                # 底部导航
    └── theme/                     # Material 3 主题
```

## Contributing

参见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## License

MIT License - 详见 [LICENSE](LICENSE)

## Acknowledgments

- [DashScope API](https://dashscope.aliyun.com/) - 阿里云百炼
- [Stable Diffusion WebUI Forge](https://github.com/lllyasviel/stable-diffusion-webui-forge)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3](https://m3.material.io/)
- [Coil](https://github.com/coil-kt/coil) - 图片加载
