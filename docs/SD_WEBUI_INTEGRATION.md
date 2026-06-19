# Stable Diffusion WebUI Forge 集成方案

## 目标

在 genbrush (Qwen Image) app 中新增本地 SD WebUI 后端，使用户可以在手机上调用电脑的 Stable Diffusion Forge 生成图片。

## 前置条件

- PC 端 SD WebUI Forge Classic 已启动，`--api` 参数已添加（已完成）
- 手机和电脑在同一局域网
- PC 局域网 IP: `10.134.43.26`，SD 端口: `7860`
- SD API base URL 格式: `http://<ip>:7860`

## SD WebUI REST API 参考

### 核心端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/sdapi/v1/txt2img` | POST | 文生图 |
| `/sdapi/v1/img2img` | POST | 图生图（编辑） |
| `/sdapi/v1/sd-models` | GET | 列出可用 checkpoint 模型 |
| `/sdapi/v1/loras` | GET | 列出可用 LoRA |
| `/sdapi/v1/samplers` | GET | 列出可用采样器 |
| `/sdapi/v1/options` | GET | 获取当前配置 |
| `/sdapi/v1/options` | POST | 修改配置（切模型等） |
| `/sdapi/v1/progress` | GET | 当前生成进度 |

### txt2img 请求格式

```json
POST /sdapi/v1/txt2img
{
    "prompt": "a cat sitting on a chair",
    "negative_prompt": "ugly, blurry",
    "steps": 20,
    "cfg_scale": 1,
    "distilled_cfg_scale": 3,
    "width": 1024,
    "height": 1024,
    "seed": -1,
    "batch_size": 1,
    "sampler_name": "Euler",
    "scheduler": "Beta"
}
```

**重要**: Flux 模型使用 `cfg_scale=1` + `distilled_cfg_scale=3`，不要用普通 SD 的 cfg_scale=7。

### txt2img 响应格式

```json
{
    "images": ["base64_encoded_png_string"],
    "parameters": { ... },
    "info": "{\"seed\": 12345, \"steps\": 20, ...}"
}
```

图片在 `images[0]` 中，是 base64 编码的 PNG。

### img2img 请求格式

```json
POST /sdapi/v1/img2img
{
    "prompt": "make it a watercolor painting",
    "negative_prompt": "",
    "init_images": ["base64_encoded_image"],
    "steps": 20,
    "cfg_scale": 1,
    "distilled_cfg_scale": 3,
    "denoising_strength": 0.7,
    "width": 1024,
    "height": 1024,
    "sampler_name": "Euler",
    "scheduler": "Beta"
}
```

### sd-models 响应格式

```json
[
    {
        "title": "flux1-dev-Q8_0.gguf [129032f322]",
        "model_name": "flux1-dev-Q8_0",
        "hash": "129032f322",
        "filename": "D:/Apps/sd-webui-forge-classic/models/Stable-diffusion/flux1-dev-Q8_0.gguf"
    }
]
```

### loras 响应格式

```json
[
    {
        "name": "aidmaNSFWunlock-FLUX-V0.2",
        "alias": "aidmaNSFWunlock-FLUX-V0.2",
        "path": "D:/Apps/sd-webui-forge-classic/models/Lora/aidmaNSFWunlock-FLUX-V0.2.safetensors"
    }
]
```

## 架构设计

### 新增文件

1. **`data/remote/StableDiffusionApi.kt`** — SD WebUI REST API 客户端
2. **`data/remote/model/SdApiModels.kt`** — SD API 请求/响应数据模型

### 修改文件

3. **`data/local/PreferencesManager.kt`** — 新增 backend 类型和 SD 服务器 URL
4. **`data/repository/GenerationRepository.kt`** — 根据 backend 类型分发请求
5. **`ui/settings/SettingsScreen.kt`** — 新增后端切换和 SD URL 输入
6. **`ui/settings/SettingsViewModel.kt`** — 新增后端相关状态
7. **`ui/components/ModelSelector.kt`** — 模型列表支持动态加载
8. **`ui/texttoimage/TextToImageViewModel.kt`** — 传递 backend 信息
9. **`ui/imageedit/ImageEditViewModel.kt`** — 传递 backend 信息
10. **`QwenImageApp.kt`** — 初始化 SD API 客户端
11. **`ui/localization/AppStrings.kt`** — 新增 SD 相关文案

## 详细实现指引

### Step 1: 数据模型 — `SdApiModels.kt`

