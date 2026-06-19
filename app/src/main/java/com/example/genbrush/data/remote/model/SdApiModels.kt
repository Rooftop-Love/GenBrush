package com.example.genbrush.data.remote.model

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
