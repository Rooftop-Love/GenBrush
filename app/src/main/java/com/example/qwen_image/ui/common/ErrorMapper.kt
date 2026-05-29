package com.example.qwen_image.ui.common

fun mapError(e: Throwable): String {
    val msg = e.message ?: "未知错误"
    return when {
        msg.contains("API key", ignoreCase = true) -> msg
        msg.contains("InvalidApiKey", ignoreCase = true) -> "API Key 无效，请在设置中检查。"
        msg.contains("Throttling", ignoreCase = true) || msg.contains("429") ->
            "请求频率过高，请稍后再试。"
        msg.contains("DataInspectionFailed", ignoreCase = true) ->
            "内容安全审核未通过，请更换提示词重试。"
        msg.contains("timeout", ignoreCase = true) || msg.contains("connect", ignoreCase = true) ->
            "网络错误，请检查网络连接。"
        msg.contains("HTTP 5", ignoreCase = true) ->
            "服务器错误，请稍后重试。"
        else -> msg
    }
}
