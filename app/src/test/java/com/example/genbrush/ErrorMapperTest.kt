package com.example.genbrush

import com.example.genbrush.data.remote.DashScopeException
import com.example.genbrush.ui.common.mapError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 验证图片生成失败时，API 原始错误信息不会被静默吞掉，
 * 而是正确传递给用户。
 */
class ErrorMapperTest {

    @Test
    fun `DashScopeException 应展示错误码和原始消息`() {
        val ex = DashScopeException("InvalidParameter", "prompt is too long")
        val result = mapError(ex)

        assertEquals("[InvalidParameter] prompt is too long", result)
    }

    @Test
    fun `DashScopeException 含 Throttling 也应展示原始 API 错误而非通用消息`() {
        val ex = DashScopeException("Throttling.RateQuota", "Requests rate limit exceeded")
        val result = mapError(ex)

        // DashScopeException 应优先展示 API 原始信息，不走通用映射
        assertTrue("应包含错误码", result.contains("Throttling.RateQuota"))
        assertTrue("应包含原始消息", result.contains("Requests rate limit exceeded"))
    }

    @Test
    fun `DashScopeException 含 DataInspectionFailed 也应展示原始 API 错误`() {
        val ex = DashScopeException("DataInspectionFailed", "Input data may contain inappropriate content")
        val result = mapError(ex)

        assertTrue("应包含错误码", result.contains("DataInspectionFailed"))
        assertTrue("应包含原始消息", result.contains("inappropriate content"))
    }

    @Test
    fun `DashScopeException 含 InvalidApiKey 也应展示原始 API 错误`() {
        val ex = DashScopeException("InvalidApiKey", "The API key is invalid")
        val result = mapError(ex)

        assertEquals("[InvalidApiKey] The API key is invalid", result)
    }

    @Test
    fun `DashScopeException 服务器错误应展示原始信息`() {
        val ex = DashScopeException("InternalError", "An internal server error occurred")
        val result = mapError(ex)

        assertEquals("[InternalError] An internal server error occurred", result)
    }

    @Test
    fun `普通 Exception 含网络关键词应映射为友好提示`() {
        val ex = Exception("connect timed out")
        val result = mapError(ex)

        assertTrue("网络错误应展示友好提示", result.contains("网络错误"))
    }

    @Test
    fun `普通 Exception 含 SD API 错误应原样展示`() {
        val ex = Exception("SD API 错误: HTTP 500 - Internal Server Error")
        val result = mapError(ex)

        assertEquals("SD API 错误: HTTP 500 - Internal Server Error", result)
    }

    @Test
    fun `无消息的 Throwable 应展示未知错误`() {
        val ex = Exception()
        val result = mapError(ex)

        assertEquals("未知错误", result)
    }
}
