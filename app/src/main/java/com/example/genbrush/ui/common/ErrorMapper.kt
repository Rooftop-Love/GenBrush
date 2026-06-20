package com.example.genbrush.ui.common

import com.example.genbrush.data.remote.DashScopeException

fun mapError(e: Throwable): String {
    // DashScope API 错误：直接展示 API 返回的错误码和消息
    if (e is DashScopeException) {
        return "[${ e.code }] ${e.message}"
    }

    val msg = e.message ?: "未知错误"
    return when {
        msg.contains("API key", ignoreCase = true) -> msg
        msg.contains("InvalidApiKey", ignoreCase = true) -> "API Key 无效，请在设置中检查。"
        msg.contains("Throttling", ignoreCase = true) || msg.contains("429") ->
            "请求频率过高，请稍后再试。"
        msg.contains("DataInspectionFailed", ignoreCase = true) ->
            "内容安全审核未通过，请更换提示词重试。"
        msg.contains("SD API 错误", ignoreCase = true) -> msg
        msg.contains("SD 未返回图片", ignoreCase = true) -> msg
        msg.contains("请先在设置中配置 SD 服务器", ignoreCase = true) -> msg
        msg.contains("timeout", ignoreCase = true) || msg.contains("connect", ignoreCase = true) ->
            "网络错误，请检查网络连接。手机和电脑是否在同一局域网？"
        msg.contains("HTTP 5", ignoreCase = true) ->
            "服务器错误，请稍后重试。"
        msg.contains("cleartext", ignoreCase = true) ->
            "网络安全限制，请确认服务器地址格式正确（http/https）。"
        else -> msg
    }
}