```kotlin
package com.example.qwen_image.data.remote.model

// --- Request ---
data class SdTxt2ImgRequest(
    val prompt: String,
    val negative_prompt: String = "",
    val steps: Int = 20,
    val cfg_scale: Float = 1f,
    val distilled_cfg_scale: Float = 3f,
    val width: Int = 1024,
    val height: Int = 1024,
    val seed: Int = -1,
    val batch_size: Int = 1,
    val sampler_name: String = "Euler",
    val scheduler: String = "Beta"
)

data class SdImg2ImgRequest(
    val prompt: String,
    val negative_prompt: String = "",
    val init_images: List<String>,  // base64 encoded
    val steps: Int = 20,
    val cfg_scale: Float = 1f,
    val distilled_cfg_scale: Float = 3f,
    val denoising_strength: Float = 0.7f,
    val width: Int = 1024,
    val height: Int = 1024,
    val sampler_name: String = "Euler",
    val scheduler: String = "Beta"
)

// --- Response ---
data class SdImageResponse(
    val images: List<String>,  // base64 encoded PNG
    val parameters: Map<String, Any>? = null,
    val info: String? = null
)

data class SdModelInfo(
    val title: String,
    val model_name: String,
    val hash: String,
    val filename: String
)

data class SdLoraInfo(
    val name: String,
    val alias: String?,
    val path: String
)

data class SdProgressResponse(
    val progress: Float,  // 0.0 ~ 1.0
    val eta_relative: Float,
    val state: Map<String, Any>?
)
```

### Step 2: SD API 客户端 — `StableDiffusionApi.kt`

```kotlin
package com.example.qwen_image.data.remote

import com.example.qwen_image.data.remote.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class StableDiffusionApi(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    // serverUrl 从 PreferencesManager 读取，格式: "http://192.168.x.x:7860"

    suspend fun txt2img(
        serverUrl: String,
        request: SdTxt2ImgRequest
    ): Result<SdImageResponse> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = gson.toJson(request)
            val httpRequest = Request.Builder()
                .url("$serverUrl/sdapi/v1/txt2img")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(httpRequest).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("响应内容为空"))

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("SD API 错误: HTTP ${response.code} - ${body.take(200)}")
                )
            }

            val result = gson.fromJson(body, SdImageResponse::class.java)
            if (result.images.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("SD 未返回图片"))
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun img2img(
        serverUrl: String,
        request: SdImg2ImgRequest
    ): Result<SdImageResponse> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = gson.toJson(request)
            val httpRequest = Request.Builder()
                .url("$serverUrl/sdapi/v1/img2img")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(httpRequest).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("响应内容为空"))

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("SD API 错误: HTTP ${response.code} - ${body.take(200)}")
                )
            }

            val result = gson.fromJson(body, SdImageResponse::class.java)
            if (result.images.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("SD 未返回图片"))
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getModels(
        serverUrl: String
    ): Result<List<SdModelInfo>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$serverUrl/sdapi/v1/sd-models")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("响应内容为空"))

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val type = object : TypeToken<List<SdModelInfo>>() {}.type
            val models: List<SdModelInfo> = gson.fromJson(body, type)
            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLoras(
        serverUrl: String
    ): Result<List<SdLoraInfo>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$serverUrl/sdapi/v1/loras")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("响应内容为空"))

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val type = object : TypeToken<List<SdLoraInfo>>() {}.type
            val loras: List<SdLoraInfo> = gson.fromJson(body, type)
            Result.success(loras)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProgress(
        serverUrl: String
    ): Result<SdProgressResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$serverUrl/sdapi/v1/progress")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("响应内容为空"))

            val result = gson.fromJson(body, SdProgressResponse::class.java)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(serverUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$serverUrl/sdapi/v1/sd-models")
                .get()
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Step 3: 修改 PreferencesManager

在 `PreferencesManager` 中新增：

```kotlin
// 新增 key
private const val KEY_BACKEND = "backend_type"
private const val KEY_SD_SERVER_URL = "sd_server_url"

// Backend 类型枚举值
companion object {
    const val BACKEND_DASHSCOPE = "dashscope"
    const val BACKEND_SD_WEBUI = "sd_webui"
}

var backend: String
    get() = prefs.getString(KEY_BACKEND, BACKEND_DASHSCOPE) ?: BACKEND_DASHSCOPE
    set(value) = prefs.edit().putString(KEY_BACKEND, value).apply()

var sdServerUrl: String
    get() = prefs.getString(KEY_SD_SERVER_URL, "http://192.168.105.1:7860") ?: ""
    set(value) = prefs.edit().putString(KEY_SD_SERVER_URL, value).apply()
```

### Step 4: 修改 GenerationRepository

在 `GenerationRepository` 中注入 `StableDiffusionApi`，根据 `prefs.backend` 分发：

```kotlin
class GenerationRepository(
    private val dashScopeApi: DashScopeApi,
    private val sdApi: StableDiffusionApi,  // 新增
    private val imageStore: ImageStore,
    private val prefs: PreferencesManager
) {
    suspend fun generateTextToImage(
        prompt: String,
        negativePrompt: String,
        model: String,
        size: String
    ): Result<ImageEntry> {
        return when (prefs.backend) {
            PreferencesManager.BACKEND_SD_WEBUI -> generateWithSd(prompt, negativePrompt, size)
            else -> generateWithDashScope(prompt, negativePrompt, model, size)
        }
    }

    private suspend fun generateWithSd(
        prompt: String,
        negativePrompt: String,
        size: String
    ): Result<ImageEntry> {
        val serverUrl = prefs.sdServerUrl
        if (serverUrl.isBlank()) {
            return Result.failure(Exception("请先在设置中配置 SD 服务器地址"))
        }

        // 解析尺寸 "1024*1024" -> width=1024, height=1024
        val parts = size.split("*")
        val width = parts.getOrNull(0)?.toIntOrNull() ?: 1024
        val height = parts.getOrNull(1)?.toIntOrNull() ?: 1024

        val request = SdTxt2ImgRequest(
            prompt = prompt,
            negative_prompt = negativePrompt,
            width = width,
            height = height
        )

        val response = sdApi.txt2img(serverUrl, request)
        return response.fold(
            onSuccess = { sdResponse ->
                try {
                    val base64Image = sdResponse.images.first()
                    val imageBytes = android.util.Base64.decode(
                        base64Image,
                        android.util.Base64.DEFAULT
                    )
                    val fileName = ImageStore.createFileName("text_to_image")
                    val entry = ImageEntry(
                        fileName = fileName,
                        prompt = prompt,
                        model = "SD Forge (local)",
                        type = "text_to_image"
                    )
                    Result.success(imageStore.saveImageFromBytes(imageBytes, entry))
                } catch (e: Exception) {
                    Result.failure(Exception("保存图片失败：${e.message}"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    // img2img 类似，使用 sdApi.img2img()
    // ...
}
```

注意：`ImageStore` 需要新增一个 `saveImageFromBytes(bytes: ByteArray, entry: ImageEntry)` 方法，因为 SD 返回 base64 而非 URL。实现参考现有的 `saveImageFromUrl`，把 URL 下载改为直接写入字节。

### Step 5: 修改设置页

在 SettingsScreen 中新增：
- 后端切换（Radio: DashScope / SD WebUI Forge）
- SD 服务器地址输入框（仅当选择 SD 后端时显示）
- 连接测试按钮

SettingsState 新增字段：
```kotlin
data class SettingsState(
    // ... existing fields ...
    val backend: String = PreferencesManager.BACKEND_DASHSCOPE,
    val sdServerUrl: String = "",
    val sdConnectionStatus: String? = null  // null=未测试, "ok", "fail:xxx"
)
```

### Step 6: 模型列表动态化

当后端为 SD 时，ModelSelector 应该从 `/sdapi/v1/sd-models` 动态获取模型列表，而不是使用硬编码的 `GENERATION_MODELS`。

在 TextToImageViewModel 中：
```kotlin
init {
    if (prefs.backend == PreferencesManager.BACKEND_SD_WEBUI) {
        // 异步加载 SD 模型列表
        viewModelScope.launch {
            sdApi.getModels(prefs.sdServerUrl).onSuccess { models ->
                _state.update { it.copy(availableModels = models.map { m -> m.model_name }) }
            }
        }
    }
}
```

尺寸选择器也要适配：SD 模式下显示常见尺寸（512x512, 768x768, 1024x1024, 1024x576 等），或允许用户自定义输入。

### Step 7: 依赖更新

`QwenImageApp.kt` 中初始化时同时创建两个 API 客户端：

```kotlin
val sdApi = StableDiffusionApi(okHttpClient, gson)
repository = GenerationRepository(api, sdApi, imageStore, prefs)
```

## 注意事项

1. **超时**: SD 生成 Flux 模型很慢（2-3分钟），OkHttp 的 readTimeout 要设够大（建议 300 秒）。当前是 60 秒，需要改。
2. **内存**: base64 解码 1024x1024 PNG 约占 4-10MB，注意在 IO 线程处理。
3. **错误处理**: SD API 返回的错误信息可能在 HTTP body 里，不一定有 JSON 结构。
4. **网络**: 手机和电脑必须在同一局域网。如果 WiFi 和有线不在同一网段，需要调整。
5. **Windows 防火墙**: 首次连接时 Windows 可能弹出防火墙提示，需要允许。
6. **LoRA 语法**: 在 prompt 中使用 `<lora:name:weight>` 格式加载 LoRA，不需要额外 API 调用。
7. **Flux 特性**: Flux 模型不支持 negative prompt（WebUI 里也是 disabled 状态），可以忽略。
8. **进度轮询**: 可选功能，用 `/sdapi/v1/progress` 轮询生成进度，在 loading 界面显示百分比。优先级低，先实现基础功能。

## 验证清单

- [ ] 设置页可以切换后端（DashScope / SD WebUI）
- [ ] 设置页可以输入 SD 服务器地址并测试连接
- [ ] 选择 SD 后端后，模型列表从服务器动态加载
- [ ] txt2img 功能正常，图片能保存到相册
- [ ] img2img 功能正常（可选，优先 txt2img）
- [ ] DashScope 后端功能不受影响
- [ ] 生成过程中有 loading 提示
- [ ] 错误信息能正确显示（网络不通、服务器错误等）
